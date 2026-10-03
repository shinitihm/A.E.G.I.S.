"""Briefing do dia: temperatura e última chuva da cidade (Open-Meteo) + manchetes principais (feed RSS).

Nenhuma das duas fontes precisa de chave. Cada metade falha sozinha: se o clima cair, as notícias
continuam chegando (e vice-versa), com a mensagem de erro no campo *_error.
"""

import asyncio
import time
import xml.etree.ElementTree as ET
from datetime import date

import httpx

from .config import settings
from .models import Briefing, LastRain, NewsItem, Weather

USER_AGENT = "AEGIS/1.0 (projeto escolar Android)"
GEOCODE_URL = "https://geocoding-api.open-meteo.com/v1/search"
FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
PAST_DAYS = 30  # até onde olhamos para achar a última chuva (a API aceita até 92)
RAIN_MM = 0.1  # abaixo disso é garoa de sensor, não conta como chuva
NEWS_COUNT = 2

_client = httpx.AsyncClient(timeout=10, headers={"User-Agent": USER_AGENT})
_cache: dict[str, tuple[float, object]] = {}


class BriefingError(Exception):
    """Falha esperada (sem internet, cidade inexistente...). A mensagem vai para o app."""


async def _get(url: str, **params) -> httpx.Response:
    try:
        resp = await _client.get(url, params=params)
        resp.raise_for_status()
    except httpx.HTTPError as e:
        raise BriefingError("Sem acesso à internet ou serviço fora do ar") from e
    return resp


async def _cached(key: str, ttl: float, fetch):
    hit = _cache.get(key)
    if hit and hit[0] > time.time():
        return hit[1]
    value = await fetch()  # se falhar, a exceção passa direto e nada é guardado
    _cache[key] = (time.time() + ttl, value)
    return value


# ── clima ──

async def geocode(city: str) -> tuple[str, float, float]:
    async def fetch():
        body = (await _get(GEOCODE_URL, name=city, count=1, language="pt", format="json")).json()
        results = body.get("results") or []  # sem resultados, a API pode omitir o campo
        if not results:
            raise BriefingError(f"Não encontrei a cidade '{city}'")
        r = results[0]
        label = r["name"] + (f", {r['admin1']}" if r.get("admin1") else "")
        return label, r["latitude"], r["longitude"]

    return await _cached(f"geo:{city.lower()}", 24 * 3600, fetch)


def parse_weather(body: dict) -> Weather:
    days = body["daily"]["time"]
    rain = body["daily"]["precipitation_sum"]
    today = date.fromisoformat(days[-1])  # com forecast_days=1 o último dia da lista é hoje (fuso da cidade)
    last = None
    for day, mm in zip(reversed(days), reversed(rain)):
        if (mm or 0) >= RAIN_MM:  # a API manda null nos dias sem dado
            last = LastRain(date=day, days_ago=(today - date.fromisoformat(day)).days, mm=round(mm, 1))
            break
    return Weather(temperature_c=body["current"]["temperature_2m"], last_rain=last, window_days=PAST_DAYS)


async def weather(lat: float, lon: float) -> Weather:
    async def fetch():
        resp = await _get(FORECAST_URL, latitude=lat, longitude=lon, current="temperature_2m",
                          daily="precipitation_sum", past_days=PAST_DAYS, forecast_days=1, timezone="auto")
        return parse_weather(resp.json())

    return await _cached(f"wx:{lat:.2f},{lon:.2f}", 15 * 60, fetch)


# ── notícias ──

def parse_news(xml_text: str, limit: int = NEWS_COUNT) -> list[NewsItem]:
    items: list[NewsItem] = []
    for item in ET.fromstring(xml_text).iterfind("./channel/item"):
        title = (item.findtext("title") or "").strip()
        link = (item.findtext("link") or "").strip()
        if not title or not link.startswith(("http://", "https://")):  # o app abre este link: só web
            continue
        source = (item.findtext("source") or "").strip() or None
        if source and title.endswith(f" - {source}"):  # o Google News cola a fonte no fim do título
            title = title[: -len(source) - 3]
        items.append(NewsItem(title=title, source=source, url=link))
        if len(items) == limit:
            break
    return items


async def top_news() -> list[NewsItem]:
    async def fetch():
        items = parse_news((await _get(settings.news_feed_url)).text)
        if not items:
            raise BriefingError("O feed não trouxe manchetes")
        return items

    return await _cached("news", 10 * 60, fetch)


# ── junta tudo ──

async def _section(coro, unexpected: str):
    """Roda uma metade do briefing; devolve (valor, None) ou (None, mensagem de erro)."""
    try:
        return await coro, None
    except BriefingError as e:
        return None, str(e)
    except (KeyError, IndexError, TypeError, ValueError, ET.ParseError):  # resposta fora do formato esperado
        return None, unexpected


async def build(city: str) -> Briefing:
    city = city.strip()

    async def city_weather():
        label, lat, lon = await geocode(city)
        return label, await weather(lat, lon)

    (found, wx_error), (news, news_error) = await asyncio.gather(
        _section(city_weather(), "Resposta inesperada do serviço de clima"),
        _section(top_news(), "Resposta inesperada do feed de notícias"),
    )
    label, wx = found or (None, None)
    return Briefing(city=label, weather=wx, weather_error=wx_error, news=news or [], news_error=news_error)

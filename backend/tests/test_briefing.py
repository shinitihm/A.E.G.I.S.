"""Testes sem internet: Open-Meteo e o feed de notícias são substituídos por respostas falsas."""

import httpx
import pytest
from fastapi.testclient import TestClient

from app import briefing
from app.main import app

GEOCODE = {"results": [{"name": "Campinas", "admin1": "São Paulo", "latitude": -22.9, "longitude": -47.06}]}

RSS = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0"><channel><title>Top stories</title>
<item><title>Primeira manchete - G1</title><link>https://news.example/1</link><source url="https://g1">G1</source></item>
<item><title>Segunda manchete - Folha</title><link>https://news.example/2</link><source url="https://folha">Folha</source></item>
<item><title>Terceira manchete - UOL</title><link>https://news.example/3</link><source url="https://uol">UOL</source></item>
</channel></rss>"""


def forecast(days: list[str], rain: list, temp: float = 27.4) -> dict:
    return {"current": {"temperature_2m": temp}, "daily": {"time": days, "precipitation_sum": rain}}


DAYS = ["2026-09-26", "2026-09-27", "2026-09-28", "2026-09-29", "2026-09-30"]  # o último é "hoje"


class Net:
    """Roteia as chamadas HTTP do módulo para respostas falsas e guarda o que foi pedido."""

    def __init__(self):
        self.geocode = GEOCODE
        self.forecast = forecast(DAYS, [0, 5.2, 0, 0, 0])
        self.rss = RSS
        self.calls: list[tuple[str, dict]] = []
        self.down: set[str] = set()  # "geocode" | "forecast" | "rss"

    async def get(self, url, **params):
        self.calls.append((url, params))
        for name, prefix in (("geocode", briefing.GEOCODE_URL), ("forecast", briefing.FORECAST_URL)):
            if url == prefix:
                if name in self.down:
                    raise briefing.BriefingError("Sem acesso à internet ou serviço fora do ar")
                return httpx.Response(200, json=getattr(self, name))
        if "rss" in self.down:
            raise briefing.BriefingError("Sem acesso à internet ou serviço fora do ar")
        return httpx.Response(200, text=self.rss)


@pytest.fixture
def net(monkeypatch):
    fake = Net()
    monkeypatch.setattr(briefing, "_get", fake.get)
    briefing._cache.clear()
    return fake


client = TestClient(app)


def test_last_rain_is_the_most_recent_rainy_day(net):
    body = client.get("/briefing", params={"city": "Campinas"}).json()
    assert body["city"] == "Campinas, São Paulo"
    assert body["weather"]["temperature_c"] == 27.4
    assert body["weather"]["last_rain"] == {"date": "2026-09-27", "days_ago": 3, "mm": 5.2}


def test_rain_today_is_zero_days_ago(net):
    net.forecast = forecast(DAYS, [0, 0, 0, 0, 1.0])
    assert client.get("/briefing", params={"city": "Campinas"}).json()["weather"]["last_rain"]["days_ago"] == 0


def test_no_rain_in_window_and_null_values(net):
    net.forecast = forecast(DAYS, [0, None, 0, None, 0])
    weather = client.get("/briefing", params={"city": "Campinas"}).json()["weather"]
    assert weather["last_rain"] is None
    assert weather["window_days"] == briefing.PAST_DAYS


def test_drizzle_below_threshold_is_not_rain(net):
    net.forecast = forecast(DAYS, [0, 0.05, 0, 0, 0])
    assert client.get("/briefing", params={"city": "Campinas"}).json()["weather"]["last_rain"] is None


def test_forecast_uses_geocoded_coordinates_and_window(net):
    client.get("/briefing", params={"city": "Campinas"})
    _, params = next(c for c in net.calls if c[0] == briefing.FORECAST_URL)
    assert (params["latitude"], params["longitude"]) == (-22.9, -47.06)
    assert params["past_days"] == briefing.PAST_DAYS and params["timezone"] == "auto"


def test_two_headlines_with_source_stripped_from_title(net):
    news = client.get("/briefing", params={"city": "Campinas"}).json()["news"]
    assert [n["title"] for n in news] == ["Primeira manchete", "Segunda manchete"]
    assert news[0]["source"] == "G1" and news[0]["url"] == "https://news.example/1"


def test_non_http_links_are_dropped(net):
    net.rss = RSS.replace("https://news.example/1", "javascript:alert(1)")
    news = client.get("/briefing", params={"city": "Campinas"}).json()["news"]
    assert [n["url"] for n in news] == ["https://news.example/2", "https://news.example/3"]


def test_unknown_city_keeps_the_news(net):
    net.geocode = {}  # a Open-Meteo pode omitir "results" quando não acha nada
    body = client.get("/briefing", params={"city": "Xyzzyville"}).json()
    assert body["weather"] is None and "Xyzzyville" in body["weather_error"]
    assert len(body["news"]) == 2 and body["news_error"] is None


def test_news_down_keeps_the_weather(net):
    net.down.add("rss")
    body = client.get("/briefing", params={"city": "Campinas"}).json()
    assert body["weather"]["temperature_c"] == 27.4
    assert body["news"] == [] and body["news_error"]


def test_weather_down_keeps_the_news(net):
    net.down.add("forecast")
    body = client.get("/briefing", params={"city": "Campinas"}).json()
    assert body["weather"] is None and body["weather_error"]
    assert len(body["news"]) == 2


def test_malformed_feed_and_empty_feed_are_reported_not_500(net):
    net.rss = "<rss><channel><item>"
    body = client.get("/briefing", params={"city": "Campinas"}).json()
    assert body["news"] == [] and body["news_error"]
    briefing._cache.clear()
    net.rss = '<rss version="2.0"><channel></channel></rss>'
    assert client.get("/briefing", params={"city": "Campinas"}).json()["news_error"]


def test_unexpected_forecast_shape_is_reported_not_500(net):
    net.forecast = {"oops": True}
    body = client.get("/briefing", params={"city": "Campinas"}).json()
    assert body["weather"] is None and body["weather_error"]


def test_geocode_is_cached(net):
    client.get("/briefing", params={"city": "Campinas"})
    client.get("/briefing", params={"city": " campinas "})
    assert sum(1 for url, _ in net.calls if url == briefing.GEOCODE_URL) == 1


def test_city_must_have_two_letters(net):
    assert client.get("/briefing", params={"city": "a"}).status_code == 422
    assert client.get("/briefing").status_code == 422

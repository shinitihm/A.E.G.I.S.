"""Cliente da Comic Vine API com cache em memória.

A Comic Vine limita ~200 requisições por recurso por hora, então tudo que
vem de lá passa pelo cache antes.
"""

import asyncio
import html
import re
import time
from datetime import date

import httpx
from fastapi import HTTPException

from . import threat as threat_calc
from .config import settings
from .models import HeroDetail, HeroSummary, Mission, MissionDetail, Threat

USER_AGENT = "AEGIS/1.0 (projeto escolar Android)"
CHARACTER_PREFIX = "4005"
STORY_ARC_PREFIX = "4045"

LIST_FIELDS = "id,name,real_name,deck,image,publisher,count_of_issue_appearances,first_appeared_in_issue"
DETAIL_FIELDS = (
    LIST_FIELDS + ",aliases,description,origin,powers,teams,character_enemies,character_friends,site_detail_url"
)
ARC_FIELDS = "id,name,deck,image,publisher,count_of_isssue_appearances,first_appeared_in_issue"  # "isssue" é typo da própria API

# Alvos que podem aparecer como "Alvo do dia" (widget)
DAILY_TARGETS = [
    "Thanos", "Hulk", "Doctor Doom", "Magneto", "Galactus", "Loki", "Ultron", "Wolverine",
    "Thor", "Captain America", "Spider-Man", "Scarlet Witch", "Doctor Strange", "Black Panther",
    "Venom", "Red Skull", "Kang", "Deadpool", "Silver Surfer", "Black Widow",
]

# Arquivos de missão (story arcs)
MISSION_ARCS = [
    "Civil War", "Infinity Gauntlet", "Secret Wars", "House of M", "Secret Invasion",
    "Age of Ultron", "Planet Hulk", "Dark Phoenix Saga", "Extremis", "Armor Wars",
    "Demon in a Bottle", "Infinity War",
]

_cache: dict[str, tuple[float, object]] = {}
_threats: dict[int, Threat] = {}  # alvos já escaneados
_client = httpx.AsyncClient(timeout=20, headers={"User-Agent": USER_AGENT})
_slots = asyncio.Semaphore(2)  # a Comic Vine bloqueia rajadas de requisições


async def _get(path: str, **params) -> dict | list:
    key = path + "?" + "&".join(f"{k}={v}" for k, v in sorted(params.items()))
    hit = _cache.get(key)
    if hit and hit[0] > time.time():
        return hit[1]
    if not settings.comic_vine_api_key:
        raise HTTPException(503, "COMIC_VINE_API_KEY não configurada no backend (.env)")

    async with _slots:
        try:
            resp = await _client.get(
                settings.comic_vine_base_url + path,
                params={"api_key": settings.comic_vine_api_key, "format": "json", **params},
            )
        except httpx.HTTPError as e:
            raise HTTPException(502, f"Falha ao contatar a Comic Vine: {e}") from e
    if resp.status_code == 420 or resp.status_code == 429:
        raise HTTPException(429, "Comic Vine: limite de requisições atingido, tente em alguns minutos")
    if resp.status_code != 200:
        raise HTTPException(502, f"Comic Vine respondeu HTTP {resp.status_code}")

    body = resp.json()
    if body.get("status_code") == 101:
        raise HTTPException(404, "Alvo não encontrado")
    if body.get("status_code") != 1:
        raise HTTPException(502, f"Comic Vine: {body.get('error')}")

    _cache[key] = (time.time() + settings.cache_ttl_seconds, body["results"])
    return body["results"]


# ── conversões ──────────────────────────────────────────────

def clean_html(raw: str | None, limit: int = 2500) -> str | None:
    if not raw:
        return None
    text = re.sub(r"<(br|/p|/h\d|/li)[^>]*>", "\n", raw)
    text = html.unescape(re.sub(r"<[^>]+>", "", text))
    text = re.sub(r"\n\s*\n+", "\n\n", text).strip()
    return text[:limit].rsplit(" ", 1)[0] + "…" if len(text) > limit else text


def _is_marvel(item: dict) -> bool:
    return "marvel" in ((item.get("publisher") or {}).get("name") or "").lower()


def _names(items: list[dict] | None) -> list[str]:
    return [i["name"] for i in items or [] if i.get("name")]


def _first_issue(item: dict) -> str | None:
    issue = item.get("first_appeared_in_issue") or {}
    if not issue:
        return None
    return f"{issue.get('name') or 'Edição'} #{issue.get('issue_number', '?')}"


def _summary(item: dict) -> HeroSummary:
    image = item.get("image") or {}
    return HeroSummary(
        id=item["id"],
        name=item["name"],
        real_name=item.get("real_name") or None,
        deck=item.get("deck"),
        image_url=image.get("super_url") or image.get("medium_url"),
        thumb_url=image.get("small_url") or image.get("thumb_url"),
        appearances=item.get("count_of_issue_appearances") or 0,
        first_appearance=_first_issue(item),
        publisher=(item.get("publisher") or {}).get("name"),
        threat=_threats.get(item["id"]),
    )


# ── heróis ──────────────────────────────────────────────────

async def list_heroes(page: int) -> tuple[list[HeroSummary], bool]:
    """Personagens mais famosos primeiro, só Marvel. Retorna (itens, tem_mais)."""
    results = await _get(
        "characters/", sort="count_of_issue_appearances:desc", limit=100, offset=page * 100,
        field_list=LIST_FIELDS,
    )
    return [_summary(r) for r in results if _is_marvel(r)], len(results) == 100


async def search_heroes(query: str) -> list[HeroSummary]:
    results = await _get(
        "characters/", filter=f"name:{query}", sort="count_of_issue_appearances:desc", limit=50,
        field_list=LIST_FIELDS,
    )
    return [_summary(r) for r in results if _is_marvel(r)]


async def hero_detail(hero_id: int) -> HeroDetail:
    r = await _get(f"character/{CHARACTER_PREFIX}-{hero_id}/", field_list=DETAIL_FIELDS)
    powers, teams = _names(r.get("powers")), _names(r.get("teams"))
    enemies, friends = _names(r.get("character_enemies")), _names(r.get("character_friends"))
    base = _summary(r)
    threat = threat_calc.calculate(powers, base.appearances, len(enemies), len(teams))
    _threats[hero_id] = threat
    return HeroDetail(
        **base.model_dump(exclude={"threat"}),
        threat=threat,
        bio=clean_html(r.get("description")),
        aliases=[a for a in (r.get("aliases") or "").splitlines() if a.strip()],
        origin=(r.get("origin") or {}).get("name"),
        powers=powers, teams=teams, enemies=enemies, friends=friends,
        site_url=r.get("site_detail_url"),
    )


async def find_hero(name_or_id: str) -> HeroDetail:
    """Aceita um ID numérico ou um nome ("Thanos", "homem de ferro" não — use o nome em inglês)."""
    if name_or_id.strip().isdigit():
        return await hero_detail(int(name_or_id))
    matches = await search_heroes(name_or_id.strip())
    if not matches:
        raise HTTPException(404, f"Nenhum alvo Marvel chamado '{name_or_id}'")
    exact = [m for m in matches if m.name.lower() == name_or_id.strip().lower()]
    return await hero_detail((exact or matches)[0].id)


async def target_of_the_day() -> HeroDetail:
    return await find_hero(DAILY_TARGETS[date.today().toordinal() % len(DAILY_TARGETS)])


def scanned_threats() -> dict[int, Threat]:
    return _threats


# ── missões (story arcs) ────────────────────────────────────

def _mission(item: dict) -> Mission:
    image = item.get("image") or {}
    return Mission(
        id=item["id"],
        name=item["name"],
        deck=item.get("deck"),
        image_url=image.get("super_url") or image.get("medium_url"),
        issue_count=item.get("count_of_isssue_appearances"),
        first_issue=_first_issue(item),
    )


async def _find_arc(name: str) -> Mission | None:
    try:
        results = await _get("story_arcs/", filter=f"name:{name}", field_list=ARC_FIELDS, limit=20)
    except HTTPException:
        return None
    marvel = [r for r in results if _is_marvel(r)]
    if not marvel:
        return None
    exact = [r for r in marvel if r["name"].lower() == name.lower()]
    return _mission((exact or marvel)[0])


async def list_missions() -> list[Mission]:
    found = await asyncio.gather(*(_find_arc(n) for n in MISSION_ARCS))
    return [m for m in found if m]


async def mission_detail(arc_id: int) -> MissionDetail:
    r = await _get(f"story_arc/{STORY_ARC_PREFIX}-{arc_id}/", field_list=ARC_FIELDS + ",description,issues")
    issues = [f"{i.get('name') or 'Edição'} #{i.get('issue_number', '?')}" for i in (r.get("issues") or [])[:25]]
    return MissionDetail(**_mission(r).model_dump(), description=clean_html(r.get("description"), 4000), issues=issues)


# ── armaduras: tenta achar imagem em /objects ───────────────

async def object_image(query: str) -> str | None:
    try:
        results = await _get("objects/", filter=f"name:{query}", field_list="name,image", limit=5)
    except HTTPException:
        return None
    for r in results:
        url = (r.get("image") or {}).get("super_url")
        if url and "default" not in url:  # ignora o placeholder genérico
            return url
    return None

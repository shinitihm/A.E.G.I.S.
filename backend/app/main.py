"""A.E.G.I.S. — backend.

Rodar:  uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
Docs:   http://localhost:8000/docs
"""

from fastapi import FastAPI, Query

from . import armors, briefing, comicvine, jarvis
from .config import settings
from .models import Armor, Briefing, ChatRequest, ChatResponse, CompareResult, HeroDetail, HeroPage, HeroSummary, Mission, MissionDetail

app = FastAPI(title="A.E.G.I.S. API", version="1.0")


@app.get("/health")
async def health():
    return {
        "status": "ONLINE",
        "comic_vine": bool(settings.comic_vine_api_key),
        "jarvis": "IA" if settings.gemini_api_key else "OFFLINE",
    }


# ── heróis ──

@app.get("/heroes", response_model=HeroPage)
async def heroes(page: int = Query(0, ge=0)):
    items, has_more = await comicvine.list_heroes(page)
    return HeroPage(items=items, next_page=page + 1 if has_more else None)


@app.get("/heroes/search", response_model=list[HeroSummary])
async def search(q: str = Query(..., min_length=2)):
    return await comicvine.search_heroes(q)


@app.get("/heroes/target-of-the-day", response_model=HeroDetail)
async def target_of_the_day():
    return await comicvine.target_of_the_day()


@app.get("/heroes/{hero_id}", response_model=HeroDetail)
async def hero(hero_id: int):
    return await comicvine.hero_detail(hero_id)


@app.get("/compare", response_model=CompareResult)
async def compare(a: str, b: str):
    """a e b podem ser IDs ou nomes em inglês: /compare?a=Hulk&b=Thor"""
    hero_a, hero_b = await comicvine.find_hero(a), await comicvine.find_hero(b)
    sa, sb = hero_a.threat.score, hero_b.threat.score
    prob_a = round(100 * sa / (sa + sb)) if sa + sb else 50
    if prob_a == 50:
        winner, verdict = None, f"Empate técnico, senhor. {hero_a.name} e {hero_b.name} se anulam."
    else:
        w, l, p = (hero_a, hero_b, prob_a) if prob_a > 50 else (hero_b, hero_a, 100 - prob_a)
        winner = w.id
        margin = "vitória esmagadora" if p >= 70 else "vantagem clara" if p >= 58 else "disputa apertada"
        verdict = (f"{margin.capitalize()}, senhor. {w.name} vence em {p}% das simulações. "
                   f"Ameaça {w.threat.level} contra {l.threat.level} de {l.name}.")
    return CompareResult(a=hero_a, b=hero_b, winner_id=winner, probability_a=prob_a, verdict=verdict)


# ── briefing (pop-up de boas-vindas) ──

@app.get("/briefing", response_model=Briefing)
async def daily_briefing(city: str = Query(..., min_length=2)):
    return await briefing.build(city)


# ── missões ──

@app.get("/missions", response_model=list[Mission])
async def missions():
    return await comicvine.list_missions()


@app.get("/missions/{arc_id}", response_model=MissionDetail)
async def mission(arc_id: int):
    return await comicvine.mission_detail(arc_id)


# ── arsenal ──

@app.get("/armors", response_model=list[Armor])
async def armor_list():
    return armors.ARMORS


@app.get("/armors/{mark}", response_model=Armor)
async def armor(mark: int):
    return await armors.get_with_image(mark)


# ── J.A.R.V.I.S. ──

@app.post("/jarvis/chat", response_model=ChatResponse)
async def jarvis_chat(req: ChatRequest):
    return await jarvis.chat(req)

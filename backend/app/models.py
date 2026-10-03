from typing import Literal

from pydantic import BaseModel


class ThreatFactor(BaseModel):
    label: str
    points: int
    max: int


class Threat(BaseModel):
    score: int
    level: str  # texto exibido: BAIXO / MODERADO / ALTO / ÔMEGA
    level_code: Literal["LOW", "MODERATE", "HIGH", "OMEGA"]
    factors: list[ThreatFactor]


class HeroSummary(BaseModel):
    id: int
    name: str
    real_name: str | None = None
    deck: str | None = None
    image_url: str | None = None
    thumb_url: str | None = None
    appearances: int = 0
    first_appearance: str | None = None
    publisher: str | None = None
    threat: Threat | None = None  # só existe depois que o alvo foi escaneado


class HeroDetail(HeroSummary):
    bio: str | None = None
    aliases: list[str] = []
    origin: str | None = None
    powers: list[str] = []
    teams: list[str] = []
    enemies: list[str] = []
    friends: list[str] = []
    site_url: str | None = None
    threat: Threat


class HeroPage(BaseModel):
    items: list[HeroSummary]
    next_page: int | None


class ArmorStats(BaseModel):
    power: int
    armor: int
    speed: int
    flight: int
    tech: int


class Armor(BaseModel):
    mark: int
    code: str
    name: str
    nickname: str | None = None
    year: int
    debut: str
    armor_class: str
    status: str
    description: str
    weapons: list[str]
    stats: ArmorStats
    primary_color: str
    secondary_color: str
    image_url: str | None = None


class ChatTurn(BaseModel):
    role: Literal["user", "assistant"]
    text: str


class ChatRequest(BaseModel):
    message: str
    history: list[ChatTurn] = []


class ChatResponse(BaseModel):
    reply: str
    action: str | None = None  # HOUSE_PARTY, IRON_MAN, CLEAN_SLATE, SNAP, ULTRON, DOOM_MODE, STARK_MODE, OPEN_ARMOR:44, OPEN_HERO:1455
    mode: Literal["IA", "OFFLINE"]


class CompareResult(BaseModel):
    a: HeroDetail
    b: HeroDetail
    winner_id: int | None
    probability_a: int  # 0–100
    verdict: str


class Mission(BaseModel):
    id: int
    name: str
    deck: str | None = None
    image_url: str | None = None
    issue_count: int | None = None
    first_issue: str | None = None


class MissionDetail(Mission):
    description: str | None = None
    issues: list[str] = []


class LastRain(BaseModel):
    date: str  # AAAA-MM-DD, no fuso da cidade
    days_ago: int  # 0 = hoje
    mm: float


class Weather(BaseModel):
    temperature_c: float
    last_rain: LastRain | None = None  # None = não choveu dentro da janela
    window_days: int  # quantos dias para trás foram verificados


class NewsItem(BaseModel):
    title: str
    source: str | None = None
    url: str


class Briefing(BaseModel):
    city: str | None = None  # nome resolvido pelo geocoder, ex.: "Campinas, São Paulo"
    weather: Weather | None = None
    weather_error: str | None = None
    news: list[NewsItem] = []
    news_error: str | None = None

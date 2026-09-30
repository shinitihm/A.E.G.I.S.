"""Nível de ameaça A.E.G.I.S. — a Comic Vine não tem esse dado, então nós calculamos."""

from .models import Threat, ThreatFactor

# Poderes que sozinhos já justificam alerta máximo
COSMIC_KEYWORDS = (
    "cosmic", "reality", "omnipoten", "omniscien", "time manipulation", "time travel",
    "matter manipulation", "molecular", "immortal", "phoenix", "power cosmic",
)

LEVELS = (  # (limite inferior, texto, código)
    (80, "ÔMEGA", "OMEGA"),
    (55, "ALTO", "HIGH"),
    (30, "MODERADO", "MODERATE"),
    (0, "BAIXO", "LOW"),
)


def calculate(powers: list[str], appearances: int, enemies: int, teams: int) -> Threat:
    cosmic = any(k in p.lower() for p in powers for k in COSMIC_KEYWORDS)
    factors = [
        ThreatFactor(label="Poderes", points=min(len(powers) * 4, 40), max=40),
        ThreatFactor(label="Aparições", points=min(appearances // 50, 25), max=25),
        ThreatFactor(label="Inimigos", points=min(enemies // 2, 15), max=15),
        ThreatFactor(label="Times", points=min(teams * 2, 10), max=10),
        ThreatFactor(label="Poder cósmico", points=10 if cosmic else 0, max=10),
    ]
    score = min(sum(f.points for f in factors), 100)
    _, level, code = next(lvl for lvl in LEVELS if score >= lvl[0])
    return Threat(score=score, level=level, level_code=code, factors=factors)

"""Testes sem internet: a Comic Vine é substituída por respostas falsas."""

import pytest
from fastapi.testclient import TestClient

from app import comicvine, jarvis, threat
from app.main import app

MARVEL = {"id": 31, "name": "Marvel"}
DC = {"id": 10, "name": "DC Comics"}

CHARACTERS = [
    {"id": 1, "name": "Hulk", "real_name": "Bruce Banner", "publisher": MARVEL, "count_of_issue_appearances": 3000,
     "image": {"super_url": "http://img/hulk.jpg", "small_url": "http://img/hulk_s.jpg"}},
    {"id": 2, "name": "Thor", "real_name": "Thor Odinson", "publisher": MARVEL, "count_of_issue_appearances": 2500},
    {"id": 3, "name": "Superman", "publisher": DC, "count_of_issue_appearances": 9000},
]
DETAILS = {
    1: {**CHARACTERS[0], "description": "<p>Gamma <b>rage</b></p>", "powers": [{"name": f"P{i}"} for i in range(12)],
        "teams": [{"name": "Avengers"}], "character_enemies": [{"name": "Abomination"}] * 20, "character_friends": []},
    2: {**CHARACTERS[1], "powers": [{"name": "Weather Control"}, {"name": "Cosmic Power"}],
        "teams": [], "character_enemies": [], "character_friends": []},
}


@pytest.fixture(autouse=True)
def fake_comicvine(monkeypatch):
    async def fake_get(path, **params):
        if path == "characters/":
            name = params.get("filter", "name:").split(":", 1)[1].lower()
            return [c for c in CHARACTERS if name in c["name"].lower()]
        if path.startswith("character/4005-"):
            return DETAILS[int(path.split("-")[1].strip("/"))]
        return []

    monkeypatch.setattr(comicvine, "_get", fake_get)
    monkeypatch.setattr(jarvis, "_gemini", None)  # testes sempre no modo OFFLINE
    comicvine._threats.clear()


client = TestClient(app)


def test_threat_levels():
    assert threat.calculate([], 0, 0, 0).level_code == "LOW"
    omega = threat.calculate([f"p{i}" for i in range(10)] + ["Reality Warping"], 5000, 40, 10)
    assert omega.score == 100 and omega.level == "ÔMEGA"


def test_heroes_list_only_marvel():
    body = client.get("/heroes").json()
    assert [h["name"] for h in body["items"]] == ["Hulk", "Thor"]
    assert body["next_page"] is None


def test_detail_scans_threat_and_cleans_html():
    hulk = client.get("/heroes/1").json()
    assert hulk["bio"] == "Gamma rage"
    assert hulk["threat"]["score"] == 40 + 25 + 10 + 2  # poderes + aparições + inimigos + times
    # depois de escaneado, o alvo aparece com ameaça na lista
    assert client.get("/heroes").json()["items"][0]["threat"]["level_code"] == "HIGH"


def test_compare():
    body = client.get("/compare", params={"a": "Hulk", "b": "Thor"}).json()
    assert body["winner_id"] == 1
    assert body["probability_a"] > 50
    assert "Hulk" in body["verdict"]


def test_armors():
    armors = client.get("/armors").json()
    assert len(armors) >= 15
    assert client.get("/armors/44").json()["nickname"].startswith("Hulkbuster")
    assert client.get("/armors/999").status_code == 404


@pytest.mark.parametrize("message, action", [
    ("I am Iron Man", "IRON_MAN"),
    ("jarvis, protocolo house party!", "HOUSE_PARTY"),
    ("Chama a Veronica", "OPEN_ARMOR:44"),
    ("me fala da mark 85", "OPEN_ARMOR:85"),
    ("quem é o Hulk?", "OPEN_HERO:1"),
    ("hulk vs thor", "COMPARE:hulk|thor"),
])
def test_jarvis_offline(message, action):
    body = client.post("/jarvis/chat", json={"message": message}).json()
    assert body["action"] == action
    assert body["reply"]

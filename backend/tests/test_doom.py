"""Gatilhos de troca de tema: "Sou Victor." liga o modo Dr. Doom, "Sou Tony Stark." volta ao normal."""

import pytest
from fastapi.testclient import TestClient

from app import jarvis
from app.main import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def no_ai(monkeypatch):
    monkeypatch.setattr(jarvis, "_gemini", None)  # nunca chamar a IA de verdade nos testes


@pytest.mark.parametrize("message", [
    "Sou Victor.", "sou victor", "  SOU VICTOR!  ", "Eu sou Victor", "sou o Victor von Doom",
    "Sou o Doutor Destino", "sou Doctor Doom", "Sou Dr. Doom",
])
def test_doom_trigger(message):
    body = client.post("/jarvis/chat", json={"message": message}).json()
    assert body["action"] == "DOOM_MODE"
    assert "Majestade" in body["reply"]


@pytest.mark.parametrize("message", ["Sou Tony Stark.", "sou tony", "Eu sou o Tony Stark!"])
def test_stark_trigger(message):
    assert client.post("/jarvis/chat", json={"message": message}).json()["action"] == "STARK_MODE"


@pytest.mark.parametrize("message", [
    "Sou Victor Hugo", "sou victoria", "meu amigo Victor chegou", "sou victor e quero saber do Hulk",
    "quem é o Victor von Doom?", "Sou Tony Ramos", "sou tonya",
])
def test_similar_phrases_do_not_switch_theme(message):
    assert jarvis.easter_egg(message) is None


def test_existing_easter_eggs_still_work():
    assert jarvis.easter_egg("I am Iron Man").action == "IRON_MAN"
    assert jarvis.easter_egg("house party").action == "HOUSE_PARTY"


def test_snap_and_ultron():
    assert jarvis.easter_egg("Snap!").action == "SNAP"
    assert jarvis.easter_egg("vou estalar os dedos").action == "SNAP"
    assert jarvis.easter_egg("  ULTRON. ").action == "ULTRON"
    assert jarvis.easter_egg("quem é o Ultron?") is None  # pergunta sobre o personagem não vira easter egg

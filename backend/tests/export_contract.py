"""Gera os JSONs de exemplo usados pelo teste de contrato do app Android.

Rodar (dentro de backend/):  python -m tests.export_contract
Saída: ../android/app/src/test/resources/contract/*.json

Se você mudar um modelo em app/models.py, rode de novo e o teste Java (ContractTest) acusa o que quebrou.
"""

import json
from pathlib import Path

from fastapi.testclient import TestClient

from app import comicvine, jarvis
from app.main import app
from tests.test_api import CHARACTERS, DETAILS

OUT = Path(__file__).resolve().parents[2] / "android/app/src/test/resources/contract"


async def fake_get(path, **params):
    if path == "characters/":
        name = params.get("filter", "name:").split(":", 1)[1].lower()
        return [c for c in CHARACTERS if name in c["name"].lower()]
    if path.startswith("character/4005-"):
        return DETAILS[int(path.split("-")[1].strip("/"))]
    return []


def main():
    comicvine._get = fake_get
    jarvis._gemini = None
    client = TestClient(app)
    OUT.mkdir(parents=True, exist_ok=True)

    samples = {
        "hero_detail": client.get("/heroes/1"),
        "hero_page": client.get("/heroes"),
        "compare": client.get("/compare", params={"a": "Hulk", "b": "Thor"}),
        "armors": client.get("/armors"),
        "chat_action": client.post("/jarvis/chat", json={"message": "quem é o Hulk?"}),
        "health": client.get("/health"),
    }
    for name, response in samples.items():
        assert response.status_code == 200, (name, response.text)
        (OUT / f"{name}.json").write_text(json.dumps(response.json(), ensure_ascii=False, indent=2), encoding="utf-8")
        print("gerado", name)


if __name__ == "__main__":
    main()

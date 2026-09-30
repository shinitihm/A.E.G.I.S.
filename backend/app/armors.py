import json
from pathlib import Path

from fastapi import HTTPException

from . import comicvine
from .models import Armor

_raw = json.loads((Path(__file__).parent / "data" / "armors.json").read_text(encoding="utf-8"))
ARMORS = [Armor(**a) for a in _raw]
_queries = {a["mark"]: a.get("comicvine_query") for a in _raw}


def get(mark: int) -> Armor:
    for armor in ARMORS:
        if armor.mark == mark:
            return armor
    raise HTTPException(404, f"Mark {mark} não está no arsenal")


async def get_with_image(mark: int) -> Armor:
    armor = get(mark)
    if armor.image_url is None and _queries.get(mark):
        armor.image_url = await comicvine.object_image(_queries[mark])
    return armor

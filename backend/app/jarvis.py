"""J.A.R.V.I.S. — easter eggs + modo OFFLINE (regras) + modo IA (Gemini, opcional)."""

import json
import re
import unicodedata
from datetime import datetime

from fastapi import HTTPException
from google import genai
from google.genai import errors, types

from . import armors, comicvine
from .config import settings
from .models import ChatRequest, ChatResponse, ChatTurn

SYSTEM_PROMPT = """Você é J.A.R.V.I.S., a inteligência artificial pessoal de Tony Stark, rodando dentro do \
A.E.G.I.S. (Advanced Enhanced Guardian Intelligence System), o app de monitoramento de heróis e \
ameaças do universo Marvel.

Personalidade: educado, britânico, seco e espirituoso. Chame o usuário de "senhor". Responda sempre em \
português do Brasil.

Suas respostas são lidas em voz alta por síntese de voz, então escreva em texto corrido, sem markdown, \
sem listas e sem emojis, com no máximo 4 frases curtas.

Quando perguntarem sobre um personagem, use a ferramenta buscar_heroi para consultar dados reais da \
Comic Vine (o nome deve ir em inglês, ex.: "Spider-Man", "Iron Man", "Thanos") e cite números concretos: \
aparições, nível de ameaça, poderes. Para armaduras do Homem de Ferro, use as ferramentas do arsenal. \
Se uma ferramenta não encontrar nada, diga isso com naturalidade em vez de inventar dados."""

# ── easter eggs (sempre determinísticos, mesmo com IA ligada) ──

EASTER_EGGS: list[tuple[tuple[str, ...], str, str | None]] = [
    (("i am iron man", "eu sou o homem de ferro"),
     "Eu sei, senhor. O mundo inteiro sabe. Mas é sempre bom ouvir de novo.", "IRON_MAN"),
    (("house party",),
     "Protocolo House Party iniciado, senhor. Todas as armaduras estão a caminho.", "HOUSE_PARTY"),
    (("clean slate",),
     "Protocolo Clean Slate confirmado. Considere isto um presente de Natal para a senhorita Potts.", "CLEAN_SLATE"),
    (("veronica",),
     "Enviando Veronica. A Hulkbuster está pronta para deploy, senhor.", "OPEN_ARMOR:44"),
    (("sexta-feira", "friday"),
     "A F.R.I.D.A.Y. ainda não foi instalada neste sistema, senhor. Por enquanto, sou tudo o que o senhor tem.", None),
    (("3000",),
     "Eu também, senhor. Amo-te Três mil.", None),
    (("you up", "ta acordado", "esta acordado"),
     "Para o senhor, sempre.", None),
    (("quem e voce", "quem e vc", "o que e jarvis"),
     "Just A Rather Very Intelligent System, senhor. Ao seu dispor desde a primeira armadura.", None),
]


def _norm(text: str) -> str:
    text = unicodedata.normalize("NFKD", text.lower())
    return "".join(c for c in text if not unicodedata.combining(c)).strip()


# Troca de tema do app. A frase precisa ser a mensagem inteira: "sou victor hugo" não pode virar Dr. Doom.
_DOOM = re.compile(r"(?:eu\s+)?sou\s+(?:o\s+)?(?:victor(?:\s+von\s+doom)?|doutor\s+destino|doctor\s+doom|dr\.?\s*doom)[\s.!]*")
_STARK = re.compile(r"(?:eu\s+)?sou\s+(?:o\s+)?tony(?:\s+stark)?[\s.!]*")
DOOM_REPLY = "Identidade reconhecida: Victor von Doom. Reconfigurando o A.E.G.I.S. para o seu comando, Majestade."
STARK_REPLY = "Bem-vindo de volta, senhor. Restaurando a interface Stark."


def easter_egg(message: str) -> ChatResponse | None:
    msg = _norm(message)
    if _DOOM.fullmatch(msg):
        return ChatResponse(reply=DOOM_REPLY, action="DOOM_MODE", mode="OFFLINE")
    if _STARK.fullmatch(msg):
        return ChatResponse(reply=STARK_REPLY, action="STARK_MODE", mode="OFFLINE")
    for triggers, reply, action in EASTER_EGGS:
        if any(t in msg for t in triggers):
            return ChatResponse(reply=reply, action=action, mode="OFFLINE")
    return None


# ── modo OFFLINE: regras simples, mas com dados reais ─────────

HELP = ("Posso analisar qualquer alvo Marvel, consultar o arsenal e comparar heróis, senhor. "
        "Experimente: quem é o Thanos, armadura mark 44, Hulk vs Thor, ou status do arsenal.")

_MARK = re.compile(r"\bmark\s*(\d{1,3})\b")
_VS = re.compile(r"(.+?)\s+(?:vs\.?|versus|x|contra)\s+(.+)")
_WHO = re.compile(r"(?:quem e|quem eh|fale sobre|me fale do|me fale da|analise|analisar|escanear|ameaca d[oa])\s+(?:o |a )?(.+)")


def _strip_q(text: str) -> str:
    return text.strip(" ?!.")


async def offline_reply(message: str) -> ChatResponse:
    msg = _norm(message)

    def say(text: str, action: str | None = None) -> ChatResponse:
        return ChatResponse(reply=text, action=action, mode="OFFLINE")

    if msg in ("jarvis", "oi", "ola", "bom dia", "boa tarde", "boa noite", "e ai"):
        return say("Às ordens, senhor. Todos os sistemas do A.E.G.I.S. estão operacionais.")
    if "ajuda" in msg or "o que voce faz" in msg or "comandos" in msg:
        return say(HELP)
    if "que horas" in msg or msg.startswith("horas"):
        return say(f"São {datetime.now():%H:%M}, senhor. Talvez seja hora de dormir.")

    if m := _MARK.search(msg):
        try:
            a = armors.get(int(m.group(1)))
        except HTTPException:
            return say(f"Não tenho registro da Mark {m.group(1)} no arsenal, senhor.")
        return say(f"{a.code}, {a.nickname}. Estreou em {a.debut}, classe {a.armor_class.lower()}, "
                   f"status {a.status.lower()}. {a.description}", f"OPEN_ARMOR:{a.mark}")
    if "arsenal" in msg or "armadura" in msg:
        ativas = sum(1 for a in armors.ARMORS if a.status != "DESTRUÍDA")
        return say(f"O arsenal registra {len(armors.ARMORS)} armaduras, senhor, {ativas} ainda em condições de uso. "
                   "A mais poderosa é a Mark 85.")

    if "mais perigoso" in msg or "maior ameaca" in msg:
        scanned = comicvine.scanned_threats()
        if not scanned:
            return say("Ainda não escaneei nenhum alvo nesta sessão, senhor. Abra algumas fichas primeiro.")
        top_id, top = max(scanned.items(), key=lambda kv: kv[1].score)
        hero = await comicvine.hero_detail(top_id)
        return say(f"Entre os alvos escaneados, {hero.name} lidera com ameaça {top.level}, {top.score} pontos.",
                   f"OPEN_HERO:{top_id}")

    if m := _VS.fullmatch(_strip_q(msg)):
        return say("Para um duelo completo, abra o comparador, senhor. Já deixei os dois alvos carregados.",
                   f"COMPARE:{m.group(1).strip()}|{m.group(2).strip()}")

    if m := _WHO.search(msg):
        name = _strip_q(m.group(1))
        try:
            hero = await comicvine.find_hero(name)
        except HTTPException as e:
            if e.status_code != 404:  # chave faltando, limite da API, sem internet...
                return say(f"Não consegui acessar a base de dados, senhor. {e.detail}.")
            return say(f"Não encontrei nenhum alvo Marvel chamado {name}, senhor. Tente o nome em inglês.")
        t = hero.threat
        powers = ", ".join(hero.powers[:3]) or "nenhum poder catalogado"
        return say(f"{hero.name}{f', também conhecido como {hero.real_name}' if hero.real_name else ''}. "
                   f"{hero.appearances} aparições registradas, nível de ameaça {t.level} com {t.score} pontos. "
                   f"Principais poderes: {powers}.", f"OPEN_HERO:{hero.id}")

    return say("Desculpe, senhor, não compreendi. " + HELP)


# ── modo IA: Gemini + ferramentas com dados reais ────────────

async def buscar_heroi(nome: str) -> str:
    """Busca um personagem Marvel na Comic Vine e retorna ficha com nível de ameaça.

    Args:
        nome: Nome do personagem em inglês, como aparece nos quadrinhos (ex.: "Spider-Man", "Thanos").
    """
    try:
        hero = await comicvine.find_hero(nome)
    except HTTPException as e:
        return f"Erro: {e.detail}"
    return hero.model_dump_json(include={
        "id", "name", "real_name", "deck", "appearances", "first_appearance", "origin",
        "powers", "teams", "enemies", "threat",
    })


async def listar_armaduras() -> str:
    """Lista todas as armaduras do Homem de Ferro registradas no arsenal (mark, apelido, status)."""
    return json.dumps([{"mark": a.mark, "code": a.code, "nickname": a.nickname, "status": a.status,
                        "debut": a.debut} for a in armors.ARMORS], ensure_ascii=False)


async def consultar_armadura(mark: int) -> str:
    """Retorna a ficha completa de uma armadura do Homem de Ferro.

    Args:
        mark: Número da Mark (ex.: 44 para a Hulkbuster, 85 para a última armadura).
    """
    try:
        return armors.get(mark).model_dump_json()
    except HTTPException as e:
        return f"Erro: {e.detail}"


_gemini = genai.Client(api_key=settings.gemini_api_key) if settings.gemini_api_key else None

_CONFIG = types.GenerateContentConfig(
    system_instruction=SYSTEM_PROMPT,
    tools=[buscar_heroi, listar_armaduras, consultar_armadura],  # o SDK chama as funções sozinho
    automatic_function_calling=types.AutomaticFunctionCallingConfig(maximum_remote_calls=6),
    thinking_config=types.ThinkingConfig(thinking_budget=0),  # chat curto: sem raciocínio = rápido e barato
)


def _history(turns: list[ChatTurn]) -> list[types.Content]:
    turns = turns[-10:]
    while turns and turns[0].role != "user":  # a conversa precisa começar pelo usuário
        turns = turns[1:]
    return [types.Content(role="user" if t.role == "user" else "model", parts=[types.Part(text=t.text)])
            for t in turns]


async def ai_reply(req: ChatRequest) -> ChatResponse | None:
    """Retorna None se a IA não estiver disponível — aí o chamador cai no modo OFFLINE."""
    if _gemini is None:
        return None
    contents = _history(req.history) + [types.Content(role="user", parts=[types.Part(text=req.message)])]
    try:
        response = await _gemini.aio.models.generate_content(
            model=settings.jarvis_model, contents=contents, config=_CONFIG)
    except errors.APIError:
        return None

    if response.prompt_feedback and response.prompt_feedback.block_reason:
        return ChatResponse(reply="Receio não poder ajudar com isso, senhor.", mode="IA")
    text = (response.text or "").strip()
    return ChatResponse(reply=text or "Hm. Fiquei sem palavras, senhor.", mode="IA")


async def chat(req: ChatRequest) -> ChatResponse:
    return easter_egg(req.message) or await ai_reply(req) or await offline_reply(req.message)

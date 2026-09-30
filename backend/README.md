# A.E.G.I.S. — Backend (FastAPI)

O app Android **não** fala direto com a Comic Vine nem com a IA. Ele fala com este servidor, que:

- guarda as chaves de API (elas nunca vão dentro do APK);
- faz cache das respostas da Comic Vine (limite de ~200 req/h por recurso);
- calcula o **nível de ameaça** de cada herói;
- serve as armaduras do Homem de Ferro (`app/data/armors.json`);
- roda a **J.A.R.V.I.S.** (regras offline ou IA de verdade com Gemini).

## Rodar

**Windows, jeito fácil:** dê dois cliques em `rodar.bat`. Na primeira vez ele cria o ambiente, instala as dependências
e abre o `.env` para você colar a chave. Ou, na mão:

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate            # Windows   (Linux/Mac: source .venv/bin/activate)
pip install -r requirements.txt
copy .env.example .env            # Linux/Mac: cp .env.example .env   → preencha as chaves
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Documentação interativa: <http://localhost:8000/docs>

`--host 0.0.0.0` é necessário para o celular/emulador enxergar o servidor.

| Onde o app roda | `aegis.api.url` (em `android/local.properties`) |
|---|---|
| Emulador do Android Studio | `http://10.0.2.2:8000/` (é o padrão) |
| Celular na mesma rede Wi-Fi | `http://IP-DO-SEU-PC:8000/` (`ipconfig` para ver o IP; libere a porta 8000 no firewall) |

## Chaves (`.env`)

| Variável | Obrigatória | Para quê |
|---|---|---|
| `COMIC_VINE_API_KEY` | sim | <https://comicvine.gamespot.com/api/> |
| `GEMINI_API_KEY` | não | <https://aistudio.google.com/apikey> — liga a J.A.R.V.I.S. com IA. Sem ela ela funciona no modo **OFFLINE** (regras + dados reais). |
| `JARVIS_MODEL` | não | Modelo usado no modo IA (padrão `gemini-2.5-flash`). |

`GET /health` mostra se as chaves foram lidas e em que modo a J.A.R.V.I.S. está.

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| GET | `/health` | Status do servidor |
| GET | `/heroes?page=0` | Lista paginada (só Marvel, mais famosos primeiro) |
| GET | `/heroes/search?q=wolverine` | Busca de novos alvos |
| GET | `/heroes/{id}` | Ficha completa + **nível de ameaça** |
| GET | `/heroes/target-of-the-day` | Alvo do dia (widget) |
| GET | `/compare?a=Hulk&b=Thor` | Simulador de combate com veredito |
| GET | `/missions` · `/missions/{id}` | Arcos de história (Guerra Civil, Guerra Infinita…) |
| GET | `/armors` · `/armors/{mark}` | Arsenal do Homem de Ferro |
| POST | `/jarvis/chat` | Conversa com a J.A.R.V.I.S. |

Erros vêm como `{"detail": "mensagem"}` e o app mostra a mensagem na tela.

## Nível de ameaça (`app/threat.py`)

Soma de 5 fatores, limitada a 100:

| Fator | Regra | Máx |
|---|---|---|
| Poderes | 4 pts por poder | 40 |
| Aparições | 1 pt a cada 50 edições | 25 |
| Inimigos | 1 pt a cada 2 inimigos | 15 |
| Times | 2 pts por time | 10 |
| Poder cósmico | +10 se tiver realidade/cósmico/tempo/onipotência… | 10 |

`0–29 BAIXO` · `30–54 MODERADO` · `55–79 ALTO` · `80–100 ÔMEGA`

Os pesos são fáceis de ajustar no arquivo. Teste com Thanos, Hulk, Wolverine e Homem-Aranha e veja se o ranking faz sentido.

## J.A.R.V.I.S

Ordem de decisão em `POST /jarvis/chat`:

1. **Easter eggs** (sempre, mesmo com IA): `I am Iron Man`, `House Party`, `Clean Slate`, `Veronica`, `3000`…
2. **Modo IA** (se `GEMINI_API_KEY` existir): Gemini com 3 ferramentas — `buscar_heroi`, `listar_armaduras`, `consultar_armadura` — então responde com dados reais. Se a chamada falhar, cai no passo 3.
3. **Modo OFFLINE**: regras por palavra-chave (`quem é o Thanos`, `mark 44`, `hulk vs thor`, `status do arsenal`…).

A resposta traz `action` quando o app deve fazer algo: `HOUSE_PARTY`, `IRON_MAN`, `CLEAN_SLATE`, `OPEN_ARMOR:44`, `OPEN_HERO:1455`, `COMPARE:hulk|thor`.

## Testes

```bash
pytest                              # não usa internet: a Comic Vine é simulada
python -m tests.export_contract     # regera os JSONs do teste de contrato do app Android
```

## Observações

- O cache fica em memória: reiniciar o servidor limpa. Para a apresentação, deixe o servidor rodando e abra as telas uma vez antes.
- O endpoint `/missions` faz várias buscas na primeira vez (uma por arco) e demora alguns segundos.
- Para colocar online (Render, Railway, Fly.io…), use HTTPS, configure as chaves como variáveis de ambiente do serviço e troque `aegis.api.url` no `local.properties`.

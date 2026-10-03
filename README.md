# A.E.G.I.S. — App Android (Java)

Projeto gerado pelo Android Studio (`com.example.aegis`, Java 11, minSdk 24, AGP 9). Abra **esta pasta (`android/`)** no Android Studio.

## Antes de rodar

1. Suba o backend (veja `../backend/README.md`).
2. Se for usar celular real, crie/edite `android/local.properties` (o Android Studio já gera esse arquivo com o `sdk.dir`; só acrescente a linha):

   ```properties
   aegis.api.url=http://IP-DO-SEU-PC:8000/
   ```

   No emulador não precisa: o padrão é `http://10.0.2.2:8000/`.
3. **Sync Project with Gradle Files** e ▶ Run.

> Se o app abrir mas as telas mostrarem "SEM CONEXÃO COM O SERVIDOR A.E.G.I.S.", é o backend desligado ou o IP errado.

## Digital no emulador

`⋮ (Extended controls) → Fingerprint` → **Touch sensor**. Cadastre uma digital em *Settings → Security → Fingerprint* do emulador
e, quando o scanner do A.E.G.I.S. abrir, clique em **Touch the sensor** no painel.
Sem digital cadastrada, o botão **ACESSO DE EMERGÊNCIA** usa o PIN/padrão do aparelho.

## Estrutura

```
app/src/main/java/com/example/aegis/
├── SplashActivity        boot: reator arc + log de terminal
├── AuthActivity          scanner de digital (BiometricPrompt)
├── MainActivity          HUD com 5 abas (Fragments mantidos em memória)
├── data/                 Retrofit (AegisApi, ApiClient, ApiCallback), LocalStore (SharedPreferences), model/
├── ui/hud/               componentes visuais: HudGridView, ArcReactorView, ScanRingView, ThreatMeterView,
│                         StatRadarView, WaveView, IronHelmetView, StateView + Hud (helpers) + Speaker (TTS)
├── ui/heroes/            lista, ficha (dossiê) e comparador Hulk vs Thor
├── ui/search/            busca de novos alvos (debounce + cancelamento)
├── ui/jarvis/            chat: texto, voz (STT), resposta falada (TTS), easter eggs
├── ui/arsenal/           Hall of Armor, ficha da armadura e Protocolo House Party
├── ui/missions/          arquivos de missão (story arcs)
└── widget/               widget "Alvo do dia"
```

## Como cada ideia foi feita

| Ideia | Onde |
|---|---|
| Splash + digital | `SplashActivity`, `AuthActivity` |
| Nível de ameaça | calculado no backend (`threat.py`), exibido em `HeroAdapter`, `HeroDetailActivity`, `CompareActivity` |
| Busca de novos heróis | `SearchFragment` → `GET /heroes/search` |
| Monitorar alvos (favoritos) | botão na ficha → `LocalStore` (aparelho) → filtro **MONITORADOS** |
| Arsenal | `ArsenalFragment`, `ArmorDetailActivity` — dados em `backend/app/data/armors.json` |
| **House Party** | botão no Arsenal **ou** digitar "house party" pra J.A.R.V.I.S. |
| **Hulk vs Thor** | botão **COMPARAR** na aba Alvos (ou "Hulk vs Thor" no chat) |
| **Missões** | aba MISSÕES (story arcs) |
| **Voz** | 🔊 liga/desliga a resposta falada (TextToSpeech); 🎤 fala a pergunta (reconhecimento de voz do Google) |
| **Widget** | segure na tela inicial → Widgets → A.E.G.I.S. (toque abre o app pela splash, então a biometria continua obrigatória) |
| **Easter eggs** | no chat: `I am Iron Man`, `Veronica`, `Clean Slate`, `3000`, `sexta-feira`, `snap` (metade dos alvos vira pó), `Ultron` (glitch)… |

## Testes

`./gradlew testDebugUnitTest` — `ContractTest` lê os JSONs gerados pelo FastAPI (`app/src/test/resources/contract/`)
e confere que os modelos Java (Gson, `snake_case` → `camelCase`) continuam batendo com a API.
Se mudar `backend/app/models.py`, rode `python -m tests.export_contract` no backend.

## Limitações conhecidas

- A Comic Vine quase não tem imagens de armaduras; o Arsenal desenha o capacete em código (`IronHelmetView`), com as cores de cada Mark.
- Reconhecimento de voz depende do app do Google no aparelho; sem ele o botão 🎤 avisa e o resto continua funcionando.
- Os ícones antigos (`mipmap-*.webp`) para Android 7.x são os padrão do Android Studio; do Android 8 em diante o ícone é o reator arc.
- `usesCleartextTraffic` está ligado porque o backend de desenvolvimento usa `http://`. Em produção use HTTPS e desligue.

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

1. **Easter eggs** (sempre, mesmo com IA): `I am Iron Man`, `House Party`, `Clean Slate`, `Veronica`, `3000`, `snap`, `Ultron`…
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

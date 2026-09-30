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
| **Easter eggs** | no chat: `I am Iron Man`, `Veronica`, `Clean Slate`, `3000`, `sexta-feira`… |

## Testes

`./gradlew testDebugUnitTest` — `ContractTest` lê os JSONs gerados pelo FastAPI (`app/src/test/resources/contract/`)
e confere que os modelos Java (Gson, `snake_case` → `camelCase`) continuam batendo com a API.
Se mudar `backend/app/models.py`, rode `python -m tests.export_contract` no backend.

## Limitações conhecidas

- A Comic Vine quase não tem imagens de armaduras; o Arsenal desenha o capacete em código (`IronHelmetView`), com as cores de cada Mark.
- Reconhecimento de voz depende do app do Google no aparelho; sem ele o botão 🎤 avisa e o resto continua funcionando.
- Os ícones antigos (`mipmap-*.webp`) para Android 7.x são os padrão do Android Studio; do Android 8 em diante o ícone é o reator arc.
- `usesCleartextTraffic` está ligado porque o backend de desenvolvimento usa `http://`. Em produção use HTTPS e desligue.

# A.E.G.I.S. — Planejamento do Projeto

**A**DVANCED **E**NHANCED **G**UARDIAN **I**NTELLIGENCE **S**YSTEM

> App Android "de uso pessoal do Tony Stark": banco de dados de heróis com nível de ameaça,
> busca na Comic Vine API, chat com a J.A.R.V.I.S. e o arsenal completo das armaduras do Homem de Ferro.
> Visual cyber / HUD holográfico.

**Entrega:** projeto compactado + vídeo de apresentação **anônimo** — **Out/2026**.

---

## 1. Stack recomendada

| Camada | Escolha | Por quê |
|---|---|---|
| Linguagem | **Kotlin** | Padrão atual do Android; o exemplo Retrofit do enunciado já é em Kotlin |
| UI | **Jetpack Compose** + Material 3 | Animações (HUD, scanner, glitch) são muito mais fáceis que em XML |
| Arquitetura | **MVVM** (ViewModel + StateFlow) + Repository | Separação limpa, fácil de explicar na apresentação |
| Rede | **Retrofit** + OkHttp + Gson/Moshi | Interceptor injeta `api_key`, `format=json` e `User-Agent` em toda requisição |
| Imagens | **Coil** | Carrega as imagens da Comic Vine com cache |
| Cache local | **Room** | Favoritos, heróis já buscados, histórico do chat — e economiza o limite de requisições da API |
| Biometria | **androidx.biometric** (`BiometricPrompt`) | Autenticação com digital |
| Splash | **SplashScreen API** (Android 12+) + tela animada em Compose | |
| Navegação | Navigation Compose | |
| DI (opcional) | Hilt ou injeção manual simples | Manual é suficiente para o tamanho do projeto |

`minSdk 26`, `targetSdk` atual.

---

## 2. Fluxo do app

```
Splash (boot do sistema)
   └─► Autenticação biométrica ("Identificando... Tony Stark")
          ├─ sucesso ─► HUD principal (bottom nav)
          │               ├─ BANCO DE DADOS (heróis + ameaça)
          │               ├─ BUSCA (novos alvos na Comic Vine)
          │               ├─ J.A.R.V.I.S. (chat)
          │               └─ ARSENAL (armaduras Mark I → ...)
          └─ falha ─► "ACESSO NEGADO" + tentar de novo / fallback PIN
```

---

## 3. Módulos e tarefas

### 3.1 Splash screen
- [ ] Fundo preto, grid holográfico, logo A.E.G.I.S. montando-se linha a linha
- [ ] Texto de boot estilo terminal: `INITIALIZING ARC REACTOR... OK`, `CONNECTING TO STARK SATELLITE... OK`
- [ ] Reator arc pulsando (Canvas + `infiniteTransition`)
- [ ] Duração ~2,5 s (usar esse tempo para pré-carregar dados do Room)

### 3.2 Autenticação por digital
- [ ] `BiometricManager.canAuthenticate()` → se não houver biometria, fallback para PIN / credencial do aparelho (`DEVICE_CREDENTIAL`)
- [ ] Tela com ícone de digital animado (anel de scanner girando, linha de varredura)
- [ ] Sucesso: "IDENTIDADE CONFIRMADA — BEM-VINDO, SR. STARK" + vibração (haptic)
- [ ] Falha: tela pisca vermelho, "ACESSO NEGADO"
- [ ] **Dica:** no emulador dá pra simular digital em *Extended Controls → Fingerprint* (bom para gravar o vídeo)

### 3.3 Banco de dados de heróis (`/characters`)
- [ ] Lista com cards estilo "ficha" (foto, codinome, nome real, barra de ameaça)
- [ ] Filtros em chips: Todos · Heróis · Vilões · Ameaça Alta · Favoritos
- [ ] Filtrar só Marvel no client: `publisher.name == "Marvel Comics"`
- [ ] Paginação infinita com `limit`/`offset`
- [ ] Usar `field_list` para pedir só os campos necessários
- [ ] **Tela de detalhes** — "dossiê":
  - [ ] Foto com moldura HUD e efeito de scan
  - [ ] Nome real, primeira aparição, nº de aparições, editora
  - [ ] Poderes (`/powers`), times (`/teams`), aliados e inimigos
  - [ ] Bio (`deck` + `description` limpando o HTML)
  - [ ] **Nível de ameaça** (ver seção 4)
  - [ ] Botão "Perguntar à J.A.R.V.I.S. sobre este alvo"

### 3.4 Busca de novos heróis
- [ ] Barra de busca com debounce (~400 ms) → `/search?resources=character&query=...` ou `/characters?filter=name:...`
- [ ] Animação de "rastreamento" enquanto carrega (radar)
- [ ] Botão "Adicionar ao banco de dados" → salva no Room (vira favorito / monitorado)
- [ ] Estado vazio: "NENHUM ALVO ENCONTRADO"

### 3.5 J.A.R.V.I.S. (chat)
- [ ] UI de chat com bolhas holográficas e efeito de digitação (texto aparecendo letra a letra)
- [ ] Onda de áudio animada enquanto "pensa"
- [ ] Histórico salvo no Room
- [ ] Sugestões rápidas: "Qual o herói mais perigoso?", "Status das armaduras", "Quem é o Wolverine?"
- [ ] **Extra:** voz — `SpeechRecognizer` (falar) + `TextToSpeech` (J.A.R.V.I.S. responde em voz)

Duas abordagens (escolher uma):

| | A) Offline / regras | B) IA de verdade (LLM) |
|---|---|---|
| Como | Detecta intenções por palavra-chave e responde usando os dados da Comic Vine + Room | Envia a pergunta + contexto (dados do herói) para um modelo de linguagem |
| Prós | Grátis, não quebra na apresentação, zero risco com chave | Conversa natural, efeito "uau" enorme |
| Contras | Menos flexível | Custo, precisa de internet, **chave de API não pode ficar no APK** (qualquer um extrai) |
| Recomendação | **Fazer primeiro** — é o "piso" garantido | Colocar por cima se sobrar tempo, com a chave fora do app (backend simples) ou só no build local de demo |

A abordagem A já fica ótima se a J.A.R.V.I.S. responder com **dados reais** da API
("O Hulk aparece em 3.412 edições, nível de ameaça ÔMEGA, senhor.").

### 3.6 Arsenal — armaduras do Homem de Ferro
A Comic Vine não tem um endpoint pronto de "armaduras", então:
- [ ] Criar um **JSON local** (`assets/armors.json`) com as armaduras: Mark I, II, III, V (maleta), VI, VII, XVI Nightclub, XXXVIII Igor, XLII, XLIV Hulkbuster, XLVI, L (Bleeding Edge), LXXXV...
- [ ] Campos: `mark`, `nome`, `ano/filme`, `tipo` (combate, furtiva, pesada, espacial...), `armas`, `pontos fortes`, `status` (ativa / destruída / arquivada), `stats` (poder, velocidade, blindagem, voo), `imagem`
- [ ] **Verificar** o endpoint `/objects` da Comic Vine — alguns itens (ex.: "Hulkbuster Armor") existem lá e dá pra puxar descrição/imagem de verdade
- [ ] UI: carrossel 3D / "vitrine" do Hall of Armor, cada armadura em um pedestal com luz
- [ ] Detalhes: **gráfico radar** de stats, lista de armamentos, botão "Comparar" duas armaduras
- [ ] Botão "Deploy" com animação (só visual, mas rende muito na apresentação)

---

## 4. Nível de ameaça (fórmula própria)

A API não fornece isso — então calculamos (e isso conta como **criatividade**):

```
score = poderes      * 4      (nº de poderes, máx 40)
      + aparições    / 50     (issues, máx 25)
      + inimigos     * 0.5    (máx 15)
      + times        * 2      (máx 10)
      + bônus_poder           (+10 se tiver: Reality Manipulation, Cosmic Power, Omnipotence, Time Travel...)
score limitado a 0–100
```

| Faixa | Classe | Cor |
|---|---|---|
| 0–29 | BAIXO | ciano |
| 30–54 | MODERADO | amarelo |
| 55–79 | ALTO | laranja |
| 80–100 | ÔMEGA | vermelho pulsando |

Pesos ajustáveis — testar com Thanos, Hulk, Wolverine, Homem-Aranha e ver se o ranking "faz sentido".

---

## 5. Identidade visual (cyber / HUD Stark)

- **Cores:** fundo `#05070D`, ciano holográfico `#00E5FF`, dourado Stark `#FFB300`, vermelho alerta `#FF1744`
- **Fontes:** *Orbitron* ou *Rajdhani* (títulos), *JetBrains Mono* / *Share Tech Mono* (dados)
- **Elementos:** cantos cortados (chanfrados), linhas finas com glow, grid de fundo, scanlines, textos com efeito glitch, números "contando" até o valor
- **Som (extra):** bips curtos em toques e no sucesso da biometria
- Criar um `AegisTheme` e componentes reutilizáveis: `HudCard`, `HudButton`, `ThreatBar`, `ScanOverlay`, `GlitchText`

---

## 6. Estrutura de pastas

```
app/src/main/java/com/aegis/
├── data/
│   ├── remote/      ComicVineService, ApiInterceptor, DTOs
│   ├── local/       AegisDatabase, DAOs, entidades (Room)
│   ├── armors/      leitura do armors.json
│   └── repository/  CharacterRepository, ArmorRepository, JarvisRepository
├── domain/          ThreatCalculator, modelos de domínio
├── ui/
│   ├── theme/       AegisTheme, cores, tipografia
│   ├── components/  HudCard, ThreatBar, ScanOverlay, GlitchText...
│   ├── splash/
│   ├── auth/
│   ├── heroes/      lista + detalhe
│   ├── search/
│   ├── jarvis/
│   └── arsenal/
└── MainActivity.kt
```

**Chave da API:** guardar em `local.properties` (que não vai pro Git) e expor via `BuildConfig.COMIC_VINE_KEY`.
Lembrar de **remover a chave** do projeto compactado da entrega, ou deixar instrução de onde colocar.

---

## 7. Cuidados com a Comic Vine API

- Sempre `format=json` + header `User-Agent` descritivo (senão bloqueia / volta XML)
- Limite de ~**200 requisições por recurso por hora** → cachear no Room é obrigatório
- `description` vem em HTML → limpar com `Html.fromHtml` ou Jsoup
- Endpoints de detalhe usam ID com prefixo: `/character/4005-1455/`
- Tratar erro de rede com tela temática ("CONEXÃO COM SATÉLITE PERDIDA — tentar novamente")

---

## 8. Cronograma sugerido (prazo curto — Out/2026)

| Semana | Foco | Entregável |
|---|---|---|
| **1** | Setup Android Studio, tema, Retrofit + interceptor, lista de heróis funcionando | Lista puxando da API |
| **2** | Detalhe do herói, busca, Room (cache + favoritos), cálculo de ameaça | Núcleo de dados completo |
| **3** | Splash, biometria, Arsenal (JSON + telas), J.A.R.V.I.S. (versão regras) | App completo "feio-bonito" |
| **4** | Polimento visual (animações, glitch, sons), testes em aparelho real, vídeo anônimo, compactar | **Entrega** |

**Ordem de prioridade se o tempo apertar:** API + lista + detalhe → busca → ameaça → biometria/splash → arsenal → J.A.R.V.I.S. → extras.

---

## 9. Ideias extras (se sobrar tempo)

- **Protocolo "House Party":** botão que "convoca" todas as armaduras com animação
- **Mapa de ameaças:** `/locations` + mapa mostrando onde cada vilão atua
- **Comparador de heróis:** "Hulk vs Thor" lado a lado com stats e veredito da J.A.R.V.I.S.
- **Arcos de história** (`/story_arcs`): "Arquivos de missão" — Civil War, Guerra Infinita...
- **Times** (`/teams`): "Iniciativa Vingadores" com os membros
- **Widget** na tela inicial com o "Alvo do dia"
- **Modo alerta:** notificação quando um herói monitorado aparece em uma nova edição
- **Easter eggs:** digitar "I am Iron Man" no chat; tocar 5x no reator arc na splash; "Veronica" no arsenal abre a Hulkbuster
- **Modo Sexta-Feira (F.R.I.D.A.Y.):** tema alternativo / troca de assistente
- **Vibração + som** sincronizados com o scanner de digital

---

## 10. Critérios de avaliação × como o A.E.G.I.S. atende

| Critério | Resposta |
|---|---|
| Experiência de uso | Fluxo guiado (splash → auth → HUD), bottom nav clara, estados de loading/erro temáticos |
| Design visual | Identidade HUD Stark consistente em todas as telas |
| Uso da API | `/characters`, `/search`, `/powers`, `/teams`, `/objects`, `/story_arcs` |
| Criatividade | Nível de ameaça próprio, J.A.R.V.I.S., arsenal, biometria |
| Funcionalidade | Cache Room, tratamento de erros, funciona offline com o que já foi visto |
| Apresentação | Narrativa: "você é o Tony Stark acessando o sistema" — o vídeo já começa no scanner de digital |

---

## 11. Vídeo de apresentação (anônimo!)

- Gravar a tela do emulador/aparelho — **sem rosto, sem nome, sem voz identificável** (usar TTS ou só legendas)
- Roteiro: boot → digital → dossiê de um herói ÔMEGA → busca de um novo alvo → conversa com a J.A.R.V.I.S. → Hall of Armor → "House Party Protocol"
- Conferir que nenhuma tela mostra nome de conta/usuário do Android

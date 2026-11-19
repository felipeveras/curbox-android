# Redesign "Dark Glass" do Curbox (issue #1)

## Contexto
O app parece antiquado: Material 3 claro, verde-água, cards chapados, barra inferior padrão, onboarding em Coolvetica. O pedido é modernizar no estilo **Opal** (escuro, glassmorfismo, brilho suave, números grandes) com a paleta azul `#3b82f6` do CSS enviado ("Modern Minimal": fundo `#171717`, card `#262626`, borda `#404040`, destrutivo `#ef4444`, gráficos em escala de azul).

Estado do git: `claude/practical-dijkstra-l68dst` == `kt-rewrite` (`ec75d87`, PR #3 consolidação v11). Não há nada a mergear. `origin/cross_sync` é histórico divergente (832 commits distintos) e fica fora do escopo. O redesign é o backlog da issue #1. O trabalho segue o fluxo da `CONSOLIDATION.md`: worktree a partir de `origin/kt-rewrite`, PR draft, sem force push, e teste em dispositivo como etapa separada.

## Objetivos
- Visual dark-first com vidro, transparência, brilho e hierarquia tipográfica forte em todo o app.
- Home com anel de pontuação/tempo grande, barra inferior flutuante translúcida, lista de grupos/apps e onboarding redesenhados.
- Os demais ~80 layouts herdam o novo tema sem editar um a um.

## Não-objetivos / anti-objetivos
- Sem Jetpack Compose nem Navigation (`agents.md`). Só Views, XML e ViewBinding.
- Sem mudança de lógica do serviço de acessibilidade, de schema Room ou de fluxo de bloqueio.
- Sem redesenhar todas as telas de uma vez (risco de regressão).
- Não pode ficar ilegível ou pesado. Contraste AA e fonte ampliada continuam funcionando.
- Só strings em inglês, conforme `agents.md`.

## Restrições técnicas
- minSdk 26, Material 1.12.0, Kotlin, ViewBinding, JVM 1.8.
- Desfoque real só existe no Android 12+ (`RenderEffect` e `blurBehind` de janela). Abaixo disso, vidro "falso".
- Overlays de bloqueio e o contador cerebral rodam sobre outros apps e precisam continuar leves.

## Abordagens consideradas
1. Só tokens/tema: seguro, mas ganho só de "pele".
2. **Escolhida: tokens + telas principais redesenhadas.** Maior impacto visível por esforço, e as demais telas herdam o tema.
3. Todas as 90 telas: coerente, mas com risco alto de regressão no app.

## Decisões do usuário
- Caminho 2.
- Cor de destaque: azul `#3b82f6`.

## Sistema visual (minhas decisões, ajustáveis)
- **Tokens** em `res/values/colors.xml` e `res/values-night/colors.xml`, tema dark-first. O night é o tema principal. O modo claro recebe uma versão "frosted" simples:
  - fundo `#0F1115`, quase preto, com gradiente aurora azul/índigo sutil;
  - superfície vidro `#FFFFFF` a ~8% de alfa, borda `#FFFFFF` a ~14%;
  - primary `#3b82f6`, glow `#60a5fa`, destrutivo `#ef4444`, texto `#E5E5E5`, texto secundário `#A3A3A3`.
- **Tipografia:** manter Inter. Escala com números grandes e leves para o hero (Home: tempo total). Coolvetica fica só no onboarding.
- **Componentes (estilos/drawables):** `Widget.Curbox.GlassCard` (vidro + borda de 1dp + raio de 24dp), botão primário com glow, chips-pílula, divisores finos e switches/sliders com cor de destaque.
- **Desfoque real (API 31+):** diálogos (`Theme.Curbox.TransparentDialog`), a barra inferior flutuante e os overlays de aviso. Abaixo disso, fallback translúcido.

## Telas principais (estrutura nova)
1. **Home / uso de tela:** fundo aurora, anel de progresso com o tempo grande no centro, três chips de métricas (como Sleep/Focus/Rest do Opal), cards de vidro com atalhos. O anel usa dados que o app já calcula. A fórmula de uma "nota" nova fica para o design de detalhe e não entra de surpresa.
2. **Barra inferior flutuante:** `activity_fragment.xml` + `bottom_nav_menu`. Pílula translúcida com margem e cantos totalmente arredondados, em vez da barra Material padrão.
3. **Lista de grupos/apps** (`fragment_app_blocker_groups`, `item_app_group`, `app_usage_item`): cards de vidro e status em chips.
4. **Onboarding** (`fragment_onboarding*`): hero escuro com brilho.
5. **Diálogos e overlays de bloqueio** (`dialog_warning_overlay`, `mindfulmsg_overlay`, `overlay_brain_counter`): vidro, sem aumentar o custo.

## Etapas de execução
0. **Referências e mockups primeiro.** Publicar mockups HTML das 5 telas como Artifact (seguindo a skill `artifact-design`) para você aprovar o visual antes de mexer no código. Incluir um moodboard com as referências (Opal, Modern Minimal) e a paleta.
1. Criar um worktree a partir de `origin/kt-rewrite` (`feat/issue-1-dark-glass`).
2. Tokens, tema e estilos base, mais drawables de vidro. Verificar as telas que herdam o tema.
3. Barra inferior flutuante e Home.
4. Lista de grupos, onboarding e diálogos/overlays.
5. Refinar o resto das telas conforme os problemas que aparecerem.
6. PR draft com screenshots antes/depois.

## Verificação
- `python3 tools/validate_ptbr_resources.py`
- `./gradlew testFullDebugUnitTest -PcurboxTestArm64=true --no-daemon --max-workers=2` (`ScriptLanguageTest.matchesRegexSupportsCommonFlags` é falha conhecida de baseline).
- `./gradlew assembleFullDebug -PcurboxTestArm64=true --no-daemon --max-workers=2`
- Capturas de tela no emulador (API 26 e 34+, claro/escuro, fonte ampliada) e comparação com os mockups.
- Teste em dispositivo físico, como etapa separada e relatada à parte: o overlay de bloqueio e o contador cerebral continuam aparecendo e legíveis, e o desempenho não piora.

## Deixado ao implementador
Valores exatos de raio e alfa, nomes de estilos, ícones novos e animações sutis (brilho do anel, transição da barra), desde que respeitem o sistema acima.

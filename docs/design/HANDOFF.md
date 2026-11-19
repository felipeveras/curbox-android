# Handoff: redesign Dark Glass (issue #1)

Plano aprovado: `docs/superpowers/specs/2026-11-19-dark-glass-redesign-design.md`.
Mockups aprovados (abrir no navegador): `docs/design/curbox-dark-glass-mockups.html`.
Versão publicada: https://claude.ai/artifact/EnSG5sLeSp2vNwcrdmVB3n

## Decisões do usuário
- Caminho: sistema visual (tema) + telas principais (Início, barra inferior flutuante, Grupos, Onboarding, avisos/overlays).
- Cor de destaque: azul `#3b82f6`. Estilo Opal: escuro, vidro, brilho suave.
- Anel de tempo de tela na Início: traço contínuo com degradê. NÃO usar anel de pontos (o usuário rejeitou).
- Esfera de pontos no onboarding: mantida por enquanto (remete ao cérebro ASCII do app).
- Tipografia dos números grandes: Sora leve (300). Texto: Inter. Rótulos em minúsculas, sem caixa alta espaçada.
- Sem Compose nem Navigation (`agents.md`). Só Views/XML/ViewBinding. Strings só em inglês.

## Achados do código (nada alterado ainda)
- Tema: `app/src/main/res/values/{colors,themes}.xml` e `values-night/`. Cores `md_theme_*` (hoje verde-água). Layouts herdam por `?attr/colorSurface*`.
- `activity_fragment.xml`: `BottomNavigationView` simples; `FragmentActivity.kt` trata insets (nav lida com o inset inferior).
- Cards usam `app:cardBackgroundColor` explícito (ex.: `item_app_group.xml`), então estilo global não os sobrescreve; editar os layouts chave.
- Telas: `fragment_all_app_usage.xml` (Início), `fragment_app_blocker_groups.xml`, `item_app_group.xml`, `fragment_onboarding*.xml`, `dialog_warning_overlay.xml`, `mindfulmsg_overlay.xml`, `overlay_brain_counter.xml`.
- Desfoque real só no Android 12+; abaixo disso, vidro translúcido.

## Próximos passos
1. Tokens: `md_theme_*` claro/escuro, cores de vidro, drawables `bg_aurora`, `bg_glass_card`, `bg_glass_nav`.
2. Barra inferior flutuante + Início com anel.
3. Grupos, onboarding, diálogos/overlays.
4. Build `assembleFullDebug -PcurboxTestArm64=true`, instalar via ADB no celular conectado, capturar telas.
5. PR draft com antes/depois (só quando o usuário pedir).

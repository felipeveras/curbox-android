# Base consolidada v11

Esta é uma fotografia do trabalho local existente sobre `90444ce`, não uma nova implementação nem declaração de estabilidade. Fork e upstream `kt-rewrite` tinham o mesmo HEAD antes da consolidação (divergência 0/0). Inclui PT-BR, destino alternativo, opt-in de 5 segundos, pesquisa por nome/pacote, interface Material original e contador cerebral v11. As alterações têm dependências em recursos, configurações e testes compartilhados; são preservadas em um snapshot honesto, sem inventar uma sequência histórica de funcionalidades independentes.

## Limitações conhecidas

- O usuário relata que o contador continua bugado após v11, ainda sem reprodução específica: [issue #2](https://github.com/felipeveras/curbox-android/issues/2). Causa não estabelecida.
- O detector retorna `null` tanto para raiz de acessibilidade indisponível como para ausência de resultado. O tracker pode interpretar isso como saída e limpar o comparador. Não corrigir silenciosamente: obter evidência no dispositivo e escrever regressão.
- Detecção reativa não garante ausência do primeiro frame do app bloqueado. Sem validação física nesta consolidação.
- `ScriptLanguageTest.matchesRegexSupportsCommonFlags` é uma falha conhecida de baseline; registrar resultado e exceção de cada execução separadamente, não declarar CI verde.
- O redesign da [issue #1](https://github.com/felipeveras/curbox-android/issues/1) permanece backlog aberto e fora do escopo. Não iniciar nem fechar por esta consolidação.
- `REDIRECT_FEATURE.md` e `PTBR_TRANSLATION_REPORT.md` são registros históricos de diferentes APKs. Números de testes e metadados antigos não descrevem automaticamente o novo commit; há seções históricas com controles/estágios já substituídos. A seção v11 e o código atual prevalecem. Caminhos `<LOCAL>` representam evidência privada, não arquivos necessários ao build.

## Fluxo obrigatório de desenvolvimento

1. Issue com problema/aceite e investigação, sem presumir causa a partir de relato.
2. Partir de base identificada e limpa: `git fetch origin`, `git worktree add -b fix/issue-N ../curbox-issue-N origin/kt-rewrite`. Não desenvolver na principal suja, não apagar trabalho local e não fazer force push.
3. Registrar SHA base e executar regressão no worktree. Para bugs, demonstrar RED antes de alteração; comparar falhas com baseline em worktree separado. Preservar logs fora do repositório.
4. Alteração mínima, auditoria de segredos e `git diff --check`; revisão independente real. Não tratar autorrevisão como aprovação independente.
5. Commit coeso, push ao fork, PR inicialmente draft com testes reais, baseline, limitações e checklist de dispositivo. Não misturar falha preexistente com regressão nova.
6. Build a partir de worktree limpo do SHA final. Guardar APK e metadata externamente com commit completo, SHA256 do APK, pacote, versionCode, ABI e SHA256 do certificado. Nunca versionar chave, local.properties, APK, cache, logs grandes ou credenciais.
7. Teste no dispositivo é etapa separada de CI, Robolectric e compilação: atualização sem desinstalar, grupos/destinos/pesquisa, espera de 5 segundos/abrir agora/cancelar/Home, contador sessão/dia, retorno após 5 minutos, tela desligada, reinício, toque/arraste, fonte ampliada e TalkBack. Relatar resultado observado antes de retirar draft.

## Reprodução local

Use Java 17 e Android SDK compatível com compileSdk 35; configurar `JAVA_HOME`, `ANDROID_HOME` e `ANDROID_SDK_ROOT` fora do git. O wrapper usa Gradle 8.13. O certificado debug local precisa ser o mesmo da instalação anterior para atualização; não distribuir a chave.

```sh
python3 tools/validate_ptbr_resources.py
./gradlew testFullDebugUnitTest -PcurboxTestArm64=true --no-daemon --max-workers=2
./gradlew assembleFullDebug -PcurboxTestArm64=true --no-daemon --max-workers=2
```

A suíte completa pode falhar no teste baseline mencionado; executar build separadamente para não ocultar o resultado. Debug mantém `neth.iecal.curbox.redirect.debug`, versionCode 11 e ARM64 quando a propriedade é usada; release permanece com identidade e versão upstream. APK debug não é publicação de produção.

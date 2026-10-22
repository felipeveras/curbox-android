# Relatório de localização PT-BR e APK de teste

## Escopo

- Traduzi e completei `app/src/main/res/values-pt-rBR/strings.xml`, preservando as traduções existentes. O locale cobre recursos do app, incluindo onboarding, foco, bloqueios, configurações, alertas, backup, sincronização, NFC/QR e o fluxo de redirecionamento.
- Externalizei strings visíveis que ainda estavam literais no Kotlin: prompts QR e de pareamento, erro de nome de grupo, compartilhamento de logs, ajuda contextual, categorias de apps, dias da semana, rótulos de uso, controles NFC/QR e nomes dos scripts modelo do ocultador de interface.
- Mantive nomes de produtos e apps, URLs, identificadores de script/pacote, formatos, protocolos e conteúdo dos scripts. A configuração de `pt-BR` já fazia parte do seletor de idioma. Ela continua disponível e a seleção automática reconhece dispositivos configurados em português pelo código de idioma `pt`, incluindo preferências sem região explícita.
- O `versionCode` 7 é aplicado somente às variantes debug pelo Android Components API. Release permanece no `versionCode` 6. O pacote pessoal `neth.iecal.curbox.redirect.debug` e a assinatura de debug foram mantidos.

## Cobertura calculada

`tools/validate_ptbr_resources.py` compara o locale com os recursos padrão, verifica XML, duplicatas, tipos, placeholders, plurais, quantidade/ordem de arrays, presença das strings de redirect e inclusão de `pt-BR` no seletor.

- **1.090/1.090 recursos (100%)** em português do Brasil.
- **1.079 strings**, **8 plurais** e **3 arrays**, com as mesmas quantidades e ordem dos recursos padrão.
- **124 placeholders** conferidos; nenhuma divergência.
- Recursos de redirect presentes: ajuda, estado desligado, seleção de app, rótulo de destino, botão de abertura e mensagem de indisponibilidade.

Validação executada: `./tools/validate_ptbr_resources.py` — aprovada.

## Testes e build

Toolchain usada: Java em `<LOCAL>/builds/streak-toolchain/java`, Android SDK em `<LOCAL>/builds/streak-toolchain/android`, Gradle Wrapper 8.13, `--no-daemon --max-workers=2`.

- Testes direcionados: `testFullDebugUnitTest --tests '*AppRedirect*' --tests '*WarningRedirectTest' --tests '*RedirectSettingsTest' -PcurboxTestArm64=true` — **12 testes em 5 classes, 0 falhas**.
- Suíte completa: **177 testes; 176 passaram e 1 falhou**: `ScriptLanguageTest.matchesRegexSupportsCommonFlags`, em `ScriptLanguageTest.kt:224`. Esse teste já consta como falha upstream no registro de baseline em `REDIRECT_FEATURE.md`; naquele checkout limpo a falha ocorreu com exceção diferente. Nenhum código da linguagem de scripts foi alterado. Portanto, a suíte completa não está totalmente verde.
- Build: `assembleFullDebug -PcurboxTestArm64=true --no-daemon --max-workers=2` — `BUILD SUCCESSFUL`.
- Avisos upstream observados: KSP 1.9.25 com Kotlin 1.9.24 e o recurso `ascii_chillguy` sem valor padrão, removido pelo AAPT. Não impediram os testes direcionados nem o APK.

## APK verificado

- Arquivo final: `<LOCAL>/builds/curbox-redirect-ptbr.apk`
- SHA-256: `659a11fef53e2534b6610a97471eaad34ea3897382264e22745365e2f1a6b829`
- Tamanho: **16.525.156 bytes**
- Pacote: `neth.iecal.curbox.redirect.debug`
- Rótulo: `Curbox Redirect Test`
- Versão: `6-full-redirect-debug`, `versionCode=7`
- ABI nativa presente: `arm64-v8a`
- Assinatura: verificada com `apksigner`; SHA-256 do certificado `ae500a99a0e477a808098fa1ddddbbeb6d96ff73e28bb031754c825c8fb77c98` (um signatário, DN `C=US, O=Android, CN=Android Debug`).

## Texto fora do controle dos recursos do app

As telas de permissões e administração do Android, a interface do seletor/compartilhamento do sistema, nomes de apps instalados no picker e conteúdo de apps/sites externos são fornecidos pelo sistema ou por terceiros e seguem o idioma/conteúdo deles. Documentação aberta em páginas externas também não é localizada por este APK. Código, comentários e valores operacionais dos scripts, protocolos, URLs e identificadores foram preservados conforme solicitado. Não houve validação em dispositivo físico ou emulador.

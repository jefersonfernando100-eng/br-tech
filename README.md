# Br Tech (Android)

Kotlin + Jetpack Compose • minSdk 26 (Android 8+) • sem bibliotecas pesadas.

## Gerar o APK

**Opção A – Android Studio:** File > Open > pasta BrTech > aguarde o sync >
Build > Build APK(s). O APK fica em `app/build/outputs/apk/`.

**Opção B – GitHub (sem instalar nada):** crie um repositório, envie esta pasta,
vá em Actions > "Build APK" > Run workflow. Baixe o APK em "Artifacts" (BrTech-apk).

Obs.: o release usa a chave de debug para poder ser instalado direto.
Para publicar na Play Store, configure sua própria chave de assinatura.

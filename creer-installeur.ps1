$ErrorActionPreference = "Stop"

./mvnw clean package -DskipTests
Copy-Item target/CarnetEntretien-1.0-SNAPSHOT.jar target/installer/

jpackage `
  --type exe `
  --name "Carnet Entretien" `
  --description "Suivi de l'entretien de vos vehicules" `
  --vendor "PaulMUNOZ14" `
  --app-version 1.0.0 `
  --input target/installer `
  --main-jar CarnetEntretien-1.0-SNAPSHOT.jar `
  --main-class com.carnetentretien.carnetentretien.Launcher `
    --runtime-image "$env:JAVA_HOME" `
  --java-options "--enable-native-access=ALL-UNNAMED" `
  --dest target/dist `
  --win-menu --win-shortcut --win-dir-chooser --win-per-user-install
if ($LASTEXITCODE -ne 0) { throw "jpackage a échoué (code $LASTEXITCODE)" }
Write-Host "Installeur créé dans target\dist" -ForegroundColor Green
  # --icon installer/icone.ico (ajouter quand il y aura une icone)
  
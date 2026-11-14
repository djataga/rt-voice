# Release signing

The release APK is signed with a local keystore that is **never committed**
(`.gitignore` covers `*.jks`, `*.keystore` and `keystore.properties`).

## Files (local only)

| File | Role |
|---|---|
| `rtvoice-release.jks` | The signing key. **Back this up** — losing it means future updates require uninstalling the app (local data such as bookmarks is lost). |
| `keystore.properties` | Credentials consumed by `app/build.gradle.kts`. |

## Regenerating a lost environment

1. Recreate the keystore:
   ```
   keytool -genkeypair -v -keystore rtvoice-release.jks -alias rtvoice \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -storepass <PASSWORD> -keypass <PASSWORD> \
     -dname "CN=RT Voice, O=Djataga, C=FR"
   ```
   ⚠️ A regenerated key is a *different* identity: installed builds signed with
   the old key must be uninstalled before installing the new signature.
2. Recreate `keystore.properties` at the repository root:
   ```
   storeFile=rtvoice-release.jks
   storePassword=<PASSWORD>
   keyAlias=rtvoice
   keyPassword=<PASSWORD>
   ```

## Building a release APK

```
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

If `keystore.properties` is absent, the release build stays unsigned
(`signingConfigs` is left empty) and the APK must be signed manually.

---
name: echo-release
description: Cut an ECHO release — version bump, gate, build both APKs, read the APKs and signing certificate, tag, publish on GitHub and check the upload. Use when the owner asks for a release.
---

# Releasing

Only when the owner asks. Run every check; read results from the tools, not from memory.

1. Run the gate (see `CLAUDE.md`). Stop on any failure.
2. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`. Commit alone as
   `chore(release): <version>`.
3. Build: `./gradlew assembleRelease assembleDebug`. The APKs are copied to
   `dist/ECHO-<version>.apk` and `debug/ECHO-<version>-debug.apk`.
4. Read the APKs, not their names:

   ```sh
   B=~/Android/Sdk/build-tools/36.0.0
   $B/aapt2 dump badging dist/ECHO-<v>.apk | grep -oE "versionCode='[0-9]+' versionName='[^']+'"
   $B/apksigner verify --print-certs dist/ECHO-<v>.apk | grep -i 'SHA-256 digest'
   ```

   The release certificate must be
   `fa019c95e69b5adee8097e61f22323a606b50e0104a1cca0d25f154d77021d40`. Any other certificate
   cannot upgrade an existing install. Stop and tell the owner.
5. Push `main`, then tag and push: `git tag -a v<v> -m "ECHO <v>"` and `git push origin v<v>`.
6. Publish:
   `gh release create v<v> dist/ECHO-<v>.apk debug/ECHO-<v>-debug.apk --verify-tag --latest --notes-file <notes>`.
   Release notes are the changelog (`CHANGELOG.md` points to the releases page). Write them for
   users: what changed, grouped by area, plain sentences.
7. Check: `gh api repos/Sonophage/echo-launcher/releases/latest --jq .tag_name`
   names the new tag, and the uploaded asset sizes match `stat -c %s` of the local files.

#!/usr/bin/env bash
# Downloads the Discord Social SDK aar from this repository's Git LFS store into
# discord/discord-native/libs/. The aar is proprietary and is not committed.
# Needs: gh (signed in, or GH_TOKEN set), curl, python3, sha256sum.
set -euo pipefail

OID=e2b84847311923a7cf5ee61ea4a7911b0bb89fec5454c297ac85024fefeaee1c
SIZE=29641460
REPO=Sonophage/platform-selection-portal-launcher
ROOT=$(cd "$(dirname "$0")/.." && pwd)
DEST="$ROOT/discord/discord-native/libs/discord_partner_sdk.aar"

if [ -f "$DEST" ] && echo "$OID  $DEST" | sha256sum -c --status; then
    echo "already present: $DEST"
    exit 0
fi

TOKEN=${GH_TOKEN:-$(gh auth token)}
HREF=$(curl -fsS "https://github.com/$REPO.git/info/lfs/objects/batch" \
    -H "Accept: application/vnd.git-lfs+json" \
    -H "Content-Type: application/vnd.git-lfs+json" \
    -u "x-access-token:$TOKEN" \
    -d "{\"operation\":\"download\",\"transfers\":[\"basic\"],\"objects\":[{\"oid\":\"$OID\",\"size\":$SIZE}]}" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["objects"][0]["actions"]["download"]["href"])')

TMP="$DEST.part"
curl -fsSL -o "$TMP" "$HREF"
echo "$OID  $TMP" | sha256sum -c --status || { rm -f "$TMP"; echo "sha256 mismatch" >&2; exit 1; }
mv "$TMP" "$DEST"
echo "fetched: $DEST"

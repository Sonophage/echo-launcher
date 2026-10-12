#!/usr/bin/env bash
# Check ECHO on the owner's AYN Thor (two displays) over adb.
#
#   tools/thor.sh install          build the debug app and install it
#   tools/thor.sh key A DPAD_DOWN  press controller keys (KEYCODE_ is added), 0.6 s apart
#   tools/thor.sh shot NAME        capture both displays, half size: build/thor/NAME-d0.png, NAME-d<second>.png
#   tools/thor.sh ime              who the keyboard answers to on each display, and whether it is shown
#
# The Thor is found by its model, or set THOR=<serial>. Display 0 is the first (upper) display; the
# second display's id is read from the device, as is each display's SurfaceFlinger id that screencap needs.
set -euo pipefail
cd "$(dirname "$0")/.."

serial=${THOR:-}
if [ -z "$serial" ]; then
    for s in $(adb devices | awk 'NR>1 && $2=="device" {print $1}'); do
        [ "$(adb -s "$s" shell getprop ro.product.model | tr -d '\r')" = "AYN Thor" ] && serial=$s && break
    done
fi
[ -n "$serial" ] || { echo "thor: no AYN Thor on adb (adb devices)" >&2; exit 1; }
a() { adb -s "$serial" "$@"; }

# the second display's Android id, and each display's SurfaceFlinger id from its uniqueId "local:<id>"
displays() {
    a shell dumpsys display | grep -oE 'mDisplayId=[0-9]+|uniqueId "local:[0-9]+"' | tr -d '"' \
        | awk -F'[=:]' '/^mDisplayId/ {id=$2} /^uniqueId/ && id!="" {print id, $2; id=""}' | sort -u
}

case "${1:-}" in
install)
    ./gradlew :app:assembleDebug -q
    a install -r app/build/outputs/apk/debug/app-debug.apk
    ;;
key)
    shift
    for k in "$@"; do a shell input gamepad keyevent "KEYCODE_${k#KEYCODE_}"; sleep 0.6; done
    ;;
shot)
    name=${2:?thor: shot needs a name}
    mkdir -p build/thor
    while read -r id sf; do
        out=build/thor/$name-d$id.png
        a exec-out screencap -p -d "$sf" > "$out"
        command -v magick >/dev/null && magick "$out" -resize 50% "$out"
        echo "$out"
    done < <(displays)
    ;;
ime)
    a shell dumpsys window | grep -oE 'ime(Input|Control|Layering)Target in display# [0-9]+ Window\{[^}]*\}' | sed -E 's/Window\{[0-9a-f]+ u0 //; s/\}$//'
    a shell dumpsys input_method | grep -oE 'mInputShown=[a-z]+|mShowRequested=[a-z]+' | sort -u
    a shell dumpsys input | grep -m1 -oE 'FocusedDisplayId: [0-9]+'
    ;;
*)
    sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac

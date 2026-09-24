#!/usr/bin/env sh
set -eu
PORT_TO_USE="${PORT:-8080}"
echo "My Chhachh APK is ready"
echo "Serving on port ${PORT_TO_USE}"
exec python3 -m http.server "${PORT_TO_USE}" --directory /public

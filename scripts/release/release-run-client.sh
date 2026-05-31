#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

export LANG="${LANG:-C.UTF-8}"
export LC_ALL="${LC_ALL:-C.UTF-8}"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
export AUCTION_SERVER_HOST="${1:-127.0.0.1}"
export AUCTION_SERVER_PORT="${2:-8080}"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    chcp.com 65001 >/dev/null
    ;;
esac

shopt -s nullglob
jars=("$SCRIPT_DIR"/auction-*-client.jar)

if (( ${#jars[@]} == 0 )); then
  echo "Không tìm thấy file auction-*-client.jar trong $SCRIPT_DIR" >&2
  exit 1
fi

java -jar "${jars[0]}"

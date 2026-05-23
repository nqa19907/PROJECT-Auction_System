#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DATA_DIR="$SCRIPT_DIR/data"

usage() {
    echo "Usage: ./clear-data.sh [--keep-users]"
    echo
    echo "Deletes serialized auction data in: $DATA_DIR"
    echo "  default      delete all .ser and .bak files"
    echo "  --keep-users delete auction/item/bid data, keep users.ser"
}

if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    usage
    exit 0
fi

if [ ! -d "$DATA_DIR" ]; then
    echo "Data directory does not exist: $DATA_DIR"
    exit 0
fi

if [ "${1:-}" = "--keep-users" ]; then
    rm -f \
        "$DATA_DIR/auctions.ser" \
        "$DATA_DIR/items.ser" \
        "$DATA_DIR/bid_transactions.ser" \
        "$DATA_DIR/auctions.ser".*.bak \
        "$DATA_DIR/items.ser".*.bak \
        "$DATA_DIR/bid_transactions.ser".*.bak
    echo "Deleted auction data. Kept users.ser."
elif [ "$#" -eq 0 ]; then
    rm -f "$DATA_DIR"/*.ser "$DATA_DIR"/*.bak
    echo "Deleted all serialized data in: $DATA_DIR"
else
    usage
    exit 1
fi

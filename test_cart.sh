#!/bin/bash

BASE_URL="http://localhost:8081"
USER_ID="user123"

for i in {1..7}; do
  ITEM_ID="item_$i"
  ITEM_JSON="{\"itemId\":\"$ITEM_ID\", \"quantity\":1, \"unitPrice\":19.99}"

  echo "🛒 Requête POST n°$i (itemId=$ITEM_ID)"
  curl -i -X POST "$BASE_URL/carts/$USER_ID/items" \
    -H "Content-Type: application/json" \
    -d "$ITEM_JSON"
  echo -e "\n---"
done

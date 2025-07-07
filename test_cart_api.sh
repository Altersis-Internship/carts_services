#!/bin/bash

BASE_URL="http://localhost:8081"
USER_ID="user123"
ITEM_ID="abc123"
ITEM_JSON="{\"itemId\":\"$ITEM_ID\", \"quantity\":1, \"unitPrice\":19.99}"

echo "1. Voir le panier (doit être vide au départ)"
curl -s -X GET "$BASE_URL/carts/$USER_ID"
echo -e "\n---"

echo "2. Ajouter un article"
curl -s -X POST "$BASE_URL/carts/$USER_ID/items" \
  -H "Content-Type: application/json" \
  -d "$ITEM_JSON"
echo -e "\n---"

echo "3. Ajouter le même article (incrémente)"
curl -s -X POST "$BASE_URL/carts/$USER_ID/items" \
  -H "Content-Type: application/json" \
  -d "$ITEM_JSON"
echo -e "\n---"

echo "4. Voir les articles du panier"
curl -s -X GET "$BASE_URL/carts/$USER_ID/items"
echo -e "\n---"

echo "5. Mettre à jour la quantité à 5"
curl -s -X PATCH "$BASE_URL/carts/$USER_ID/items" \
  -H "Content-Type: application/json" \
  -d "{\"itemId\":\"$ITEM_ID\", \"quantity\":5, \"unitPrice\":19.99}"
echo -e "\n---"

echo "6. Supprimer l'article"
curl -s -X DELETE "$BASE_URL/carts/$USER_ID/items/$ITEM_ID"
echo -e "\n---"

echo "7. Supprimer le panier entier"
curl -s -X DELETE "$BASE_URL/carts/$USER_ID"
echo -e "\n---"

#!/bin/bash

echo "Generando infinitos requests..."

while true; do 
  curl https://$GATEWAY_URL --insecure
  sleep .5
done

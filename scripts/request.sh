#!/bin/bash

cantidadDeRequests=$1

if [ $# -eq 0 ]
then
	let "cantidadDeRequests = 10"
	echo "Ejemplo de uso:"
	echo "    request.sh [CANTIDAD]"
	echo "Por defecto: request.sh 10"
	echo " "
fi
	let "i = 0"
	echo "Realizando $cantidadDeRequests Requests..."
	while [ $i -lt $cantidadDeRequests ]; do
		curl https://$GATEWAY_URL --insecure
		sleep .1
		let "i=$((i + 1))"
	done


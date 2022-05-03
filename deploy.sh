#!/bin/bash

set -e

log() {
  echo
  echo "##### $*"
}

if [[ -z ${PROJECT} ]]; then
  log "La variable de entorno PROJECT no está definida"
  exit
else
  log "Usando PROJECT con valor: ${PROJECT}"
fi

if [[ -z ${GATEWAY_URL} ]]; then
  log "La variable de entorno GATEWAY_URL no está definida"
  exit
else
  log "Usando GATEWAY_URL con valor: ${GATEWAY_URL}"
fi

#log "Login..."
#oc login -u user1 -p r3dh4t1!

#log "Creando proyecto..."
#oc new-project $PROJECT || true

log "Desplegando microservicios..."

log "Cuenta v1"
oc create -f cuenta/kubernetes/cuenta-service-template.yml -n $PROJECT
oc create -f cuenta/kubernetes/Service.yml -n $PROJECT

log "Producto v1"
oc create -f producto/kubernetes/producto-service-template.yml -n $PROJECT
oc create -f producto/kubernetes/Service.yml -n $PROJECT

log "Precio v1"
oc create -f precio/kubernetes/precio-service-template.yml -n $PROJECT
oc create -f precio/kubernetes/Service.yml -n $PROJECT

#log "Creando Gateway y VirtualService de Cuenta"
#oc create -f istio/Gateway-VirtualService.yaml -n $PROJECT

#log "Probando flujo de microservicios..."
#./scripts/request.sh 3

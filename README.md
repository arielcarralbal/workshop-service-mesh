# Workshop OpenShift Service Mesh

En este Workshop usaremos tres microservicios que interactúan y se comunican entre sí para demostrar las capacidades de OpenShift Service Mesh.

- Cuenta: Es el punto de entrada al flujo de microservicios.
- Producto: Es llamado por *Cuenta* y su función ficticia sería obtener una serie de precios.
- Precio: Es llamado por *Producto* y su función ficticia sería devolver una serie de precios. Este microservicio posee varios endpoints que simulan diferentes comportamientos.

El flujo de comunicación entre los servicios es:

					Cuenta -> Producto -> Precio

## Instalación

### 1. Prerequisitos

Debemos tener instalados los siguientes operadores en OpenShift:

- OpenShift Elasticsearch
- Jaeger
- Kiali
- Red Hat OpenShift Service Mesh

Debemos contar con los siguientes comandos:

- git ([Descargar aquí](https://git-scm.com/downloads "Descargar git"))
- oc ([Descargar aquí](https://access.redhat.com/downloads/content/290/ver=4.10/rhel---8/4.10.10/x86_64/product-software "Descargar oc"))
- siege ([Windows](https://github.com/ewwink/siege-windows "Descargar siege para Windows") - [Mac con Homebrew](https://formulae.brew.sh/formula/siege "Descargar siege para Mac"))



### 2. Deploy de microservicios

Ejecutamos los siguientes comandos. 

IMPORTANTE: En el primero reemplazamos la "N" con el grupo asignado.
```sh
echo "export PROJECT=workshop-mesh-apps-N" >> $HOME/.bashrc
source $HOME/.bashrc
mkdir ~/workshop-mesh && cd "$_"
git clone https://github.com/arielcarralbal/workshop-service-mesh
cd workshop-service-mesh
```
Luego de clonar este repositorio, iniciamos sesión en OpenShift.

IMPORTANTE: Reemplazamos la "N" con el número de grupo asignado.
```sh
oc login -u userN -p r3dh4t1!
```

Verificamos los valores de las siguientes variables de entorno antes de continuar:
- *PROJECT* con el nombre del proyecto (namespace). 

```sh
echo $PROJECT
```
Debemos ver *workshop-mesh-apps-N* ("N" es el número de grupo asignado).

Damos permiso de ejecución al script deploy y lo ejecutamos.
```sh
chmod +x deploy.sh
./deploy.sh
```

Creamos una nueva variable de entorno:
- *GATEWAY_URL* con la ruta expuesta (Ej: workshop-mesh-apps-N.apps.kali.rlab.sh).

En el primero reemplazamos la "N" con el grupo asignado.
```sh
echo "export GATEWAY_URL=workshop-mesh-apps-N.apps.kali.rlab.sh" >> $HOME/.bashrc
source $HOME/.bashrc
echo $GATEWAY_URL
```
Editamos el archivo 
**istio/Gateway-VirtualService.yaml** y en *host* reemplazamos "N" con el grupo asignado.
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: gateway-workshop
spec:
  selector:
    istio: ingressgateway # use istio default controller
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: cliente-certs
    hosts:
    - 'workshop-mesh-apps-N.apps.kali.rlab.sh'
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: gateway-workshop
spec:
  hosts:
  - 'workshop-mesh-apps-N.apps.kali.rlab.sh'
  gateways:
  - gateway-workshop
  http:
  - match:
    - uri:
        exact: /
    route:
    - destination:
        host: cuenta
        port:
          number: 8080

```

Por último creamos el Gateway y VirtualService con el yaml recién editado:
```sh
oc create -f istio/Gateway-VirtualService.yaml -n $PROJECT
```
Con la creación del Gateway, OpenShift crea la ruta (Route) automáticamente.
Veamos en cómo quedó todo.

# Ejercicios
## 1. Control de tráfico
### 1.1 Rutear a versión específica

Desplegamos la v2 del servicio Precio.

```sh
oc create -f precio-v2/kubernetes/precio-service-template.yml -n $PROJECT
```

Abrimos una nueva ventana de terminal para seguir los comportamientos (primero damos permisos de ejecución al scripts):
```sh
chmod +x ./scripts/request-infinito.sh
./scripts/request-infinito.sh
```

Volvemos a la ventana terminal anterior, creamos un DestinationRule y un VirtualService para redirecionar todo el tráfico a la v2.

```sh
oc create -n $PROJECT -f istio/destination-rule/destination-rule-precio-v1-v2.yml
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-v2.yml
```

#### Canary release
Mediante VirtualService vamos a redirecionar el 90% del tráfico a la v1 y sólo el 10% a la v2.

```sh
oc replace -n $PROJECT -f istio/virtual-service/virtual-service-precio-v1_and_v2_90_10.yml
```
¡Probemos otras opciones de balanceo!

## Resiliencia de servicios
### Balanceo de carga
Istio brinda 3 algoritmos configurables: ROUND_ROBIN, RANDOM, LEAST_CONN. Eliminamos el VS y el DR precio. Luego escalamos v2 a 3 réplicas.
```sh
oc delete virtualservice precio -n $PROJECT
oc delete destinationrule precio -n $PROJECT
oc scale deployment precio-v2 --replicas=3 -n $PROJECT
```
Luego de observar el comportamiento actual, creamos el siguiente DR.
```sh
oc create -n $PROJECT -f istio/destination-rule/destination-rule-precio_lb_policy_app.yml
```
Editemos el yaml y probemos otro balanceo.
Por último, eliminamos el DR y volvemos a 1 réplica.
```sh
oc delete -n $PROJECT -f istio/destination-rule/destination-rule-precio_lb_policy_app.yml
oc scale deployment precio-v2 --replicas=1 -n $PROJECT
```
### Timeout
Agregaremos a la v2 una demora de 3 segundos al responder.
Ingresamos dentro dentro del contenedor del pod
```sh
oc exec -it $(oc get pods | grep precio-v2 | awk '{ print $1 }' | head -1) -c precio /bin/bash
```
El endpoint *slow* hará que demore 3 segundos.

```sh
curl localhost:8080/slow
exit
```
Reemplazamos el VirtualService por uno con tolerancia de un segundo.
```sh
oc create -f istio/virtual-service/virtual-service-precio-timeout.yml  -n $PROJECT
```
Eliminamos el VS y quitamos la demora de 3 segundos llamando al enpoint *fast*.
```sh
oc delete virtualservice precio -n $PROJECT
```
```sh
oc exec -it $(oc get pods | grep precio-v2 | awk '{ print $1 }' | head -1) -c precio /bin/bash
curl localhost:8080/fast
exit
```

### Circuit breaker
Agregamos una demora de 3 segundos en la respueta a la v2 desde el endpoint *slow*
```sh
oc exec -it $(oc get pods | grep precio-v2 | awk '{ print $1 }' | head -1) -c precio /bin/bash
curl localhost:8080/slow
exit
```
Agredamos los siguientes DR y VS.
```sh
oc create -n $PROJECT -f istio/destination-rule/destination-rule-precio-v1-v2.yml
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-v1_and_v2_50_50.yml
```
Generemos carga: 20 clientes enviando cada 2 requests concurrentes cada uno.
```sh
siege -r 2 -c 20 -v https://$GATEWAY_URL
```
Recordemos que el VirtualService actual divide 50% a v1 y 50% a v2. Ahora, creamos la política del cb con el DestinationRule que afecta sólo al 50% del tráfico (v2), limitando el número de conexiones y el número de solicitudes pendientes a 1.
```sh
oc -n $PROJECT replace -f istio/destination-rule/destination-rule-precio_cb_policy_version_v2.yml
```
Volvemos a generar carga
```sh
siege -r 2 -c 20 -v https://$GATEWAY_URL
```
Devolvemos a la versión v2 los tiempos de respuesta normales desde el endpoint *fast*, eliminamos el VS y el DR.
```sh
oc exec -it $(oc get pods | grep precio-v2 | awk '{ print $1 }' | head -1) -c precio /bin/bash
curl localhost:8080/fast
exit
oc delete virtualservice precio -n $PROJECT
oc delete destinationrule precio -n $PROJECT
```


## Testing de Caos
### HTTP Errors
Ahora vamos a generar errores desde Istio y no desde el servicio.

Creamos un VS que genera error 503 el 50% de las veces.
```sh
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-503.yml
```
Eliminamos el vs.
```sh
oc delete virtualservice precio -n $PROJECT
```
### Demoras

El siguiente VS inyecta 7 segundos de demora en el 50% de las respuestas del servicio.
```sh
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-delay.yml
```
Eliminamos el vs.
```sh
oc delete virtualservice precio -n $PROJECT
```

## Observabilidad
Ingresamos dentro dentro del contenedor del pod
```sh
oc exec -it $(oc get pods | grep precio-v2 | awk '{ print $1 }' | head -1) -c precio /bin/bash
```
El endpoint *misbehave* hará que nuestra aplicación devuelva solo errores 503.

```sh
curl localhost:8080/misbehave
exit
```
Abramos Kiali y veamos qué sucede.
Aplicamos el siguiente VS. Esta regla establece sus reintentos en 3 y utilizará un tiempo de espera de 2 segundos para cada reintento. Por lo tanto, el tiempo de espera acumulativo es de 6 segundos más el tiempo de la llamada original.
```sh
oc create -n $PROJECT -f istio/destination-rule/destination-rule-precio-v1-v2.yml
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-v2_retry.yml
```
Eliminamos el VS y el DR.
```sh
oc delete virtualservice precio -n $PROJECT
oc delete destinationrule precio -n $PROJECT
```
y devolvemos a precio-v2 el comportamiento correcto.
```sh
oc exec -it $(oc get pods | grep precio-v2 | awk '{ print $1 }' | head -1) -c precio /bin/bash
curl localhost:8080/behave
exit
```
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

Luego de clonar este repositorio, iniciamos sesión en OpenShift.
Creamos dos variables de entorno:
- *PROJECT* con el nombre del proyecto (namespace) que vayamos a crear.
- *GATEWAY_URL* con la ruta expuesta (Ej: workshop-a.ejemplo.com).
Verificamos que sus valores antes de continuar.

```sh
echo $PROJECT
echo $GATEWAY_URL
```

Creamos el proyecto en OpenShift (este proyecto se debe dar de alta en la Service Mesh antes o después). 

```sh
oc new-project $PROJECT
```

Damos permiso de ejecución al script deploy.
```sh
chmod +x deploy.sh
```
Ejecutamos el script.

```sh
./deploy.sh
```

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

####Canary release
Mediante VirtualService vamos a redirecionar el 90% del tráfico a la v1 y sólo el 10% a la v2.

```sh
oc replace -n $PROJECT -f istio/virtual-service/virtual-service-precio-v1_and_v2_90_10.yml
```

## Resiliencia de servicios
####Balanceo de carga
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
Por último, eliminamos el DR y volvemos a 1 réplica.
```sh
oc delete -n $PROJECT -f istio/destination-rule/destination-rule-precio_lb_policy_app.yml
oc scale deployment precio-v2 --replicas=1 -n $PROJECT
```
####Timeout
Reemplazamos la v2 por una que demora 3 segundos en responder. Editamos la imagen a la versión 2.1.0

Reemplazamos el VirtualService por uno con tolerancia de un segundo.
```sh
oc create -f istio/virtual-service/virtual-service-precio-timeout.yml  -n $PROJECT
```
Volvemos a la versión v2 original: 2.0.1 el el yaml del Deployment, y eliminamos el VS
```sh
oc delete virtualservice precio -n $PROJECT
```
####Reintentos
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
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-v2_retry.yml
```
Eliminamos el VS y devolvemos a precio-v2 el comportamiento correcto.
```sh
oc delete virtualservice precio -n $PROJECT
oc delete pod -l app=precio,version=v2 -n $PROJECT
```
####Circuit breaker
Reemplazamos la v2 por una que demora 3 segundos en responder. Editamos la imagen a la versión 2.1.0
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
Volvemos a la versión v2 original: 2.0.1 el el yaml del Deployment, eliminamos el VS y el DR.
```sh
oc delete virtualservice precio -n $PROJECT
oc delete destinationrule precio -n $PROJECT
```
####Pool Ejection

Creamos el DR, VS y escalamos v2 a 2 réplicas. 
```sh
oc create -n $PROJECT -f istio/destination-rule/destination-rule-precio-v1-v2.yml
oc create -n $PROJECT -f istio/virtual-service/virtual-service-precio-v1_and_v2_50_50.yml
oc scale deployment precio-v2 --replicas=2 -n $PROJECT
```
Entramos a la terminal de uno de los pods de v2 y llamamos al endpoint /misbehave para que genere mal comportamiento.

Con esta DestinationRule, Istio verifica cada 5 segundos los pods que se comportan mal y elimina esos pods del grupo de balanceo de carga después de un error consecutivo y lo mantiene fuera durante 15 segundos.
```sh
oc -n $PROJECT replace -f istio/destination-rule/destination-rule-precio_cb_policy_pool_ejection.yml
```

## Testing de Caos
`Pendiente`

## Observabilidad
`Pendiente`

## Seguridad
`Pendiente`


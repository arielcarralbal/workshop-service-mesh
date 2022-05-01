# Workshop OpenShift Service Mesh

En este Workshop usaremos tres microservicios que interactúan y se comunican entre sí para demostrar las capacidades de OpenShift Service Mesh. Son servicios sencillos:

- Cuenta: Es el punto de entrada al sistema.
- Producto: Es llamado por “Cuenta” y su función ficticia sería obtener una serie de precios.
- Precio: Es llamado por “Producto” y su función ficticia sería devolver una serie de precios.

El flujo de comunicación entre los servicios:

					Cuenta -> Producto -> Precio 

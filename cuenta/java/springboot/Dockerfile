FROM fabric8/java-jboss-openjdk8-jdk:1.3.1
ENV JAVA_APP_DIR=/deployments
ENV JAEGER_SERVICE_NAME=cuenta\
  JAEGER_ENDPOINT=http://jaeger-collector.istio-system.svc:14268/api/traces\
  JAEGER_PROPAGATION=b3\
  JAEGER_SAMPLER_TYPE=const\
  JAEGER_SAMPLER_PARAM=1
EXPOSE 8080 8778 9779
COPY target/cuenta-service-1.0.0.jar /deployments/

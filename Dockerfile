FROM centos:latest
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-Linux-* &&\
    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-Linux-*
#docker
RUN yum upgrade -y
USER root
RUN mkdir -p /home/app
ENV JAVA_APP_DIR=/deployments \
    JAVA_MAJOR_VERSION=11
RUN yum install -y \
       java-11-openjdk-devel \
    && echo "securerandom.source=file:/dev/urandom" >> /usr/lib/jvm/jre/lib/security/java.security \
    && yum clean all

ENV JAVA_HOME /etc/alternatives/jre
# Maven Build
FROM maven:3.6.0-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package
FROM openjdk:11-jre-slim
COPY --from=build /home/app/target/*.jar /usr/src/java-app/app.jar
WORKDIR /usr/src/java-app
COPY run.sh /usr/src/java-app/run.sh
RUN chmod +x run.sh
COPY standard_trusts.jks /usr/src/java-app/standard_trusts.jks
COPY ca_ctc_nonprod.crt /usr/src/java-app/.kube/ca_ctc_nonprod.crt
COPY elr-stage-kubernetes.crt /usr/src/java-app/.kube/elr-stage-kubernetes.crt
COPY elr-prod-kubernetes.crt /usr/src/java-app/.kube/elr-prod-kubernetes.crt
RUN chown -R 1001:0 /usr/src/java-app

EXPOSE 8095
USER 1001
CMD ["./run.sh"]

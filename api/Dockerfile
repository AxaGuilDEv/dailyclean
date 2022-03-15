
FROM node:lts-buster-slim AS web

ENV APP_ROOT /app_root

WORKDIR ${APP_ROOT}
COPY --chown=${USER} ./web .

RUN npm ci 
RUN npm test --  --runInBand --coverage --watchAll=false
RUN npm run build 
    
FROM registry.access.redhat.com/ubi8/ubi:8.5-200 AS build

ENV APP_ROOT /app_root
ENV HOME /home

WORKDIR ${APP_ROOT}
USER root

ENV MAVEN_VERSION 3.8.4
ENV MAVEN_DOWNLOAD_SUM a9b2d825eacf2e771ed5d6b0e01398589ac1bfa4171f36154d1b5787879605507802f699da6f7cfc80732a5282fd31b28e4cd6052338cbef0fa1358b48a5e3c8
ENV MAVEN_DIR=/opt/maven

RUN INSTALL_PKGS="java-11-openjdk java-11-openjdk-devel" && \
  dnf -y --setopt=tsflags=nodocs install $INSTALL_PKGS && \
  rpm -V $INSTALL_PKGS && \
  dnf -y clean all --enablerepo='*'

WORKDIR ${HOME}

RUN curl -fsSL https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -O && \
    echo "$MAVEN_DOWNLOAD_SUM  apache-maven-${MAVEN_VERSION}-bin.tar.gz" | sha512sum -c - && \
    tar xvfz apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    mkdir -p ${MAVEN_DIR} && \
    cp -R apache-maven-${MAVEN_VERSION}/* ${MAVEN_DIR} && \
    rm ${MAVEN_DIR}/conf/settings.xml && \
    #cp /opt/configurations/settings.xml ${MAVEN_DIR}/conf/settings.xml && \
    rm -Rf apache-maven-${MAVEN_VERSION}
    

ENV PATH ${MAVEN_DIR}/bin:$PATH

RUN rm -r /var/cache/dnf && curl -fsSL https://git.centos.org/rpms/centos-repos/raw/c8/f/SOURCES/CentOS-Stream-PowerTools.repo -O && \
    mv CentOS-Stream-PowerTools.repo /etc/yum.repos.d/ && \
    curl -fsSL https://git.centos.org/rpms/centos-repos/raw/c8/f/SOURCES/RPM-GPG-KEY-centosofficial -O && \
    mv RPM-GPG-KEY-centosofficial /etc/pki/rpm-gpg/ && \
    INSTALL_PKGS="glibc-devel zlib-devel gcc" && \
    dnf -y --setopt=tsflags=nodocs install $INSTALL_PKGS && \
    rpm -V $INSTALL_PKGS && \
    echo 8-stream > /etc/yum/vars/stream && \
    dnf makecache && \
    dnf --nobest --enablerepo=powertools -y install libstdc++-static && \
    rpm -V libstdc++-static && \
    dnf -y clean all --enablerepo='*'

ENV JAVA_VERSION java11
ENV MANDREL_VERSION 21.3.0.0-Final
ENV MANDREL_DOWNLOAD_SUM 581744f6448464a8efd712ce04d4879d5c10910559d0d10dcbd56c03e06fea47

WORKDIR ${APP_ROOT}

RUN curl -fsSL https://github.com/graalvm/mandrel/releases/download/mandrel-${MANDREL_VERSION}/mandrel-${JAVA_VERSION}-linux-amd64-${MANDREL_VERSION}.tar.gz -O && \
    echo "$MANDREL_DOWNLOAD_SUM  mandrel-${JAVA_VERSION}-linux-amd64-${MANDREL_VERSION}.tar.gz" | sha256sum -c - && \
    tar xvfz mandrel-${JAVA_VERSION}-linux-amd64-${MANDREL_VERSION}.tar.gz && \
    rm mandrel-${JAVA_VERSION}-linux-amd64-${MANDREL_VERSION}.tar.gz && \
    mv mandrel-${JAVA_VERSION}-${MANDREL_VERSION} mandrel && \
    rm -Rf mandrel/demo mandrel/man 

ENV JAVA_HOME=${APP_ROOT}/mandrel
ENV GRAALVM_HOME=${APP_ROOT}/mandrel
ENV PATH=${JAVA_HOME}/bin:${PATH}

COPY --chown=${USER} ./api .
RUN rm -Rf ./src/main/resources/META-INF/resources
COPY --chown=${USER} --from=web ${APP_ROOT}/build ./src/main/resources/META-INF/resources

RUN mvn package -Pnative -B

FROM registry.access.redhat.com/ubi8-minimal:8.5-204 AS runtime

ENV APP_ROOT /app_root

WORKDIR ${APP_ROOT}
COPY --chown=${USER} --from=build ${APP_ROOT}/target/*-runner ./application

EXPOSE 8080
USER ${USER}

CMD ["./application", "-Dquarkus.http.host=0.0.0.0", "-Xms40m", "-Xmx60m", "-Xmn20m"]


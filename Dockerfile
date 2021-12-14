#FROM node:lts-buster-slim AS web
#
#WORKDIR /src
#COPY --chown=${USER} ./web .
#
#RUN npm ci 
##RUN npm test --  --runInBand --coverage --watchAll=false
#RUN npm run build 
    
FROM registry.access.redhat.com/ubi8/ubi:8.5-200 AS build

WORKDIR ${APP_ROOT}
USER root



ENV MAVEN_VERSION 3.6.3
ENV MAVEN_DOWNLOAD_SUM c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0
ENV MAVEN_DIR=/opt/maven

RUN INSTALL_PKGS="java-11-openjdk java-11-openjdk-devel" && \
  dnf -y --setopt=tsflags=nodocs install $INSTALL_PKGS && \
  rpm -V $INSTALL_PKGS && \
  dnf -y clean all --enablerepo='*'

WORKDIR ${HOME}

RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -O && \
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
ENV MANDREL_VERSION 21.0.0.0-Final
ENV MANDREL_DOWNLOAD_SUM 85c5d39997e373fa488a1e5555a1040bebb98f307968f3614a002f75ebf678ff

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
#RUN rm -Rf ./src/main/resources/META-INF/resources
#COPY --chown=${USER} --from=web ${APP_ROOT}/build ./src/main/resources/META-INF/resources

RUN mvn package -Pnative -B

#FROM registry.access.redhat.com/ubi8/python-39:1-22.1638364042 AS runtime
#
#WORKDIR ${APP_ROOT}
#COPY --chown=${USER} --from=build ${APP_ROOT}/target/*-runner ./application
#
#EXPOSE 8080
#USER ${USER}
#
#CMD ["./application", "-Dquarkus.http.host=0.0.0.0", "-Xms40m", "-Xmx60m", "-Xmn20m"]


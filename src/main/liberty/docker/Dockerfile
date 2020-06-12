FROM websphere-liberty:20.0.0.5-full-java8-ibmjava

ARG WAR_FILE

RUN  installUtility install --acceptLicense defaultServer

RUN  mkdir -p /opt/ibm/wlp/usr/shared/config/lib/global
RUN  mkdir -p /opt/ibm/wlp/usr/shared/resources   
RUN  mkdir -p /opt/ibm/wlp/output/defaultServer/resources/security
RUN  mkdir -p /opt/ibm/wlp/output/defaultServer/all_config

COPY ./src/main/liberty/run.sh         /opt/ibm/wlp/output/defaultServer/run.sh
COPY ./src/main/liberty/config/DEV/    /opt/ibm/wlp/output/defaultServer/all_config/DEV
COPY ./src/main/liberty/config/STAGE/  /opt/ibm/wlp/output/defaultServer/all_config/STAGE
COPY ./target/${WAR_FILE}              /opt/ibm/wlp/usr/servers/defaultServer/apps/authors.war

ENV WEB_PORT 9080
EXPOSE  9080

USER 0
RUN  chmod 755 /opt/ibm/wlp/output/defaultServer/run.sh

USER 1001
CMD ["/opt/ibm/wlp/output/defaultServer/run.sh"]
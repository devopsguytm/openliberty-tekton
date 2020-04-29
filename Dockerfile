FROM openliberty/open-liberty-s2i:19.0.0.12
MAINTAINER Vlad Sancira <vlad.sancira@ro.ibm.com>
ARG WAR_FILE

COPY ./target/${WAR_FILE}  /config/dropins/
RUN ls -l /config/dropins/

COPY ./src/main/liberty/config/*.xml /config/
RUN ls -l /config/

ENV WEB_PORT 9080
EXPOSE  9080

CMD ["/opt/ol/wlp/bin/server", "run", "defaultServer"]
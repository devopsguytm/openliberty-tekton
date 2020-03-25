FROM openliberty/open-liberty-s2i:19.0.0.12

COPY ./ci-cd-pipeline/kubernetes-tekton/artefacts/authors.war  /config/dropins/
RUN ls -l /config/dropins/

COPY ./ci-cd-pipeline/kubernetes-tekton/artefacts/*.xml /config/
RUN ls -l /config/

ENV WEB_PORT 9080
EXPOSE  9080

CMD ["/opt/ol/wlp/bin/server", "run", "defaultServer"]
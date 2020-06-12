#!/bin/bash
set +ex
echo "Copying ${ENVIRONMENT} configuration files..."   
   cp    /opt/ibm/wlp/output/defaultServer/all_config/${ENVIRONMENT}/config/*         /opt/ibm/wlp/usr/servers/defaultServer   
   cp    /opt/ibm/wlp/output/defaultServer/all_config/${ENVIRONMENT}/global/*         /opt/ibm/wlp/usr/shared/config/lib/global
echo "Done. Starting Defaultserver..."
server run defaultServer

#!/bin/bash
set -e

# valori di default(sovvrascrivibili via anv)
: "${DB_HOST:=localhost}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=homegym}"
: "${DB_USER:=root}"
: "${DB_PASSWORD:=password}"
: "${DB_MAX_ACTIVE:=20}"

TEMPLATE="/usr/local/tomcat/conf/Catalina/localhost/ROOT.xml.template"
TARGET="/usr/local/tomcat/conf/Catalina/localhost/ROOT.xml"

mkdir -p "$(dirname "$TARGET")"

# usa envsubst per sostituire le variabili nel template
envsubst '\$DB_HOST \$DB_PORT \$DB_NAME \$DB_USER \$DB_PASSWORD \$  DB_MAX_ACTIVE' < "$TEMPLATE" > "$TARGET"

# avvia il comando passato (di solito catalina.sh run)
exec "$@"

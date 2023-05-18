#!/bin/sh

java \
  -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
  -Djavax.net.ssl.trustStore=standard_trusts.jks \
  -Djavax.net.ssl.trustStorePassword=ipp_user \
  -jar app.jar --use-ssl true --ssl-cert ssl_cert.jks --ssl-key $SSL_KEY
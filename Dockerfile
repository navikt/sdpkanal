FROM navikt/java:11

ENV NO_NAV_SDPKANAL_DOKUMENT_PATH_PREFIX='/q1/dokumentdistribusjon/documentFileshare/'
ENV NO_NAV_SDPKANAL_KEYSTORE=/var/run/secrets/nais.io/vault/virksomhet.jks.b64
ENV NO_NAV_SDPKANAL_TRUSTSTORE=/var/run/secrets/nais.io/vault/truststore.jks.b64
ENV NO_NAV_SDPKANAL_CREDENTIALSPATH=/var/run/secrets/nais.io/vault/credentials.json
ENV NO_NAV_SDPKANAL_SFTP_KEY_PATH=/var/run/secrets/nais.io/vault/sftp_key
ENV NO_NAV_SDPKANAL_SFTP_KNOWN_HOSTS=/var/run/secrets/nais.io/vault/known_hosts

ENV JAVA_OPTS='-Dlogback.configurationFile=logback-remote.xml -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -Djavax.net.ssl.keyStore=keystore.p12 -Djavax.net.ssl.keyStoreType=pkcs12 -Djavax.net.ssl.keyStorePassword=changeit'

COPY genkey.sh /init-scripts/10-genkey.sh
COPY build/libs/sdpkanal*-all.jar app.jar

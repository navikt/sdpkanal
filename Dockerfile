FROM navikt/java:8

ENV NO_NAV_DOKUMENT_PATH_PREFIX='/q1/dokumentdistribusjon/documentFileshare/'
ENV NO_NAV_SDPKANAL_KEYSTORE=/var/run/secrets/nais.io/vault/virksomhet.jks.b64
ENV NO_NAV_SDPKANAL_TRUSTSTORE=/var/run/secrets/nais.io/vault/truststore.jks.b64
ENV NO_NAV_SDPKANAL_CREDENTIALSPATH=/var/run/secrets/nais.io/vault/credentials.json
ENV NO_NAV_SDPKANAL_SFTP_KEY_PATH=/var/run/secrets/nais.io/vault/sftp_key

COPY target/sdpkanal*.jar app.jar

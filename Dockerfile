FROM navikt/java:11

ENV NO_NAV_SDPKANAL_KEYSTORE=/var/run/secrets/nais.io/virksomhetssertifikat/key.p12.b64
ENV NO_NAV_SDPKANAL_KEYSTORE_CREDENTIALS=/var/run/secrets/nais.io/virksomhetssertifikat/credentials.json

ENV NO_NAV_SDPKANAL_TRUSTSTORE=/var/run/secrets/nais.io/vault/truststore.p12.b64
ENV NO_NAV_SDPKANAL_CREDENTIALSPATH=/var/run/secrets/nais.io/vault/credentials.json
ENV NO_NAV_SDPKANAL_SFTP_KEY_PATH=/var/run/secrets/nais.io/vault/sftp_key
ENV NO_NAV_SDPKANAL_SFTP_KNOWN_HOSTS=/var/run/secrets/nais.io/vault/known_hosts

ENV JAVA_OPTS='-Dlogback.configurationFile=logback-remote.xml -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -XX:MaxRAMPercentage=75'

COPY build/libs/sdpkanal*-all.jar app.jar

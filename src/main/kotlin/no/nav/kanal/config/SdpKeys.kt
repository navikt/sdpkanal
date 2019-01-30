package no.nav.kanal.config

import no.difi.sdp.client2.domain.Noekkelpar
import java.io.FileInputStream
import java.security.KeyStore
import java.util.Base64

class SdpKeys constructor(
        keystorePath: String,
        truststorePath: String,
        credentials: VirksomhetssertifikatCredentials) {
    val keystore: KeyStore = KeyStore.getInstance(credentials.format).apply {
        load(Base64.getDecoder().wrap(FileInputStream(keystorePath)), credentials.password.toCharArray())
    }
    val truststore: KeyStore = KeyStore.getInstance(credentials.format).apply {
        load(Base64.getDecoder().wrap(FileInputStream(truststorePath)), credentials.password.toCharArray())
    }
    val keypair: Noekkelpar = Noekkelpar.fraKeyStoreOgTrustStore(keystore, truststore, credentials.alias, credentials.password)
}

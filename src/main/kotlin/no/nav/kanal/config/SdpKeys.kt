package no.nav.kanal.config

import no.difi.sdp.client2.domain.Noekkelpar
import java.io.FileInputStream
import java.security.KeyStore
import java.util.Base64

class SdpKeys constructor(
        keystorePath: String,
        truststorePath: String,
        keystoreCredentials: VirksomhetssertifikatCredentials,
        truststoreCredentials: VaultCredentials) {
    val keystore: KeyStore = KeyStore.getInstance(keystoreCredentials.type).apply {
        load(Base64.getDecoder().wrap(FileInputStream(keystorePath)), keystoreCredentials.password.toCharArray())
    }
    val truststore: KeyStore = KeyStore.getInstance(truststoreCredentials.truststoreType).apply {
        load(Base64.getDecoder().wrap(FileInputStream(truststorePath)), truststoreCredentials.truststorePassword.toCharArray())
    }
    val keypair: Noekkelpar = Noekkelpar.fraKeyStoreOgTrustStore(keystore, truststore, keystoreCredentials.alias, keystoreCredentials.password)
}

package no.nav.kanal.config

import no.difi.sdp.client2.domain.Noekkelpar
import java.io.FileInputStream
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.Base64
import java.security.cert.CertPathValidator
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters



class SdpKeys constructor(
    keystorePath: String,
    truststorePath: String,
    keystoreCredentials: VirksomhetssertifikatCredentials,
    truststoreCredentials: VaultCredentials,
    val validateRevocation: Boolean
) {
    val keystore: KeyStore = KeyStore.getInstance(keystoreCredentials.type).apply {
        load(Base64.getDecoder().wrap(FileInputStream(keystorePath)), keystoreCredentials.password.toCharArray())
    }
    val truststore: KeyStore = KeyStore.getInstance(truststoreCredentials.truststoreType).apply {
        load(Base64.getDecoder().wrap(FileInputStream(truststorePath)), truststoreCredentials.truststorePassword.toCharArray())
    }
    val keypair: Noekkelpar = Noekkelpar.fraKeyStoreOgTrustStore(keystore, truststore, keystoreCredentials.alias, keystoreCredentials.password)

    @Throws(CertPathValidatorException::class, InvalidAlgorithmParameterException::class)
    fun validateVirksomhetssertifikat() {
        validateCertificate(keypair.virksomhetssertifikat.x509Certificate)
    }

    @Throws(CertPathValidatorException::class, InvalidAlgorithmParameterException::class)
    fun validateCertificate(certificate: Certificate) {
        val cf = CertificateFactory.getInstance("X.509")
        val cp = cf.generateCertPath(listOf(certificate))
        val params = PKIXParameters(truststore).apply {
            isRevocationEnabled = validateRevocation
        }
        val cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType())
        cpv.validate(cp, params)
    }
}

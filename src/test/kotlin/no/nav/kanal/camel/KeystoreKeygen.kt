package no.nav.kanal.camel

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.ZonedDateTime
import java.util.Date
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import org.bouncycastle.crypto.util.PrivateKeyFactory

fun generateKeyStore() : KeyStore {
    Security.addProvider(BouncyCastleProvider())
    return KeyStore.getInstance("PKCS12").apply {
        load(null, null)
        generateKeysFor(984661185, "posten", "Posten test certificate", this)
        generateKeysFor(889640782, "app-key", "NAV test certificate", this)

        /*
         This is a workaround used for making the sikker digital post java client work.
         For testing I've pointed the client to the keystore generated by this code. Since the java library checks the
         number of certificates in the truststore to validate if its the right one we need to add a few extra
         certificates and keys, this is not actually needed by the parts we use in sdpkanal
          */

        generateKeysFor(1, "mock1", "dn=NAV test certificate", this)
        generateKeysFor(2, "mock2", "dn=NAV test certificate", this)
        generateKeysFor(3, "mock3", "dn=NAV test certificate", this)

        aliases().toList().map { getCertificate(it) }.forEach { println(it) }
    }
}

fun generateKeysFor(orgNr: Long, alias: String, orgName: String, keystore: KeyStore) {

    val keygen = KeyPairGenerator.getInstance("RSA", "BC")
    keygen.initialize(2048)
    val keyPair = keygen.genKeyPair()
    val publicKey = keyPair.public as RSAPublicKey
    val privateKey = keyPair.private as RSAPrivateKey
    val dn = X500Name("dn=$orgName, SERIALNUMBER=$orgNr")
    val validFrom = Date.from(ZonedDateTime.now().minusYears(1).toInstant())
    val validTo = Date.from(ZonedDateTime.now().plusYears(1).toInstant())
    val subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)
    val sigAlgId = DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA")
    val digAlgId = DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
    val privateKeyAsymKeyParam = PrivateKeyFactory.createKey(privateKey.encoded)
    val sigGen = BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam)
    val certificateHolder = X509v3CertificateBuilder(dn, BigInteger.valueOf(orgNr), validFrom, validTo, dn, subjPubKeyInfo)
            .addExtension(ASN1ObjectIdentifier("2.5.29.19"), true, BasicConstraints(true))
            .build(sigGen)
    val certificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder)
    keystore.setKeyEntry(alias, privateKey, "changeit".toCharArray(), arrayOf(certificate))
}

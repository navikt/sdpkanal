import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.cert.CertPathValidator
import java.security.cert.CertPathValidatorException
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.regex.Pattern
import javax.net.ssl.SSLContext

val password = charArrayOf('c', 'h', 'a', 'n', 'g', 'e', 'i', 't')
val prodUrls = arrayOf(
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/prod/BPClass3CA3.cer",
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/prod/BPClass3RootCA.cer",
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/prod/commfides_ca.cer",
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/prod/commfides_root_ca.cer"
)
val preprodUrls = arrayOf(
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/test/Buypass_Class_3_Test4_CA_3.cer",
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/test/Buypass_Class_3_Test4_Root_CA.cer",
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/test/commfides_test_ca.cer",
        "https://github.com/difi/sikker-digital-post-klient-java/raw/master/src/main/resources/certificates/test/commfides_test_root_ca.cer"
)

fun createTrustStore(urls: Array<String>, fileName: String) {
    val truststore = KeyStore.getInstance("pkcs12")
    truststore.load(null, null)
    for (url in urls) {
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(urlConnection.inputStream) as X509Certificate

        println(url)
        println(certificate.subjectDN)
        println(certificate.serialNumber)
        println(certificate.notBefore)
        println(certificate.notAfter)

        truststore.setCertificateEntry(url, certificate)
    }
    val buildPath = Paths.get("build", "truststores")
    Files.createDirectories(buildPath)
    truststore.store(Files.newOutputStream(buildPath.resolve(fileName)), password)
    truststore.store(Base64.getEncoder().wrap(Files.newOutputStream(buildPath.resolve("$fileName.b64"))), password)
}

println("Generating prod truststore:")
createTrustStore(prodUrls, "prod.p12")


println()
println()

println("Generating preprod truststore:")
createTrustStore(preprodUrls, "preprod.p12")

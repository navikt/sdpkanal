package no.nav.kanal.config

data class VaultCredentials(
    val mqUsername: String,
    val mqPassword: String,
    val truststorePassword: String,
    val truststoreType: String,
    val sftpUsername: String,
    val sftpKeyPassword: String,
    val serviceuserUsername: String,
    val serviceuserPassword: String,
    val applicationCertificatePassword: String?,
    val mqMutualTlsEnabled: Boolean = true
)

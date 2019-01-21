package no.nav.kanal.config

data class VaultCredentials(
    val mqUsername: String,
    val mqPassword: String,
    val virksomhetKeystorePassword: String,
    val virksomhetKeystoreAlias: String,
    val virksomhetKeystoreType: String,
    val truststorePassword: String,
    val truststoreType: String,
    val sftpUsername: String,
    val sftpKeyPassword: String,
    val serviceuserUsername: String,
    val serviceuserPassword: String,
    val applicationCertificatePassword: String
)

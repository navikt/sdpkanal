package no.nav.kanal.config

data class VaultCredentials(
    val mqUsername: String,
    val mqPassword: String,
    val virksomhetKeystorePassword: String,
    val virksomhetKeystoreAlias: String,
    val truststorePassword: String,
    val sftpKeyPassword: String
)

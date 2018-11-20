package no.nav.kanal.config

data class VaultCredentials(
    val mqUsername: String,
    val mqPassword: String,
    val flameKeystorePassword: String,
    val flameKeystoreAlias: String,
    val flameKeyPassword: String,
    val virksomhetKeystorePassword: String,
    val virksomhetKeystoreAlias: String,
    val truststorePassword: String
)

package no.nav.kanal.config.model;

public class VaultCredentials {
    private String mqUsername;
    private String mqPassword;
    private String flameKeystorePassword;
    private String flameKeystoreAlias;
    private String flameKeyPassword;
    private String virksomhetKeystorePassword;
    private String virksomhetKeystoreAlias;
    private String truststorePassword;

    public String getMqUsername() {
        return mqUsername;
    }

    public void setMqUsername(String mqUsername) {
        this.mqUsername = mqUsername;
    }

    public String getMqPassword() {
        return mqPassword;
    }

    public void setMqPassword(String mqPassword) {
        this.mqPassword = mqPassword;
    }

    public String getFlameKeystorePassword() {
        return flameKeystorePassword;
    }

    public void setFlameKeystorePassword(String flameKeystorePassword) {
        this.flameKeystorePassword = flameKeystorePassword;
    }

    public String getFlameKeystoreAlias() {
        return flameKeystoreAlias;
    }

    public void setFlameKeystoreAlias(String flameKeystoreAlias) {
        this.flameKeystoreAlias = flameKeystoreAlias;
    }

    public String getFlameKeyPassword() {
        return flameKeyPassword;
    }

    public void setFlameKeyPassword(String flameKeyPassword) {
        this.flameKeyPassword = flameKeyPassword;
    }

    public String getVirksomhetKeystorePassword() {
        return virksomhetKeystorePassword;
    }

    public void setVirksomhetKeystorePassword(String virksomhetKeystorePassword) {
        this.virksomhetKeystorePassword = virksomhetKeystorePassword;
    }

    public String getVirksomhetKeystoreAlias() {
        return virksomhetKeystoreAlias;
    }

    public void setVirksomhetKeystoreAlias(String virksomhetKeystoreAlias) {
        this.virksomhetKeystoreAlias = virksomhetKeystoreAlias;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }
}

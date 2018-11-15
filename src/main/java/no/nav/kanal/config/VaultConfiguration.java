package no.nav.kanal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.kanal.config.model.VaultCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class VaultConfiguration {
    @Value("${no.nav.sdpkanal.credentialsPath}")
    private String credentialsPath;

    @Bean
    public VaultCredentials vaultCredentials() throws Exception {
        return new ObjectMapper().readValue(new File(credentialsPath), VaultCredentials.class);
    }
}

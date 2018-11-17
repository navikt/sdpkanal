package no.nav.kanal.camel.ebms;

import no.digipost.api.EbmsEndpointUriBuilder;
import no.digipost.api.MessageSender;
import no.digipost.api.interceptors.KeyStoreInfo;
import no.digipost.api.representations.EbmsAktoer;
import no.nav.kanal.config.SdpKeys;
import no.nav.kanal.config.model.VaultCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;

@Service
public class DigipostEbms {
    private KeyStore keyStore;
    private MessageSender messageSender;
    private final EbmsAktoer databehandler = EbmsAktoer.avsender("889640782");
    private final EbmsAktoer mottaker = EbmsAktoer.meldingsformidler("984661185");

    @Autowired
    public DigipostEbms(
            SdpKeys sdpKeys,
            @Value("${ebms.msh.url}") String ebmsEndpoint,
            VaultCredentials credentials
    ) throws Exception {
        keyStore = sdpKeys.getKeystore();

        KeyStore truststore = sdpKeys.getTruststore();
        EbmsEndpointUriBuilder endpoint = EbmsEndpointUriBuilder.statiskUri(new URI(ebmsEndpoint));
        KeyStoreInfo keyStoreInfo = new KeyStoreInfo(keyStore, truststore, credentials.getVirksomhetKeystoreAlias(), credentials.getVirksomhetKeystorePassword());

        messageSender = MessageSender.create(endpoint, keyStoreInfo, databehandler, mottaker).build();
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public EbmsAktoer getDatabehandler() {
        return databehandler;
    }

    public EbmsAktoer getMottaker() {
        return mottaker;
    }
}

package no.nav.kanal;

import iaik.security.provider.IAIK;
import iaik.xml.crypto.XSecProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import java.security.Security;

@ImportResource("classpath:applicationContext.xml")
@SpringBootApplication
public class SdpKanalApplication {
    public static void main(String[] args) {
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        registerIAIKProvider();
        SpringApplication.run(SdpKanalApplication.class, args);
    }

    public static void registerIAIKProvider() {
        IAIK.addAsProvider();

        Security.addProvider(new XSecProvider());
    }
}

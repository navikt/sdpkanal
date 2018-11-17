package no.nav.kanal

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ImportResource

@ImportResource("classpath:applicationContext.xml")
@SpringBootApplication
open class SdpKanalApplication

fun main(args: Array<String>) {
    System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true")
    System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true")
    System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true")
    System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true")
    SpringApplication.run(SdpKanalApplication::class.java, *args)
}

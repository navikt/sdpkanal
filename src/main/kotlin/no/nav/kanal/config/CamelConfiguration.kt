package no.nav.kanal.config

import org.apache.camel.impl.DefaultShutdownStrategy
import org.apache.camel.spi.ShutdownStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class CamelConfiguration {
    @Bean
    open fun shutdownStrategy(): ShutdownStrategy = DefaultShutdownStrategy().apply { timeout  = 20 }
}

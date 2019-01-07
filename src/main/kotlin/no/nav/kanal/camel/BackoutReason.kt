package no.nav.kanal.camel

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

const val CAMEL_HEADER_BOQ_MESSAGE = "_exceptionMessage"
const val CAMEL_PROPERTY_ERROR_TO_PROPAGATE = "FEILMELDING"

class BackoutReason : Processor {
    private val log = LoggerFactory.getLogger(BackoutReason::class.java)

    override fun process(exchange: Exchange) {
        val e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)
        log.error("Sending message to backout, exception caught was ${exchange.loggingKeys()}",
                *exchange.loggingValues(),
                e)
        // set original message as In
        exchange.setIn(exchange.unitOfWork.originalInMessage)

        if (exchange.getProperty(CAMEL_PROPERTY_ERROR_TO_PROPAGATE) != null) {
            // set header that will propagate to message on BOQ
            exchange.getIn().setHeader(CAMEL_HEADER_BOQ_MESSAGE, exchange.getProperty(CAMEL_PROPERTY_ERROR_TO_PROPAGATE))
        }
    }
}

package no.nav.kanal.camel

import no.difi.begrep.sdp.schema_v10.SDPDigitalPost
import no.nav.kanal.ATTACHMENT_COUNTER
import no.nav.kanal.BILLABLE_BYTES_SUMMARY
import no.nav.kanal.EMAIL_NOTIFICATION_COUNTER
import no.nav.kanal.SECURITY_LEVEL_COUNTER
import no.nav.kanal.SMS_NOTIFICATION_COUNTER
import no.nav.kanal.SdpPayload
import org.apache.camel.Exchange
import org.apache.camel.Processor

class SdpPriceStatisticsCollector : Processor {

    override fun process(exchange: Exchange) {
        val sbd = exchange.getIn().getHeader(XmlExtractor.SDP_PAYLOAD, SdpPayload::class.java).standardBusinessDocument
        val billableBytes = exchange.getIn().header<Long>(BILLABLE_BYTES_HEADER)


        val orgNumber = sbd.standardBusinessDocumentHeader.receivers.first().identifier.value
        val digitalMail = sbd.any as SDPDigitalPost

        BILLABLE_BYTES_SUMMARY.labels(orgNumber).observe(billableBytes.toDouble())
        SECURITY_LEVEL_COUNTER.labels(orgNumber, digitalMail.digitalPostInfo.sikkerhetsnivaa.toString())
        ATTACHMENT_COUNTER.labels(orgNumber).inc(exchange.getIn().header<Int>(ATTACHMENT_COUNT_HEADER).toDouble())

        if (digitalMail.digitalPostInfo.varsler?.epostVarsel != null) {
            EMAIL_NOTIFICATION_COUNTER.labels(orgNumber).inc()
        }
        if (digitalMail.digitalPostInfo.varsler?.smsVarsel != null) {
            SMS_NOTIFICATION_COUNTER.labels(orgNumber).inc()
        }
    }
}

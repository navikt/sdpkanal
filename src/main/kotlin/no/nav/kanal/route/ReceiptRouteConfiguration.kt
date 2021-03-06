package no.nav.kanal.route

import io.prometheus.client.Summary
import no.digipost.api.representations.EbmsOutgoingMessage
import no.nav.kanal.METRICS_NAMESPACE
import no.nav.kanal.MPC_ID_HEADER
import no.nav.kanal.PRIORITY_HEADER
import no.nav.kanal.camel.EbmsPull
import no.nav.kanal.camel.ReceiptConfirm
import no.nav.kanal.camel.header
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint

const val RECEIPT_POLL_SUMMARY_HEADER = "RECEIPT_POLL_SUMMARY"
const val RECEIPT_SUMMARY_HEADER = "RECEIPT_ROUTE_SUMMARY"

val sdpReceiptPollSummary: Summary = Summary.Builder().namespace(METRICS_NAMESPACE).name("receipt_summary")
        .labelNames("route_name")
        .help("Summary over polling times in the receipt route").register()
val sdpReceiptWithPayloadSummary: Summary = Summary.Builder().name(METRICS_NAMESPACE).name("receipt_with_payload")
        .labelNames("route_name")
        .help("Summary over time it takes to receive a receipt with a payload").register()

fun CamelContext.createReceiptPollingRoute(
        routeName: String,
        mpcId: String,
        priority: EbmsOutgoingMessage.Prioritet,
        pollDelay: Long,
        ebmsPull: EbmsPull,
        receiptConfirm: ReceiptConfirm,
        receiptQueue: JmsEndpoint
) = object: RouteBuilder(this) {
    override fun configure() {
        // @formatter:off
        from("timer://${routeName}Timer?period=500")
                .id(routeName)
                .setHeader(RECEIPT_POLL_SUMMARY_HEADER) { sdpReceiptPollSummary.labels(routeName).startTimer() }
                .setHeader(RECEIPT_SUMMARY_HEADER) { sdpReceiptWithPayloadSummary.labels(routeName).startTimer() }
                .setHeader(MPC_ID_HEADER) { mpcId }
                .setHeader(PRIORITY_HEADER) { priority }
                .process(ebmsPull)
                .choice()
                .`when`(body().isNotNull)
                    .to(receiptQueue)
                    .process { it.`in`.header<Summary.Timer>(RECEIPT_SUMMARY_HEADER).close() }
                    .process(receiptConfirm)
                .otherwise()
                    //.log("Polling $routeName returned empty result")
                    .delay(pollDelay)
                .endChoice()
                .process { it.`in`.header<Summary.Timer>(RECEIPT_POLL_SUMMARY_HEADER).close() }
                .end()
        // @formatter:on
    }

}

package no.nav.kanal

import io.prometheus.client.Counter
import io.prometheus.client.Summary

val NAMESPACE = "sdpkanal"

val BILLABLE_BYTES_SUMMARY: Summary = Summary.Builder().namespace(NAMESPACE)
        .name("billable_bytes_counter")
        .labelNames("org_number")
        .help("Counts the number of billable bytes")
        .register()
val SECURITY_LEVEL_COUNTER: Counter = Counter.Builder().namespace(NAMESPACE)
        .name("security_level_counter")
        .labelNames("org_number", "security_level")
        .help("Counts the number of messages that require two factor authentication to see the message")
        .register()

val ATTACHMENT_COUNTER: Counter = Counter.Builder().namespace(NAMESPACE)
        .name("attachment_counter")
        .labelNames("org_number")
        .help("Counts the number of attachments")
        .register()

val EMAIL_NOTIFICATION_COUNTER: Counter = Counter.Builder().namespace(NAMESPACE)
        .name("email_notification_counter")
        .labelNames("org_number")
        .help("Counts the number of email notifications")
        .register()

val SMS_NOTIFICATION_COUNTER: Counter = Counter.Builder().namespace(NAMESPACE)
        .name("sms_receipt_counter")
        .labelNames("org_number")
        .help("Counts the number of sms notifications")
        .register()

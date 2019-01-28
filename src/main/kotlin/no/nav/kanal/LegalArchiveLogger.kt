package no.nav.kanal

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.auth.basic.BasicAuth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments
import org.apache.http.HttpHost
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.Base64

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ArchiveRequest(
        @JsonProperty(value = "meldingsId") val messageId: String,
        @JsonProperty(value = "avsender") val sender: String,
        @JsonProperty(value = "mottaker") val receiver: String,
        @JsonProperty(value = "meldingsInnhold") val messageContent: ByteArray,
        @JsonProperty(value = "joarkRef") val joarkReference: String? = null,
        @JsonProperty(value = "antallAarLagres") val retentionInYears: Int? = null
)

data class ArchiveResponse(
        val id: Int
)

class LegalArchiveLogger(
    val legalArchiveUrl: String,
    legalArchiveUsername: String,
    legalArchivePassword: String,
    clientConfiguration: HttpClientEngine = Apache.create {  }
) {
    val log: Logger = LoggerFactory.getLogger(LegalArchiveLogger::class.java)
    val client = HttpClient(clientConfiguration) {
        install(BasicAuth) {
            this.username = legalArchiveUsername
            this.password = legalArchivePassword
        }
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }
    suspend fun archiveDocument(archiveRequest: ArchiveRequest): ArchiveResponse = client.post(legalArchiveUrl) {
        contentType(ContentType.Application.Json)
        body = archiveRequest
    }

    fun archiveDocument(messageId: String, sender: String, receiver: String, messageContent: Deferred<ByteArray>) = client.let {
        it.async {
            archiveDocument(ArchiveRequest(messageId, sender, receiver, messageContent.await()))
        }
    }

    suspend fun archiveDocumentLogOnException(messageId: String, sender: String, receiver: String, messageContent: Deferred<ByteArray>, msgType: String) = try {
        val response = archiveDocument(messageId, sender, receiver, messageContent)
        log.info("Logged event to legal archive with type: {} id: {} ({}, {})",
                StructuredArguments.keyValue("messageType", msgType),
                StructuredArguments.keyValue("legalArchiveId", response.await().id),
                StructuredArguments.keyValue("conversationId", messageId),
                StructuredArguments.keyValue("callId", messageId))
    } catch (e: Exception) {
        log.error("Failed to send message to legal archive ({}, {}, {})",
                StructuredArguments.keyValue("messageType", msgType),
                StructuredArguments.keyValue("conversationId", messageId),
                StructuredArguments.keyValue("callId", messageId), e)
    }
}

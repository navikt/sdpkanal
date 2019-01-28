package no.nav.kanal.camel

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.runBlocking
import no.nav.kanal.ArchiveRequest
import no.nav.kanal.LegalArchiveLogger
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.Base64
import java.util.concurrent.TimeUnit

object LegalArchiveLoggerSpek : Spek({
    val port = randomPort()
    val httpServer = embeddedServer(CIO, port) {
        routing {
            post("/legalarchive") {
                val auth = call.request.header(HttpHeaders.Authorization)
                val expectedUsernamePassword = Base64.getEncoder().encodeToString("tusr:tpwd".toByteArray(Charsets.UTF_8))
                if (auth == "Basic $expectedUsernamePassword") {
                    call.respondText("{\"id\":1}", ContentType.Application.Json)
                } else {
                    call.respondText("Missing authentication", status = HttpStatusCode.Unauthorized)
                }
            }
        }
    }.start()

    afterGroup {
        httpServer.stop(10, 10, TimeUnit.SECONDS)
    }
    val legalArchiveLogger = LegalArchiveLogger("http://localhost:$port/legalarchive", "tusr", "tpwd")
    describe("Sending a request to the legal Archive") {
        it("should result in a OK response") {
            runBlocking {
                legalArchiveLogger.archiveDocument(ArchiveRequest(
                        "123",
                        "sender",
                        "receiver",
                        "content".toByteArray(Charsets.UTF_8)
                        ))
            }
        }
    }
})

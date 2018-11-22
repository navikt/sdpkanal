package no.nav.kanal.camel

import org.amshove.kluent.shouldNotEqual
import org.apache.commons.io.IOUtils
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object XmlExtractorSpek : Spek({
    describe("A valid inlined XML") {
        val xmlExtractor = XmlExtractor()
        val xml = IOUtils.resourceToByteArray("/payloads/inline.xml").toString(Charsets.UTF_8)
        val payload = xmlExtractor.extractPayload(xml)

        it("Should unmarshal all fields") {
            payload.standardBusinessDocument shouldNotEqual null
            payload.manifest shouldNotEqual null
            payload.certificate shouldNotEqual null
            payload.hasPriority shouldNotEqual null
        }
    }
})

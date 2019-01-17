package no.nav.kanal.ebms

import no.digipost.api.representations.KanBekreftesSomBehandletKvittering
import no.digipost.api.representations.KvitteringsReferanse
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error as EbmsError
import org.w3.xmldsig.Reference

data class EbmsReceipt(
        private val messageId: String,
        val refToMessageId: String,
        val conversationId: String?,
        val sbdBytes: ByteArray,
        val references: Collection<Reference>
) : KanBekreftesSomBehandletKvittering {
    override fun getMeldingsId(): String {
        return messageId
    }

    override fun getReferanseTilMeldingSomKvitteres(): KvitteringsReferanse =
            KvitteringsReferanse.builder(references.first()).build()


}

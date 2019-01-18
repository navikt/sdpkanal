package no.nav.kanal.ebms

const val CONVERSATION_ID_PROPERTY = "loggable_context.conversation_id"
const val SHOULD_BE_LOGGED_PROPERTY = "loggable_context.should_be_loggable"
interface LoggableContext {
    val conversationId: String
    val shouldBeLogged: Boolean
}

package no.nav.kanal.camel

import javax.xml.namespace.NamespaceContext

object EbmsNamespaceContext : NamespaceContext {
    override fun getNamespaceURI(prefix: String?): String? = when (prefix) {
        "wsse" -> "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
        "ds" -> "http://www.w3.org/2000/09/xmldsig#"
        else -> null
    }

    override fun getPrefix(namespaceURI: String?): String? = when (namespaceURI) {
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" -> "wsse"
        "http://www.w3.org/2000/09/xmldsig#" -> "ds"
        else -> null
    }

    override fun getPrefixes(namespaceURI: String?): MutableIterator<String?> = mutableListOf(getPrefix(namespaceURI)).iterator()

}

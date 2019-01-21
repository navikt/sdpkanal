import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.springframework.ws.WebServiceMessage
import java.io.ByteArrayOutputStream

fun WebServiceMessage.toByteArray(coroutineScope: CoroutineScope) = coroutineScope.async {
    ByteArrayOutputStream().use {
        writeTo(it)
        it
    }.toByteArray()
}

package no.nav.kanal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

data class PooledConnection<T>(
        private val connectionPool: ConnectionPool<T>,
        val connection: T
) : AutoCloseable {
    suspend fun suspendClose() {
        connectionPool.sendChannel.send(this)
    }

    override fun close() {
        runBlocking {
            suspendClose()
        }
    }
}

class ConnectionPool<T>(factory: () -> T, private val closingFunction: (T) -> Unit, private val numberOfConnections: Int = 4) : AutoCloseable {
    internal val sendChannel: Channel<PooledConnection<T>> = Channel(numberOfConnections)

    init {
        runBlocking {
            0.until(numberOfConnections).forEach {
                sendChannel.send(PooledConnection(this@ConnectionPool, factory()))
            }
        }
    }

    suspend fun getConnection(): PooledConnection<T> = sendChannel.receive()

    fun blocking(connectionHandler: suspend (T) -> Unit) {
        runBlocking {
            invoke(connectionHandler)
        }
    }

    suspend operator fun invoke(connectionHandler: suspend (T) -> Unit) {
        val connection = getConnection()
        connectionHandler(connection.connection)
        connection.suspendClose()
    }

    override fun close() {
        runBlocking {
            0.until(numberOfConnections).forEach {
                closingFunction(sendChannel.receive().connection)
            }
        }
    }
}

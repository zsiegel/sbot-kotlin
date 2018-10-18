import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.charset.StandardCharsets

private val MAIN_LOGGER = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    val discoveryThread = LanDiscoveryThread(handler = { peer ->
        MAIN_LOGGER.debug("Found peer: $peer")
    })
    discoveryThread.start()
    discoveryThread.join()
//    "net:192.168.11.121:8008~shs:E3q64sttcWpcJdaPhSEnHnFDikDDpHx9Qfgb76Y9DDU=".parseIpv4Peer()
}

data class DiscoveredPeer(val sourceType: String,
                          val host: String,
                          val port: Int,
                          val publicKey: String)

fun String.parseIpv4Peer(): DiscoveredPeer? {
    //TODO very unsafe parsing - do better
    val addressAndKeyParts = this.split(":")
    if (addressAndKeyParts.size != 4) return null

    val port = addressAndKeyParts[2].split("~")[0]
    return DiscoveredPeer(sourceType = addressAndKeyParts[0],
                          host = addressAndKeyParts[1],
                          port = port.toInt(),
                          publicKey = addressAndKeyParts[3])
}

class LanDiscoveryThread(private val socket: DatagramSocket = DatagramSocket(8008),
                         private val handler: (DiscoveredPeer) -> Unit) : Thread() {

    private var running: Boolean = false
    private val logger = LoggerFactory.getLogger("LanDiscoveryThread")

    override fun run() {
        super.run()
        running = true
        logger.debug("Started lan discovery")
        while (running) {

            //read on the socket
            val buffer = ByteArray(128)
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)

            logger.debug("received udp packet")

            //attempt to parse each line as a peer
            val messageBytes = packet.data.sliceArray(0 until packet.length)
            val message = String(messageBytes, StandardCharsets.UTF_8)

            val discoveredPeer = message.parseIpv4Peer()
            discoveredPeer?.let {
                logger.debug("Discovered a peer: $it")

                //notify handler of peer
                handler.invoke(discoveredPeer)
            }
        }

        //handle errors
        socket.close()
    }
}
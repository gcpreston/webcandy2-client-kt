import opc.OpcClient
import org.phoenixframework.Socket

fun rgbToInt(r: Int, g: Int, b: Int): Int {
    // NOTE: Ignores alpha value
    val hexString = r.toString(16) + g.toString(16) + b.toString(16)
    return hexString.toInt(16)
}

fun main(args: Array<String>) {
    // Initialize Fadecandy
    val server = OpcClient("localhost", 7890)
    val fadecandy = server.addDevice()
    val strip = fadecandy.addPixelStrip(0, 512)

    // Create the Socket
    val params = hashMapOf("token" to "abc123")
    val socket = Socket("http://localhost:4000/socket", params)

    // Listen to events on the Socket
    socket.logger = { println(it) }
    socket.onOpen { println("Socket Opened") }
    socket.onClose { println("Socket Closed") }
    socket.onError { throwable, response -> println(response) }

    socket.connect()

    // Join channels and listen to events
    val chatroom = socket.channel("room:lobby")
    chatroom.on("shout") { message ->
        for (i in 0..511) {
            val rgba = message.payload
            val r = rgba["r"] as Double
            val g = rgba["g"] as Double
            val b = rgba["b"] as Double
            strip.setPixelColor(i, rgbToInt(r.toInt(), g.toInt(), b.toInt()))
        }
        server.show()
    }

    chatroom.join()
        .receive("ok") { /* Joined the chatroom */ }
        .receive("error") { /* failed to join the chatroom */ }

    server.close()
}
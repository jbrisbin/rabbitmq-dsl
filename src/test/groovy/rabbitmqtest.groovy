import org.springframework.amqp.core.MessageListener

mq.on error: { err -> err.printStackTrace() },
		myevent: [{ msg -> println "myevent: ${new String(msg.body)}"; return false }],
		afterPublish: { exchange, routingKey, msg -> log.info("Published to " + exchange + "/" + routingKey) }

// Initialization
mq { channel ->
	//channel.queueDelete("test")
	//channel.exchangeDelete("test")
}

//helper.format("[DEBUG]: %s", "debug data")

mq.exchange(name: "test", type: "direct") {

	queue(name: "test", routingKey: "test.key") {
		consume onmessage: "myevent"
	}

	def consumer
	def listener = [
		    onMessage: { msg ->
					println "Invoked from a standard MessageListener"
					consumer?.shutdown()
				}
		] as MessageListener

	queue(name: null, routingKey: "test.key") {
		consumer = consume onmessage: listener
	}

	queue(name: null, routingKey: "test2.key") {
		consume(ack: "auto") { msg ->
			def body = new String(msg.body)
			println "body: ${body}"
			println "myHeaderValue: ${msg.messageProperties.headers['myHeaderValue']}"

			if (msg.body == "this is from consumer".bytes) {
				println "Cancelling consumer..."
				return false
			} else {
				println "Trying again..."
				publish(myHeaderValue: "customHeader2") {
					"this is from consumer"
				}
				return true
			}

		}
	}

	publish(routingKey: "test.key") {
		"this is from a publish"
	}

	publish(routingKey: "test2.key", myHeaderValue: "customHeader", contentType: "text/plain") { out ->
		out.write("these are test bytes".bytes)
	}

}


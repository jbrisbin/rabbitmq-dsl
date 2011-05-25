mq.on error: { err -> err.printStackTrace() },
		myevent: { msg -> println "myevent: ${new String(msg.body)}" },
		afterPublish: { exchange, routingKey, msg -> log.info("Published to " + exchange + "/" + routingKey) }

// Initialization
mq { channel ->
	//channel.queueDelete("test")
	//channel.exchangeDelete("test")
}

//helper.format("[DEBUG]: %s", "debug data")

mq.exchange(name: "test", type: "direct") {

	// Named, non-durable queue
	queue(name: "test", routingKey: "test.key") {
		consume onmessage: "myevent"
	}

	// Anonymous (server-generated) non-durable queue
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

	// Poke some messages
	publish(routingKey: "test.key") {
		"this is from a publish"
	}

	publish(routingKey: "test2.key", myHeaderValue: "customHeader", contentType: "text/plain") { out ->
		out.write("these are test bytes".bytes)
	}

}

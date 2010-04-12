mq.on error: {err ->
  err.printStackTrace()
}, myevent: {msg ->
  stdout.write((msg.bodyAsString + "\n").bytes)
  stdout.flush()
}, afterPublish: {exchange, routingKey, msg ->
  log.info("Published to " + exchange + "/" + routingKey)
}

// Initialization
mq {channel ->
  //channel.queueDelete("test")
  //channel.exchangeDelete("test")
}

mq.exchange name: "test", type: "direct", durable: false, autoDelete: true, {
  // Named, non-durable queue
  queue name: "test", routingKey: "test.key", {
    consume tag: "test", onmessage: "myevent"
  }
  // Anonymous (server-generated) non-durable queue
  queue name: null, routingKey: "test2.key", {
    consume tag: "test2", onmessage: {msg ->
      log.info(msg.bodyAsString)
      stdout.write((msg.bodyAsString + "\n").bytes)
      stdout.write(("myHeaderValue=" + msg.properties.headers["myHeaderValue"] + "\n").bytes)
      stdout.flush()
      if (msg.bodyAsString.equals("this is from consumer")) {
        return false
      } else {
        def headers = ["myHeaderValue": "something"]
        send("test", "test2.key", headers, "this is from consumer".bytes)
        return true
      }
    }
  }
  // Poke some messages
  queue routingKey: "test.key", {
    publish body: "this is a test"
  }
  queue routingKey: "test2.key", {
    publish myHeaderValue: "customHeader", body: {msg, out ->
      msg.properties.contentType = "text/plain"
      out.write("these are test bytes".bytes)
    }
  }
}

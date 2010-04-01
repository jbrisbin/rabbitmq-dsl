mq.on error: {err ->
  log.error(err.message)
}, myevent: {msg ->
  stdout.write(msg.body)
}, afterPublish: {exchange, routingKey, msg ->
  log.info("Published to " + exchange + "/" + routingKey)
}

// Initialization
mq {channel ->
  //channel.queueDelete("test")
  //channel.exchangeDelete("test")
}

mq.exchange(name: "test", durable: false, autoDelete: true) {
  // Named, non-durable queue
  queue name: "test", routingKey: "test.key", {
    consume tag: "test", onmessage: "myevent"
  }
  // Anonymous (server-generated) non-durable queue
  queue name: null, routingKey: "test2.key", {
    consume tag: "test2", onmessage: {msg ->
      log.info(msg.bodyAsString)
      stdout.write(msg.body)
      log.info("myHeaderValue=" + msg.properties.headers["myHeaderValue"])
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

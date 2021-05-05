package com.insectnet.lhaas;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.Future;

public class DatabaseVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.eventBus().consumer("database.vertx.addr", msg -> {
      JsonObject params = (JsonObject) msg.body();
      System.out.println(params);
      msg.reply("Hello from DatabaseVerticle, " + params.getString("sign"));
    });
  }

}

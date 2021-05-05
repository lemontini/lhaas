package com.insectnet.lhaas;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // deploy additional verticles
    vertx.deployVerticle(new HoroscopeVerticle());
    vertx.deployVerticle(new DatabaseVerticle());

    // configure router
    Router router = Router.router(vertx);
    router.get("/").handler(this::horoscopeService);
    router.get("/db").handler(this::databaseService);

    // start http server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onSuccess(server -> {
        startPromise.complete();
        System.out.println("HTTP server started on port " + server.actualPort());
      })
      .onFailure(error -> {
        startPromise.fail(error.getCause());
      });
  }

  void horoscopeService(RoutingContext context) {
    MultiMap queryParams = context.queryParams();
    // convert params to JSON object for transfer as a message
    JsonObject params = new JsonObject();
    queryParams.forEach(param -> {
      params.put(param.getKey(), param.getValue());
    });
    vertx.eventBus().request("horoscope.vertx.addr", params, reply -> {
      context.json(reply.result().body());
    });
  }

  void databaseService(RoutingContext context) {
    MultiMap queryParams = context.queryParams();
    // convert params to JSON object for transfer as a message
    JsonObject params = new JsonObject();
    queryParams.forEach(param -> {
      params.put(param.getKey(), param.getValue());
    });
    vertx.eventBus().request("database.vertx.addr", params, reply -> {
      if (reply.failed()) System.out.println("ERROR");
      context.request().response().end((String) reply.result().body());
    });
  }

}

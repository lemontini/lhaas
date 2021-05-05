package com.insectnet.lhaas;

import com.insectnet.lhaas.model.Score;
import com.insectnet.lhaas.model.Sign;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.time.Year;
import java.time.YearMonth;
import java.util.EnumMap;

public class HoroscopeVerticle extends AbstractVerticle {

  SQLClient client;
  SQLConnection connection;

  public EnumMap<Sign, Score> db = new EnumMap<>(Sign.class);

  public boolean dbContains(String query) {
    for (Sign sign : Sign.values()) {
      if (sign.name().equalsIgnoreCase(query)) return true;
    }
    return false;
  }

  Future<Void> configureSqlClient() {
    // client = JDBCClient.createShared(vertx, config().getJsonObject("db"));
    Promise<Void> promise = Promise.promise();

    // WARNING! change "user" and "password" to your SQL database credentials
    JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:mysql://localhost:3306/?serverTimezone=UTC")
      .put("driver_class", "com.mysql.cj.jdbc.Driver")
      .put("max_pool_size", 10)
      .put("user", "****")
      .put("password", "****")
    );

    // check if connection is possible
    client.getConnection(conn -> {
      if (conn.failed()) {
        System.err.println(conn.cause().getMessage());
        promise.fail(conn.cause().getMessage());
      } else {
        connection = conn.result();
        promise.complete();
      }
    });

    return promise.future();
  }

  Future<Void> setSchema() {
    Promise<Void> promise = Promise.promise();
    // set connection to schema "horoscope"
    connection.execute("USE horoscope", res -> {
      if (res.failed()) {
        promise.fail(res.cause().getMessage());
        throw new RuntimeException(res.cause());
      } else promise.complete();
    });
    return promise.future();
  }

  Future<Void> createDB() {
    Promise<Void> promise = Promise.promise();
    // create database "horoscope"
    connection.execute("CREATE TABLE IF NOT EXISTS horoscope(year int primary key, name varchar(255))", res -> {
      if (res.failed()) {
        promise.fail(res.cause().getMessage());
        throw new RuntimeException(res.cause());
      } else promise.complete();
    });
    return promise.future();
  }

  public void queryDB(Year year) {
    // Chain the futures:
    // 1. check if the connection established
    // 2. Query from database for the user's defined year
    // 2a. If year not found, generate scores and write to database the new record
    // 3. Read from database required record
    System.out.print("Generating scores for " + year + " year...");
    for (Sign sign : Sign.values()) {
      db.put(sign, new Score(year));
    }
    System.out.println(" [OK]");
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    // configureSqlClient().compose(setSchema().compose(createDB()));

    vertx.eventBus().consumer("horoscope.vertx.addr", msg -> {
      JsonObject params = (JsonObject) msg.body();
      JsonObject response = new JsonObject();

      if (!params.containsKey("sign")) {
        response.put("ERROR", "Parameter 'sign' not specified");
      } else if (!params.containsKey("year")) {
        response.put("ERROR", "Parameter 'year' not specified");
      } else {
        String sign = (String) params.getValue("sign");
        String year = (String) params.getValue("year");

        if (!dbContains(sign)) {
          String res = "Parameter 'sign' incorrect. Use one of the following: ";
          for (Sign entry : Sign.values()) {
            res += entry.name() + ", ";
          }
          response.put("ERROR", res);
        } else {

          // parameters are correct, proceed with the business logic
          int iYear = Integer.parseInt(year);
          Year objYear = Year.of(iYear);

          // generate score database entry of a given year for all signs
          queryDB(Year.of(iYear));

          // return scores for the given year for the given Zodiac sign
          for (int monthCounter = 1; monthCounter <= 12; monthCounter++) {
            YearMonth queryMonth = objYear.atMonth(monthCounter);
            JsonObject monthlyScores = new JsonObject();

            for (int dayCounter = 1; dayCounter <= queryMonth.lengthOfMonth(); dayCounter++) {
              int dayScore = db.get(Sign.valueOf(sign.toUpperCase())).getScore(objYear, monthCounter, dayCounter);
              monthlyScores.put(String.valueOf(dayCounter), dayScore);
            }
            response.put(queryMonth.getMonth().name(), monthlyScores);
          }

          // return best month in the given year for the given sign
          response.put("bestMonth", db.get(Sign.valueOf(sign.toUpperCase())).getBestMonth(objYear));

          // calculate the the sign with the best year score overall
          Sign bestSign = Sign.AQUARIUS;
          float currScore = 0;
          for (Sign item : Sign.values()) {
            float currMonthScore = db.get(item).getYearlyAverage(objYear);
            if (currMonthScore > currScore) {
              currScore = currMonthScore;
              bestSign = item;
            }
          }
          // return the sign with the best yearly scores overall
          response.put("bestYearFor", bestSign.name());
        }
      }

      msg.reply(response);
    });
  }

}

package com.insectnet.lhaas;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.time.Year;
import java.time.YearMonth;
import java.util.EnumMap;

public class MainVerticle extends AbstractVerticle {

  public EnumMap<Sign, Score> db = new EnumMap<>(Sign.class);

  public boolean dbContains(String query) {
    for (Sign sign : Sign.values()) {
      if (sign.name().equalsIgnoreCase(query)) return true;
    }
    return false;
  }

  public void generateDB(Year year) {
    System.out.print("Generating scores for " + year + " year...");
    for (Sign sign : Sign.values()) {
      db.put(sign, new Score(year));
    }
    System.out.println(" [OK]");
  }

  @Override
  public void start(Promise<Void> startPromise) {

    Router router = Router.router(vertx);

    router.route().handler(context -> {
      MultiMap queryParams = context.queryParams();
      JsonObject response = new JsonObject();

      if (!queryParams.contains("sign")) {
        response.put("ERROR", "Parameter 'sign' not specified");
      } else if (!queryParams.contains("year")) {
        response.put("ERROR", "Parameter 'year' not specified");
      } else {
        String sign = queryParams.get("sign");
        String year = queryParams.get("year");

        if (!dbContains(sign)) {
          String res = "Parameter 'sign' incorrect. Use one of the following: ";
          for (Sign entry : Sign.values()) {
            res += entry.name() + ", ";
          }
          response.put("ERROR", res);
        } else {

          int iYear = Integer.parseInt(year);
          Year objYear = Year.of(iYear);

          // generate score database entry of a given year for all signs
          generateDB(Year.of(iYear));

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

      context.json(response);
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onSuccess(server -> System.out.println("HTTP server started on port " + server.actualPort()));

    // vertx.createHttpServer().requestHandler(req -> {
    //   req.response()
    //     .putHeader("content-type", "text/plain")
    //     .end("Hello from Vert.x!");
    // }).listen(8888, http -> {
    //   if (http.succeeded()) {
    //     startPromise.complete();
    //     System.out.println("HTTP server started on port 8888");
    //   } else {
    //     startPromise.fail(http.cause());
    //   }
    // });
  }
}

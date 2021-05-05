package com.insectnet.lhaas.model;

import java.time.Month;
import java.time.Year;

public class Score {
  private final int[] scores;

  public int[] getScores() {
    return scores;
  }

  public int getScore(Year year, int month, int day) {
    return scores[year.atMonth(month).atDay(day).getDayOfYear() - 1];
  }

  public Month getBestMonth(Year year) {
    float[] monthScores = new float[12];
    for (int month = 1; month <= 12; month++) {
      float avgPerMonth = 0;
      int daysInMonth = year.atMonth(month).lengthOfMonth();
      for (int day = 1; day <= daysInMonth; day++) {
        int currentDayInYear = year.atMonth(month).atDay(day).getDayOfYear();
        avgPerMonth += scores[currentDayInYear - 1];
      }
      avgPerMonth /= daysInMonth;
      monthScores[month - 1] = avgPerMonth;
    }

    float maxValue = monthScores[0];
    int bestMonth = 1;
    for (int month = 1; month <= 12; month++) {
      if (monthScores[month - 1] > maxValue) {
        maxValue = monthScores[month - 1];
        bestMonth = month;
      }
    }
    return Month.of(bestMonth);
  }

  public float getYearlyAverage(Year year) {
    int lengthOfYear = year.length();
    float avg = 0;
    for (int day = 1; day <= lengthOfYear; day++) {
      avg += scores[day - 1];
    }
    avg /=lengthOfYear;
    return avg;
  }

  public Score(Year year) {
    int days = year.length();
    scores = new int[days];
    for (int i = 0; i < days; i++) {
      this.scores[i] = (int) (Math.random() * 10 + 1);
    }
  }
}

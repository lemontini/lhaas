package com.insectnet.lhaas.model;

public enum Sign {
  AQUARIUS(1),
  PISCES(2),
  ARIES(3),
  TAURUS(4),
  GEMINI(5),
  CANCER(6),
  LEO(7),
  VIRGO(8),
  LIBRA(9),
  SCORPIO(10),
  SAGITTARIUS(11),
  CAPRICORN(12);

  public final int month;

  Sign(int month) {
    this.month = month;
  }

}

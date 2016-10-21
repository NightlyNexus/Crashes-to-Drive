package com.nightlynexus.crashestodrive;

import com.squareup.moshi.Json;
import java.util.Arrays;
import java.util.Date;

public final class Crash {
  @Json(name = "value1") public final Date occurred;
  @Json(name = "value2") public final String message;
  @Json(name = "value3") public final String stackTrace;

  static Crash create(Throwable e) {
    return new Crash(new Date(), e.getMessage(), Arrays.toString(e.getStackTrace()));
  }

  private Crash(Date occurred, String message, String stackTrace) {
    this.occurred = occurred;
    this.message = message;
    this.stackTrace = stackTrace;
  }

  @Override public String toString() {
    return "Crash{" +
        "occurred=" + occurred +
        ", message='" + message + '\'' +
        ", stackTrace='" + stackTrace + '\'' +
        '}';
  }
}

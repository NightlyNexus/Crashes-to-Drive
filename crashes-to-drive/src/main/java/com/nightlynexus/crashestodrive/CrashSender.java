package com.nightlynexus.crashestodrive;

public interface CrashSender {
  /**
   * May call on any thread.
   */
  interface Callback {
    void onSuccess();

    void onFailure();
  }

  void send(Crash crash, Callback callback);
}

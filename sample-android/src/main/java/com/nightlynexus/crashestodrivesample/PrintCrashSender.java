package com.nightlynexus.crashestodrivesample;

import com.nightlynexus.crashestodrive.Crash;
import com.nightlynexus.crashestodrive.CrashSender;
import java.io.PrintStream;

final class PrintCrashSender implements CrashSender {
  private final PrintStream printStream;

  PrintCrashSender(PrintStream printStream) {
    this.printStream = printStream;
  }

  @Override public void send(Crash crash, Callback callback) {
    printStream.println(crash);
    callback.onSuccess();
  }
}

package com.nightlynexus.crashestodrivesample;

import android.app.Activity;
import android.os.Bundle;
import com.nightlynexus.crashestodrive.CrashReporter;
import java.io.File;
import java.util.Random;
import okhttp3.OkHttpClient;

public final class MightCrashActivity extends Activity {
  private static final String MAKER_EVENT = ; // TODO
  private static final String MAKER_KEY = ; // TODO

  public static void main(String[] args) {
    File crashes = new File(System.getProperty("user.home"), "crashes-to-drive");
    CrashReporter.plantWithSender(crashes, new PrintCrashSender(System.out));
    throw new RuntimeException("Crashes happen.");
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    OkHttpClient client = new OkHttpClient();
    File crashes = new File(getFilesDir(), "crashes-to-drive");
    CrashReporter.plant(crashes, client, MAKER_EVENT, MAKER_KEY);
    Random random = new Random();
    if (random.nextInt(4) != 0) {
      throw new RuntimeException("Crashes happen.");
    }
  }
}

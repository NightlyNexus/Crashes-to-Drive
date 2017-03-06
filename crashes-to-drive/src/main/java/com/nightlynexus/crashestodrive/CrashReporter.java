package com.nightlynexus.crashestodrive;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;
import com.squareup.tape2.ObjectQueue;
import com.squareup.tape2.QueueFile;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import okhttp3.OkHttpClient;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

public final class CrashReporter {
  final Lock lock = new ReentrantLock();
  final ObjectQueue<Crash> queue;
  private final CrashSender sender;
  boolean inFlight;

  public static CrashReporter plant(File file, OkHttpClient client, String makerEvent,
      String makerKey) {
    Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
    JsonAdapter<Crash> adapter = moshi.adapter(Crash.class);
    return plantWithSender(file,
        new IftttToDriveNetworkCrashSender(client, adapter, makerEvent, makerKey));
  }

  public static CrashReporter plantWithSender(File file, CrashSender sender) {
    Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
    JsonAdapter<Crash> adapter = moshi.adapter(Crash.class);
    return plantWithSender(file, sender, adapter);
  }

  private static CrashReporter plantWithSender(File file, CrashSender sender,
      JsonAdapter<Crash> adapter) {
    ObjectQueue<Crash> queue;
    try {
      QueueFile queueFile = new QueueFile.Builder(file).build();
      queue = ObjectQueue.create(queueFile, new CrashConverter(adapter));
    } catch (IOException e) {
      e.printStackTrace();
      queue = ObjectQueue.createInMemory();
    }
    final CrashReporter crashReporter = new CrashReporter(queue, sender);
    crashReporter.sendNext();
    final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override public void uncaughtException(Thread thread, Throwable e) {
        Throwable cause = e;
        Throwable forward = cause.getCause();
        while (forward != null) {
          cause = forward;
          forward = forward.getCause();
        }
        crashReporter.addCrash(Crash.create(cause));
        defaultHandler.uncaughtException(thread, e);
      }
    });
    return crashReporter;
  }

  private CrashReporter(ObjectQueue<Crash> queue, CrashSender sender) {
    this.queue = queue;
    this.sender = sender;
  }

  void addCrash(Crash crash) {
    lock.lock();
    try {
      queue.add(crash);
      lock.unlock();
      send(crash);
    } catch (IOException e) {
      lock.unlock();
      e.printStackTrace();
    }
  }

  void sendNext() {
    lock.lock();
    if (!queue.isEmpty()) {
      try {
        Crash crash = queue.peek();
        lock.unlock();
        send(crash);
      } catch (IOException e) {
        lock.unlock();
        e.printStackTrace();
      }
    } else {
      lock.unlock();
    }
  }

  private void send(Crash crash) {
    lock.lock();
    if (inFlight) {
      lock.unlock();
      return;
    }
    inFlight = true;
    lock.unlock();
    sender.send(crash, new CrashSender.Callback() {
      @Override public void onSuccess() {
        lock.lock();
        inFlight = false;
        try {
          queue.remove();
          lock.unlock();
        } catch (IOException e) {
          lock.unlock();
          e.printStackTrace();
        }
        sendNext();
      }

      @Override public void onFailure() {
        lock.lock();
        inFlight = false;
        lock.unlock();
      }
    });
  }

  private static final class CrashConverter implements ObjectQueue.Converter<Crash> {
    private final JsonAdapter<Crash> adapter;

    CrashConverter(JsonAdapter<Crash> adapter) {
      this.adapter = adapter;
    }

    @Override public Crash from(byte[] bytes) throws IOException {
      return adapter.fromJson(new Buffer().write(bytes));
    }

    @Override public void toStream(Crash crash, OutputStream bytes) throws IOException {
      BufferedSink sink = Okio.buffer(Okio.sink(bytes));
      adapter.toJson(sink, crash);
      sink.close();
    }
  }
}

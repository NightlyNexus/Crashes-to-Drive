package com.nightlynexus.crashestodrive;

import com.squareup.moshi.JsonAdapter;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

final class IftttToDriveNetworkCrashSender implements CrashSender {
  private final OkHttpClient client;
  private final JsonAdapter<Crash> adapter;
  private final HttpUrl maker;
  private final MediaType mediaType = MediaType.parse("application/json; charset=UTF-8");

  IftttToDriveNetworkCrashSender(OkHttpClient client, JsonAdapter<Crash> adapter, String makerEvent,
      String makerKey) {
    this.client = client;
    this.adapter = adapter;
    this.maker = new HttpUrl.Builder().scheme("https")
        .host("maker.ifttt.com")
        .addPathSegment("trigger")
        .addPathSegment(makerEvent)
        .addPathSegment("with")
        .addPathSegment("key")
        .addPathSegment(makerKey)
        .build();
  }

  @Override public void send(Crash crash, final Callback callback) {
    Buffer sink = new Buffer();
    try {
      adapter.toJson(sink, crash);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    RequestBody body = RequestBody.create(mediaType, sink.readByteString());
    Call call = client.newCall(new Request.Builder().url(maker).post(body).build());
    call.enqueue(new okhttp3.Callback() {
      @Override public void onFailure(Call call, IOException e) {
        if (call.isCanceled()) {
          return;
        }
        callback.onFailure();
      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        response.body().close();
        if (response.isSuccessful()) {
          callback.onSuccess();
        } else {
          callback.onFailure();
        }
      }
    });
  }
}

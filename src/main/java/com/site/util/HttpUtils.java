package com.site.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class HttpUtils {
    
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();

    public String get(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
} 
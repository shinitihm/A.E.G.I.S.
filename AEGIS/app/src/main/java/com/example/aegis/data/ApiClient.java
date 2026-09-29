package com.example.aegis.data;

import com.example.aegis.BuildConfig;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Retrofit único para o app inteiro. */
public final class ApiClient {

    /** snake_case do Python ↔ camelCase do Java (real_name → realName). */
    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static AegisApi api;

    private ApiClient() {
    }

    public static synchronized AegisApi get() {
        if (api == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS) // a J.A.R.V.I.S. com IA pode demorar um pouco
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create(GSON))
                    .build()
                    .create(AegisApi.class);
        }
        return api;
    }
}

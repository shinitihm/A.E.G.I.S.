package com.example.aegis.data;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback simplificado: onSuccess com o corpo ou onError com uma mensagem pronta para a tela.
 * O Retrofit já entrega as respostas na thread principal.
 */
public abstract class ApiCallback<T> implements Callback<T> {

    public abstract void onSuccess(@NonNull T body);

    public abstract void onError(@NonNull String message);

    @Override
    public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        T body = response.body();
        if (response.isSuccessful() && body != null) {
            onSuccess(body);
        } else {
            onError(readDetail(response));
        }
    }

    @Override
    public final void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        if (!call.isCanceled()) {
            onError("SEM CONEXÃO COM O SERVIDOR A.E.G.I.S.");
        }
    }

    /** O FastAPI devolve erros como {"detail": "..."}. */
    private static String readDetail(Response<?> response) {
        try (okhttp3.ResponseBody error = response.errorBody()) {
            if (error != null) {
                JsonObject json = ApiClient.GSON.fromJson(error.string(), JsonObject.class);
                if (json != null && json.has("detail") && json.get("detail").isJsonPrimitive()) {
                    return json.get("detail").getAsString();
                }
            }
        } catch (Exception ignored) {
            // cai na mensagem genérica abaixo
        }
        return "ERRO " + response.code() + " NO SERVIDOR A.E.G.I.S.";
    }
}

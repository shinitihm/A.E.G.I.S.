package com.example.aegis.data;

import com.example.aegis.data.model.Armor;
import com.example.aegis.data.model.ChatRequest;
import com.example.aegis.data.model.ChatResponse;
import com.example.aegis.data.model.CompareResult;
import com.example.aegis.data.model.Health;
import com.example.aegis.data.model.HeroDetail;
import com.example.aegis.data.model.HeroPage;
import com.example.aegis.data.model.HeroSummary;
import com.example.aegis.data.model.Mission;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Endpoints do backend FastAPI (veja backend/app/main.py). */
public interface AegisApi {

    @GET("health")
    Call<Health> health();

    @GET("heroes")
    Call<HeroPage> heroes(@Query("page") int page);

    @GET("heroes/search")
    Call<List<HeroSummary>> search(@Query("q") String query);

    @GET("heroes/target-of-the-day")
    Call<HeroDetail> targetOfTheDay();

    @GET("heroes/{id}")
    Call<HeroDetail> hero(@Path("id") int id);

    @GET("compare")
    Call<CompareResult> compare(@Query("a") String a, @Query("b") String b);

    @GET("missions")
    Call<List<Mission>> missions();

    @GET("missions/{id}")
    Call<Mission.Detail> mission(@Path("id") int id);

    @GET("armors")
    Call<List<Armor>> armors();

    @GET("armors/{mark}")
    Call<Armor> armor(@Path("mark") int mark);

    @POST("jarvis/chat")
    Call<ChatResponse> chat(@Body ChatRequest request);
}

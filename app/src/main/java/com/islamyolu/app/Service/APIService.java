package com.islamyolu.app.Service;

import com.islamyolu.app.Entity.Question;
import com.islamyolu.app.islamicDays.IslamicDayResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface APIService {
    @GET("/yolcu-db/sual.json")
    Call<List<Question>> getAllQuestionList();

    @GET
    Call<String> getCardTextFile(@Url String fileUrl);

    @GET("/yolcu-db/importantDays.json")
    Call<List<IslamicDayResponse>> getIslamicDaysList();

}

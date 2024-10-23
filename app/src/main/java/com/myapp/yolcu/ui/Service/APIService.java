package com.myapp.yolcu.ui.Service;

import com.myapp.yolcu.ui.Entity.Question;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface APIService {
    @GET("/yolcu-db/sual.json")
    Call<List<Question>> getAllQuestionList();

}

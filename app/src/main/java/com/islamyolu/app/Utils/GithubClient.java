package com.islamyolu.app.Utils;

import com.islamyolu.app.Service.APIService;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class GithubClient {
    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(APIUrl.API_TEMP_BASE_URL) // Ana URL
            .addConverterFactory(ScalarsConverterFactory.create()) // String Converter
            .build();

    public static APIService getGitHubService() {
        return retrofit.create(APIService.class);
    }
}

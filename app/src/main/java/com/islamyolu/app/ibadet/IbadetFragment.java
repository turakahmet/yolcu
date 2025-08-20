package com.islamyolu.app.ibadet;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.Entity.Ibadet;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.APIService;
import com.islamyolu.app.Service.SoundService;
import com.islamyolu.app.Utils.APIUrl;
import com.islamyolu.app.Utils.GithubClient;
import com.islamyolu.app.databinding.FragmentIbadetBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IbadetFragment extends Fragment {
    private SoundService soundService;
    private FragmentIbadetBinding binding;
    private IbadetAdapter adapter;
    private List<Ibadet> ibadetList;
    private int cardIndex = 1;
    private RecyclerView recyclerView;
    private ProgressBar loadingIbadetFragment;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIbadetBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeSoundService(binding.getRoot());
        initializeViews(root);

        readTextFile(cardIndex);
        return root;
    }

    private void initializeViews(View root) {
        recyclerView = root.findViewById(R.id.recyclerViewIbadet);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadingIbadetFragment = root.findViewById(R.id.loadingIbadetFragment);
        ibadetList = new ArrayList<>();

    }

    private void readTextFile(int index) {
        String fileName = "card" + index + ".txt";

        APIService service = GithubClient.getGitHubService();
        Call<String> call = service.getCardTextFile(APIUrl.API_TEMP_BASE_URL + fileName);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String content = response.body();

                    // Dosya bulundu -> İşle ve bir sonrakine geç
                    parseAndDisplayContent(content);
                    cardIndex += 1;
                    readTextFile(cardIndex);
                } else {
                    // Dosya yoksa dur
                    adapter = new IbadetAdapter(getContext(), ibadetList);
                    recyclerView.setAdapter(adapter);
                    loadingIbadetFragment.setVisibility(View.GONE);
                    Log.e("INFO", "Tüm dosyalar okundu, işlem bitti.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("ERROR", "Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    private void parseAndDisplayContent(String content) {
        String imageUrl = "";
        String title = "";
        String source = "";
        StringBuilder contentBuilder = new StringBuilder();

        String[] lines = content.split("\n");

        for (String line : lines) {
            if (line.startsWith("imageUrl:")) {
                imageUrl = line.replace("imageUrl:", "").trim();
            } else if (line.startsWith("title:")) {
                title = line.replace("title:", "").trim();
            } else if (line.startsWith("source:")) {
                source = line.replace("source:", "").trim();
            } else {
                contentBuilder.append(line).append("\n");
            }
        }

        String contentText = contentBuilder.toString().trim();

        // CardView'e yükle
//        updateCardView(imageUrl, title, contentText);
        ibadetList.add(new Ibadet(title, imageUrl, contentText, source));

    }

    private void initializeSoundService(ConstraintLayout root) {
        soundService = new SoundService(root.getContext());
        soundService.commonButtonEffect();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
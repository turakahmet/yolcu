package com.myapp.yolcu.ui.ibadet;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.FragmentIbadetBinding;
import com.myapp.yolcu.ui.Service.SoundService;
import com.myapp.yolcu.ui.game.WordGameActivity;
import com.myapp.yolcu.ui.main.MainActivity;

public class IbadetFragment extends Fragment {
    private SoundService soundService;
    private FragmentIbadetBinding binding;
    private CardView cardViewIbadet_1,cardViewIbadet_2;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentIbadetBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeSoundService(binding.getRoot());
        soundService.commonButtonEffect();
        cardViewIbadet_1 =root.findViewById(R.id.cardViewIbadet_1);
        cardViewIbadet_2 =root.findViewById(R.id.cardViewIbadet_2);

        cardViewIbadet_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundService.commonButtonEffect();
                Intent intent = new Intent(getActivity(), KirkHadisActivity.class);
                startActivity(intent);
            }
        });

        cardViewIbadet_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundService.commonButtonEffect();
                Toast.makeText(root.getContext(), "cardViewIbadet_2", Toast.LENGTH_SHORT).show();

            }
        });
        return root;
    }
    private void initializeSoundService(ConstraintLayout root) {
        soundService = new SoundService(root.getContext());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package com.myapp.yolcu.ui.muze;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.FragmentMuzeBinding;
import com.myapp.yolcu.ui.Entity.Ibadet;
import com.myapp.yolcu.ui.ibadet.KirkHadisAdapter;

import java.util.List;

public class DigitalMuzeFragment extends Fragment {

    private FragmentMuzeBinding binding;
    private RecyclerView recyclerView;
    private DigitalMuzeAdapter adapter;
    private List<MuzeBilgi> muzeBilgiList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMuzeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
//        // ActionBar'ı gizle
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().hide();
//        }
        recyclerView = root.findViewById(R.id.recyclerViewMuze);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        muzeBilgiList = new MuzeBilgi().MuzeBilgiListesiniAl(requireContext());
        adapter = new DigitalMuzeAdapter(requireContext(), muzeBilgiList);
        recyclerView.setAdapter(adapter);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

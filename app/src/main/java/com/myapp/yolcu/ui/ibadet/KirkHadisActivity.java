package com.myapp.yolcu.ui.ibadet;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Entity.Ibadet;

import java.util.List;

public class KirkHadisActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private KirkHadisAdapter adapter;
    private List<Ibadet> ibadetList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hadis_kirk);
        setTitle(R.string.kirkhadis);
        recyclerView = findViewById(R.id.recyclerViewKirkHadis);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ibadetList = new Ibadet().KirkHadisListesiniAl(this);
        adapter = new KirkHadisAdapter(this, ibadetList);
        recyclerView.setAdapter(adapter);
    }
}

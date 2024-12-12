package com.myapp.yolcu.ui.ibadet;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Entity.Ibadet;
import com.myapp.yolcu.ui.Service.SoundService;

import java.util.List;

public class KirkHadisAdapter extends RecyclerView.Adapter<KirkHadisAdapter.KirkHadisViewHolder> {
    private Context context;
    private List<Ibadet> kirkHadisList;
    private SoundService soundService;

    public KirkHadisAdapter(Context context, List<Ibadet> kirkHadisList) {
        this.context = context;
        this.kirkHadisList = kirkHadisList;
        this.soundService = new SoundService(context);
    }

    @NonNull
    @Override
    public KirkHadisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_layout, parent, false);
        return new KirkHadisViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KirkHadisViewHolder holder, int position) {
        Ibadet ibadet = kirkHadisList.get(position);
        holder.baslikTextView.setText(ibadet.getBaslik());
//        holder.aciklama1TextView.setText(ibadet.getAciklama1());
//        holder.aciklama2TextView.setText(ibadet.getAciklama2());
        holder.imageView.setImageResource(ibadet.getResimKaynak()); // Örnek resim

        // CardView'e tıklama dinleyici ekleme
        holder.cardView.setOnClickListener(v -> {
            soundService.commonButtonEffect();
            Intent intent = new Intent(context, IbadetDetailActivity.class);
            intent.putExtra("baslik", ibadet.getBaslik());
            intent.putExtra("aciklama1", ibadet.getAciklama1());
            intent.putExtra("aciklama2", ibadet.getAciklama2());
            intent.putExtra("resimKaynak", ibadet.getResimKaynak());
            intent.putExtra("kaynak", ibadet.getKaynak());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return kirkHadisList.size();
    }

    public static class KirkHadisViewHolder extends RecyclerView.ViewHolder {
        private TextView baslikTextView, aciklama1TextView, aciklama2TextView;
        private ImageView imageView;
        private CardView cardView;

        public KirkHadisViewHolder(@NonNull View itemView) {
            super(itemView);
            baslikTextView = itemView.findViewById(R.id.idCardViewHeader);
//            aciklama1TextView = itemView.findViewById(R.id.idCardViewAciklama1);
//            aciklama2TextView = itemView.findViewById(R.id.idCardViewAciklama2);
            imageView = itemView.findViewById(R.id.idCardViewImage);
            cardView = itemView.findViewById(R.id.idCardView);
        }
    }
}

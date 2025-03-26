package com.islamyolu.app.muze;

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

import com.islamyolu.app.R;
import com.islamyolu.app.Service.SoundService;

import java.util.List;

public class DigitalMuzeAdapter extends RecyclerView.Adapter<DigitalMuzeAdapter.DigitalMuzelViewHolder> {
    private Context context;
    private List<MuzeBilgi> muzeBilgiList;
    private SoundService soundService;

    public DigitalMuzeAdapter(Context context, List<MuzeBilgi> muzeBilgiList) {
        this.context = context;
        this.muzeBilgiList = muzeBilgiList;
        this.soundService = new SoundService(context);
    }

    @NonNull
    @Override
    public DigitalMuzelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_layout, parent, false);
        return new DigitalMuzelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DigitalMuzelViewHolder holder, int position) {
        MuzeBilgi muzeBilgi = muzeBilgiList.get(position);
        holder.baslikTextView.setText(muzeBilgi.getBaslik());
//        holder.aciklama1TextView.setText(ibadet.getAciklama1());
//        holder.aciklama2TextView.setText(ibadet.getAciklama2());
        holder.imageView.setImageResource(muzeBilgi.getResimKaynak()); // Örnek resim

        // CardView'e tıklama dinleyici ekleme
        holder.cardView.setOnClickListener(v -> {
            soundService.commonButtonEffect();
            Intent intent = new Intent(context, MuzeDetailActivity.class);
            intent.putExtra("baslik", muzeBilgi.getBaslik());
            intent.putExtra("aciklama", muzeBilgi.getAciklama());
            intent.putExtra("kapakResim", muzeBilgi.getResimKaynak());
            intent.putIntegerArrayListExtra("gorselList", muzeBilgi.getDigerGorseller());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return muzeBilgiList.size();
    }

    public static class DigitalMuzelViewHolder extends RecyclerView.ViewHolder {
        private TextView baslikTextView, aciklama1TextView, aciklama2TextView;
        private ImageView imageView;
        private CardView cardView;

        public DigitalMuzelViewHolder(@NonNull View itemView) {
            super(itemView);
            baslikTextView = itemView.findViewById(R.id.idCardViewHeader);
//            aciklama1TextView = itemView.findViewById(R.id.idCardViewAciklama1);
//            aciklama2TextView = itemView.findViewById(R.id.idCardViewAciklama2);
            imageView = itemView.findViewById(R.id.idCardViewImage);
            cardView = itemView.findViewById(R.id.idCardView);
        }
    }
}

package com.islamyolu.app.ibadet;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.islamyolu.app.Entity.Ibadet;
import com.islamyolu.app.R;

import java.util.List;

public class IbadetAdapter extends RecyclerView.Adapter<IbadetAdapter.IbadetViewHolder> {

    private List<Ibadet> ibadetList;
    private Context context;

    public IbadetAdapter(Context context, List<Ibadet> ibadetList) {
        this.context = context;
        this.ibadetList = ibadetList;
    }

    @NonNull
    @Override
    public IbadetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ibadet_card, parent, false);
        return new IbadetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IbadetViewHolder holder, int position) {
        Ibadet ibadet = ibadetList.get(position);
        holder.title.setText(ibadet.getBaslik());

        Glide.with(context)
                .load(ibadet.getResimKaynakRemote())
                .thumbnail(Glide.with(holder.itemView.getContext()).load(R.drawable.loading))
                .error(R.drawable.baseline_image_not_supported_24) // Hata olursa gösterilecek resim
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if (position == 0) {
                intent = new Intent(context, KirkHadisActivity.class);
            } else {
                intent = new Intent(context, IbadetContentActivity.class);
                intent.putExtra("CONTENT_CONTEXT", ibadet.getAciklama1());
                intent.putExtra("CONTENT_TITLE", ibadet.getBaslik());
                intent.putExtra("CONTENT_IMAGE", ibadet.getResimKaynakRemote());
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return ibadetList.size();
    }

    public static class IbadetViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;

        public IbadetViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.cardViewIbadetLabel);
            image = itemView.findViewById(R.id.cardViewIbadetImage);
        }
    }
}

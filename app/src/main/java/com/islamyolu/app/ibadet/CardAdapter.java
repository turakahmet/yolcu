package com.islamyolu.app.ibadet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.islamyolu.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private final List<IbadetCardLayout> ibadetCardLayoutList;
    private final Context context;

    public CardAdapter(Context context, List<IbadetCardLayout> ibadetCardLayoutList) {
        this.context = context;
        this.ibadetCardLayoutList = ibadetCardLayoutList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.ibadet_arapca_content_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        IbadetCardLayout item = ibadetCardLayoutList.get(position);

        holder.baslik.setText(item.baslik);
        holder.okunus.setText(item.okunus);
        holder.aciklama.setText(item.anlam);
        // Glide kullanarak görsel yükle
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(item.imageUrl.trim())
                    .placeholder(R.drawable.baseline_menu_book_24)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_menu_book_24); // default görsel
        }


        holder.shareButton.setOnClickListener(v -> {
            // 1. LayoutInflater ile özel layout'u yükle
            LayoutInflater inflater = LayoutInflater.from(context);
            View shareView = inflater.inflate(R.layout.ibadet_share_card, new FrameLayout(context), false);

            // 2. Verileri layout içine doldur
            TextView tvBaslik = shareView.findViewById(R.id.shareBaslik);
            TextView tvAciklama = shareView.findViewById(R.id.shareAciklama);
            ImageView ivImage = shareView.findViewById(R.id.shareImage);

            IbadetCardLayout itemShare = ibadetCardLayoutList.get(holder.getAdapterPosition());

            tvBaslik.setText(itemShare.baslik);
            tvAciklama.setText(itemShare.anlam);

            // Glide ile resim yüklenirse:
            holder.imageView.setDrawingCacheEnabled(true);
            Bitmap originalBitmap = Bitmap.createBitmap(holder.imageView.getDrawingCache());
            holder.imageView.setDrawingCacheEnabled(false);

// Bunu paylaşım görünümüne uygula
            ivImage.setImageBitmap(originalBitmap);


            // 3. Ölçülmeyen view'dan bitmap al (önce ölç!)
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int width = displayMetrics.widthPixels;  // ekran genişliği
            int height = displayMetrics.heightPixels; // ekran yüksekliği

// View'i ölç ve yerleştir
            shareView.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
            );
            shareView.layout(0, 0, shareView.getMeasuredWidth(), shareView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(shareView.getMeasuredWidth(), shareView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            shareView.draw(canvas);

            // 4. Bitmap'i paylaş
            try {
                File file = new File(context.getCacheDir(), "islamyolu.png");
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();

                Uri uri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider", file);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                context.startActivity(Intent.createChooser(shareIntent, "Paylaş"));

            } catch (IOException e) {
                Log.d("CardAdapter", "Hata oluştu:" + e.getMessage());
            }
        });


    }

    @Override
    public int getItemCount() {
        return ibadetCardLayoutList.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;
        TextView baslik, okunus, aciklama;
        ImageButton shareButton;
        View cardRoot;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.idCardViewImage);
            baslik = itemView.findViewById(R.id.idCardViewBaslik);
            okunus = itemView.findViewById(R.id.idCardViewOkunus);
            aciklama = itemView.findViewById(R.id.idCardViewAciklama);
            shareButton = itemView.findViewById(R.id.ibadetCardShare);
            cardRoot = itemView.findViewById(R.id.idCardView);
        }
    }
}

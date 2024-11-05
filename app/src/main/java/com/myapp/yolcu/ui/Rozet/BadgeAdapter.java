package com.myapp.yolcu.ui.Rozet;

import static android.graphics.Color.*;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.yolcu.R;

import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> badgeList;

    public BadgeAdapter(List<Badge> badgeList) {
        this.badgeList = badgeList;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rozet_list, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badgeList.get(position);

        holder.badgeName.setText(badge.getBadgeType().getLabel());
        holder.badgeDescription.setText(badge.getBadgeType().getDescription());

        // Etkin durumuna göre ikonun rengini ayarla
        if (badge.isEnabled()) {
            holder.badgeIcon.setImageResource(badge.getBadgeType().getIconResId());
            holder.badgeIcon.setAlpha(1.0f);
            holder.badgeDescription.setAlpha(1.0f);// Gri tonlama
            holder.badgeName.setAlpha(1.0f);// Gri tonlama
            // Renkli ikon
        } else {
            holder.badgeIcon.setImageResource(badge.getBadgeType().getIconResId());
            holder.badgeIcon.setAlpha(0.2f);  // Gri tonlama
            holder.badgeDescription.setAlpha(0.5f);
            holder.badgeName.setAlpha(0.5f);
        }
        holder.bind(badge); // Bind metodunu çağır
    }

    @Override
    public int getItemCount() {
        return badgeList.size();
    }

    public static class BadgeViewHolder extends RecyclerView.ViewHolder {
        TextView badgeName;
        TextView badgeDescription;
        ImageView badgeIcon;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeName = itemView.findViewById(R.id.badgeName);
            badgeDescription = itemView.findViewById(R.id.badgeDescription);
            badgeIcon = itemView.findViewById(R.id.badgeIcon);

        }
        public void bind(Badge badge) {
            // Arka plan rengini ayarlayın
            if (badge.isEnabled()) {
                itemView.setBackgroundResource(R.drawable.rozet_bg_active); // Enable değilse şeffaf
                // Enable olduğunda arka plan kırmızı
            } else {
                itemView.setBackgroundResource(R.drawable.rozet_bg); // Enable değilse şeffaf
            }
        }
    }
}



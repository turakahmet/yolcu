package com.islamyolu.app.islamicDays;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IslamicDayAdapter extends RecyclerView.Adapter<IslamicDayAdapter.ViewHolder> {
    private final List<DiniGun> diniGunList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));

    public IslamicDayAdapter(List<DiniGun> diniGunList) {
        this.diniGunList = diniGunList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.islamic_day_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DiniGun diniGun = diniGunList.get(position);

        if (position % 2 == 0) {
            holder.itemView.setBackgroundResource(R.color.monthly_row_gray);
            holder.diniGunLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.themeTextColor_s));
            holder.diniTarihLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.themeTextColor_s));
        } else {
            holder.itemView.setBackgroundResource(R.color.monthly_row_orange);
            holder.diniGunLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
            holder.diniTarihLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
        }
        if (diniGun.isAktifGunMu()) {
            holder.itemView.setBackgroundResource(R.color.colorPrimary);
        }

        holder.diniGunLabel.setText(diniGun.getDiniGun().getAd());
        holder.diniTarihLabel.setText(diniGun.getDiniTarih());
    }

    @Override
    public int getItemCount() {
        return diniGunList.size();
    }

    public void updateData(List<DiniGun> newList) {
        this.diniGunList.clear();
        this.diniGunList.addAll(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView diniGunLabel, diniTarihLabel;

        ViewHolder(View view) {
            super(view);
            diniGunLabel = view.findViewById(R.id.diniGunLabel);
            diniTarihLabel = view.findViewById(R.id.diniTarihLabel);

        }
    }
} 
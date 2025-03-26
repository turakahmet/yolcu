package com.islamyolu.app.monthlyCalender;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.NamazVakiti;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MonthlyPrayerAdapter extends RecyclerView.Adapter<MonthlyPrayerAdapter.ViewHolder> {
    private final List<NamazVakiti> prayerTimes;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));

    public MonthlyPrayerAdapter(List<NamazVakiti> prayerTimes) {
        this.prayerTimes = prayerTimes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prayer_time_mountly, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NamazVakiti prayer = prayerTimes.get(position);

        if (position % 2 == 0) {
            holder.itemView.setBackgroundResource(R.color.monthly_row_gray);
        } else {
            holder.itemView.setBackgroundResource(R.color.monthly_row_orange);
        }

        holder.dateTextItem.setText(prayer.getMonthlyDay());
        holder.imsakTimeItem.setText(prayer.getImsak());
        holder.gunesTimeItem.setText(prayer.getGunes());
        holder.ogleTimeItem.setText(prayer.getOgle());
        holder.ikindiTimeItem.setText(prayer.getIkindi());
        holder.aksamTimeItem.setText(prayer.getAksam());
        holder.yatsiTimeItem.setText(prayer.getYatsi());
    }

    @Override
    public int getItemCount() {
        return prayerTimes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextItem, imsakTimeItem, gunesTimeItem, ogleTimeItem, ikindiTimeItem, aksamTimeItem, yatsiTimeItem;

        ViewHolder(View view) {
            super(view);
            dateTextItem = view.findViewById(R.id.date_text_item);
            imsakTimeItem = view.findViewById(R.id.imsak_time_item);
            gunesTimeItem = view.findViewById(R.id.gunes_time_item);
            ogleTimeItem = view.findViewById(R.id.ogle_time_item);
            ikindiTimeItem = view.findViewById(R.id.ikindi_time_item);
            aksamTimeItem = view.findViewById(R.id.aksam_time_item);
            yatsiTimeItem = view.findViewById(R.id.yatsi_time_item);
        }
    }
} 
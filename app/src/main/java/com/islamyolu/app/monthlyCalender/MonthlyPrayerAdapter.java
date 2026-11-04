package com.islamyolu.app.monthlyCalender;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Aylık takvim satırları: Room'daki günler, şehrin yerel saatiyle; boş vakit "—". */
public class MonthlyPrayerAdapter extends RecyclerView.Adapter<MonthlyPrayerAdapter.ViewHolder> {
    private final List<PrayerDayEntity> days;
    private final DateTimeFormatter dateFormat;

    public MonthlyPrayerAdapter(List<PrayerDayEntity> days, Locale locale) {
        this.days = days;
        this.dateFormat = DateTimeFormatter.ofPattern("d MMMM", locale);
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
        PrayerDayEntity day = days.get(position);

        if (position % 2 == 0) {
            holder.itemView.setBackgroundResource(R.color.monthly_row_gray);
            holder.dateTextItem.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.monthly_row_orange));
        } else {
            holder.itemView.setBackgroundResource(R.color.monthly_row_bg);
            holder.dateTextItem.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.monthly_row_text));
        }

        holder.dateTextItem.setText(LocalDate.parse(day.getDate()).format(dateFormat));
        holder.imsakTimeItem.setText(PrayerDisplay.time(day.getFajr()));
        holder.gunesTimeItem.setText(PrayerDisplay.time(day.getSunrise()));
        holder.ogleTimeItem.setText(PrayerDisplay.time(day.getDhuhr()));
        holder.ikindiTimeItem.setText(PrayerDisplay.time(day.getAsr()));
        holder.aksamTimeItem.setText(PrayerDisplay.time(day.getMaghrib()));
        holder.yatsiTimeItem.setText(PrayerDisplay.time(day.getIsha()));
    }

    @Override
    public int getItemCount() {
        return days.size();
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

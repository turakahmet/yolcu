package com.islamyolu.app.prayertimes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.databinding.ItemPlaceBinding;

/** Ülke / eyalet / şehir listesi. */
class PlaceAdapter extends ListAdapter<PlaceItem, PlaceAdapter.ViewHolder> {

    interface OnPlaceClickListener {
        void onPlaceClick(@NonNull PlaceItem item);
    }

    private static final DiffUtil.ItemCallback<PlaceItem> DIFF = new DiffUtil.ItemCallback<PlaceItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull PlaceItem oldItem, @NonNull PlaceItem newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlaceItem oldItem, @NonNull PlaceItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final OnPlaceClickListener listener;

    PlaceAdapter(@NonNull OnPlaceClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlaceBinding binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceItem item = getItem(position);
        holder.binding.placeName.setText(item.name);
        holder.binding.placeUnavailable.setVisibility(item.unavailable ? View.VISIBLE : View.GONE);
        holder.binding.placeName.setEnabled(!item.unavailable);
        holder.itemView.setOnClickListener(v -> listener.onPlaceClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPlaceBinding binding;

        ViewHolder(ItemPlaceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

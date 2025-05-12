package com.islamyolu.app.main;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.islamyolu.app.R;

public class ProfilePictureDialogFragment extends DialogFragment {
    public interface ProfilePictureDialogListener {
        void onProfilePictureSelected(int resourceId);
    }

    private ProfilePictureDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ProfilePictureDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement ProfilePictureDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Resim kaynakları
        final int[] imageOptions = {R.drawable.man_1, R.drawable.man_2, R.drawable.man_3, R.drawable.man_4, R.drawable.man_5,
                R.drawable.man_6, R.drawable.women_1, R.drawable.women_2, R.drawable.women_3, R.drawable.women_4, R.drawable.women_5, R.drawable.women_6};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(getString(R.string.choose_avatar))
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    // Kullanıcı seçimini yapmadıysa hiçbir şey yapma
                })
                .setNegativeButton(getString(R.string.cancel), null);

        // GridView için bir View oluştur
        GridView gridView = new GridView(getActivity());
        gridView.setNumColumns(3); // 3 sütun
        gridView.setAdapter(new ImageAdapter(getActivity(), imageOptions));

        // GridView'e tıklama olayı ekle
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            listener.onProfilePictureSelected(imageOptions[position]);
        });

        builder.setView(gridView);
        return builder.create();
    }

    // ImageAdapter sınıfı
    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private int[] images;

        public ImageAdapter(Context context, int[] images) {
            this.context = context;
            this.images = images;
        }

        @Override
        public int getCount() {
            return images.length;
        }

        @Override
        public Object getItem(int position) {
            return images[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(200, 200)); // 200dp boyut
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(images[position]);
            return imageView;
        }
    }
}

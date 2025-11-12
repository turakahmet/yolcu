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
import com.islamyolu.app.profile.Avatar;

public class ProfilePictureDialogFragment extends DialogFragment {
    public interface ProfilePictureDialogListener {
        void onProfilePictureSelected(Avatar avatar); // artık Avatar enum gönderecek
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
        // Enum dizisi
        final Avatar[] avatars = Avatar.values();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(getString(R.string.choose_avatar))
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    // Kullanıcı seçimini yapmadıysa hiçbir şey yapma
                })
                .setNegativeButton(getString(R.string.cancel), null);

        // GridView için bir View oluştur
        GridView gridView = new GridView(getActivity());
        gridView.setNumColumns(3);
        gridView.setAdapter(new ImageAdapter(getActivity(), avatars));

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            listener.onProfilePictureSelected(avatars[position]);
        });

        builder.setView(gridView);
        return builder.create();
    }

    // Adapter
    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private Avatar[] avatars;

        public ImageAdapter(Context context, Avatar[] avatars) {
            this.context = context;
            this.avatars = avatars;
        }

        @Override
        public int getCount() {
            return avatars.length;
        }

        @Override
        public Object getItem(int position) {
            return avatars[position];
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
                imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(avatars[position].getResId());
            return imageView;
        }
    }
}

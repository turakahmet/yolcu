package com.islamyolu.app.Service;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.islamyolu.app.R;

/**
 * "Konum kapalı — Konumu aç" penceresi. DialogFragment olduğu için ekran döndürme ve
 * bölünmüş ekrana geçişte kaybolmaz. Cevap {@link LocationAccess}'e Fragment Result ile iletilir.
 */
public class LocationEnableDialogFragment extends DialogFragment {

    static final String TAG = "location_enable_dialog";
    static final String RESULT_KEY = "location_enable_result";
    static final String RESULT_ACCEPTED = "accepted";

    private static final String ARG_MESSAGE = "message";
    private static final String ARG_DECLINE = "decline";

    static LocationEnableDialogFragment newInstance(@StringRes int message, @StringRes int declineLabel) {
        Bundle args = new Bundle();
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_DECLINE, declineLabel);
        LocationEnableDialogFragment fragment = new LocationEnableDialogFragment();
        fragment.setArguments(args);
        fragment.setCancelable(false);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.location_off_title)
                .setMessage(args.getInt(ARG_MESSAGE))
                .setPositiveButton(R.string.location_open_settings, (d, w) -> sendResult(true))
                .setNegativeButton(args.getInt(ARG_DECLINE), (d, w) -> sendResult(false))
                .create();
    }

    private void sendResult(boolean accepted) {
        Bundle result = new Bundle();
        result.putBoolean(RESULT_ACCEPTED, accepted);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }
}

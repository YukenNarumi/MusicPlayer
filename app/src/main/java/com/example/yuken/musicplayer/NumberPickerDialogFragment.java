package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.NumberPicker;

public class NumberPickerDialogFragment extends DialogFragment {

    private NumberPicker mNumberPicer;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_numberpicker);

        mNumberPicer = dialog.findViewById(R.id.numberPicker);
        // 設定できる上限、下限を設定する
        mNumberPicer.setMaxValue(20);
        mNumberPicer.setMinValue(0);
        // 初期値を設定する
        mNumberPicer.setValue(0);

        return dialog;
    }
}

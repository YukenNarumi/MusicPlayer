package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

public class NumberPickerDialogFragment extends DialogFragment {

    private static final int UNIT_TIME_MINTES       = 60000;
    private static final int UNIT_TIME_SECOND       = 1000;
    private static final int UNIT_TIME_MILLISECOND  = 1;

    private static final int RANGE_TIME_MINTES      = 60;
    private static final int RANGE_TIME_SECOND      = 60;
    private static final int RANGE_TIME_MILLISECOND = 1000;

    private MainActivity.LoopType loopType;
    private NumberPicker numberpickerMinute;
    private NumberPicker numberpickerSecond;
    private NumberPicker numberpickerMilliSecond;

    // 分秒ミリ秒の設定できる時間の算出
    private int ConvertMaxTime(int minTime, int maxRangeTime, int totalTime){
        if(minTime < 0 || maxRangeTime < 0 || totalTime < 0) {
            return 0;
        }
        if(totalTime < minTime) {
            return 0;
        }
        double _caluclate = Math.floor((double)totalTime / (double)minTime);
        if(_caluclate < maxRangeTime){
            return (int)_caluclate;
        }
        // 実際に設定できるのは「0」からのため「-1」する
        return maxRangeTime - 1;
    }
    private int ConvertMaxTimeToMintes(int totalTime){
        return ConvertMaxTime(UNIT_TIME_MINTES, RANGE_TIME_MINTES, totalTime);
    }
    private int ConvertMaxTimeToSecond(int totalTime){
        return ConvertMaxTime(UNIT_TIME_SECOND, RANGE_TIME_SECOND, totalTime);
    }
    private int ConvertMaxTimeToMilliSecond(int totalTime){
        return ConvertMaxTime(UNIT_TIME_MILLISECOND, RANGE_TIME_MILLISECOND, totalTime);
    }

    // 通常の分秒ミリ秒への時間の算出
    private int ConvertTime(int minTime, int maxRangeTime, int totalTime){
        if(minTime < 0 || maxRangeTime < 0 || totalTime < 0) {
            return 0;
        }
        if(totalTime < minTime) {
            return 0;
        }
        double _caluclate = Math.floor((double)totalTime / (double)minTime) % maxRangeTime;
        return (int)_caluclate;
    }
    private int ConvertTimeToMintes(int totalTime){
        return ConvertTime(UNIT_TIME_MINTES, RANGE_TIME_MINTES, totalTime);
    }
    private int ConvertTimeToSecond(int totalTime){
        return ConvertTime(UNIT_TIME_SECOND, RANGE_TIME_SECOND, totalTime);
    }
    private int ConvertTimeToMilliSecond(int totalTime){
        return ConvertTime(UNIT_TIME_MILLISECOND, RANGE_TIME_MILLISECOND, totalTime);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_numberpicker);

        numberpickerMinute = dialog.findViewById(R.id.numberPickerMinute);
        numberpickerMinute.setMinValue(0);

        numberpickerSecond = dialog.findViewById(R.id.numberPickerSeconds);
        numberpickerSecond.setMinValue(0);

        numberpickerMilliSecond = dialog.findViewById(R.id.numberPickerMilliSecond);
        numberpickerMilliSecond.setMinValue(0);


        // 元画面から値を取得
        int _musicLength = getArguments().getInt("MusicLength");
        Log.v("テスト", "ダイアログ値渡しテスト:" + ConvertMaxTimeToMintes(_musicLength) + "/" + ConvertMaxTimeToSecond(_musicLength) + "/" + ConvertMaxTimeToMilliSecond(_musicLength));
        numberpickerMinute.setMaxValue(ConvertMaxTimeToMintes(_musicLength));
        numberpickerSecond.setMaxValue(ConvertMaxTimeToSecond(_musicLength));
        numberpickerMilliSecond.setMaxValue(ConvertMaxTimeToMilliSecond(_musicLength));

        // 現在の設定時間を反映
        loopType = MainActivity.fromOrdinal(MainActivity.LoopType.class, getArguments().getInt("LoopType"));
        String _loopPointKey = (loopType == MainActivity.LoopType.START ? "LoopPointStart" : "LoopPointEnd");
        int _loopPoint = getArguments().getInt(_loopPointKey);
        Log.v("テスト", "ダイアログ値渡しテスト:" + ConvertTimeToMintes(_loopPoint) + "/" + ConvertTimeToSecond(_loopPoint) + "/" + ConvertTimeToMilliSecond(_loopPoint));
        numberpickerMinute.setValue(ConvertTimeToMintes(_loopPoint));
        numberpickerSecond.setValue(ConvertTimeToSecond(_loopPoint));
        numberpickerMilliSecond.setValue(ConvertTimeToMilliSecond(_loopPoint));

        Button setButton = dialog.findViewById(R.id.numberPickerSetBtn);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 音楽再生
                SetNumbetPickerrValue();
            }
        });

        return dialog;
    }

    //
    private void SetNumbetPickerrValue(){
        int _minute = numberpickerMinute.getValue();
        int _second = numberpickerSecond.getValue();
        int _milliSecond = numberpickerMilliSecond.getValue();
        int _total = _minute * UNIT_TIME_MINTES + _second * UNIT_TIME_SECOND + _milliSecond * UNIT_TIME_MILLISECOND;

        // TODO:このタイミングで設定できるか判定する

        Log.v("テスト", "SetNumbetPickerrValue:" + loopType);

        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.UpdateNumbetPickerr(loopType, _total);
    }
}

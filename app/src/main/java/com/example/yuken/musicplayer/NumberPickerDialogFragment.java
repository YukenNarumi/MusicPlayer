package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.NumberPicker;

public class NumberPickerDialogFragment extends DialogFragment {

    private NumberPicker numberpickerMinute;
    private NumberPicker numberpickerSecond;
    private NumberPicker numberpickerMilliSecond;

    // 分秒ミリ秒の設定できる時間の算出
    private int ConvertMaxTime(int minTime, int unitTime, int totalTime){
        if(minTime < 0 || unitTime < 0 || totalTime < 0) {
            return 0;
        }
        if(totalTime < minTime) {
            return 0;
        }
        double _caluclate = Math.floor((double)totalTime / (double)minTime);
        if(_caluclate < unitTime){
            return (int)_caluclate;
        }
        // 実際に設定できるのは「0」からのため「-1」する
        return unitTime - 1;
    }
    private int ConvertMaxTimeToMintes(int totalTime){
        return ConvertMaxTime(60000, 60, totalTime);
    }
    private int ConvertMaxTimeToSecond(int totalTime){
        return ConvertMaxTime(1000, 60, totalTime);
    }
    private int ConvertMaxTimeToMilliSecond(int totalTime){
        return ConvertMaxTime(1, 1000, totalTime);
    }

    // 通常の分秒ミリ秒への時間の算出
    private int ConvertTime(int minTime, int unitTime, int totalTime){
        if(minTime < 0 || unitTime < 0 || totalTime < 0) {
            return 0;
        }
        if(totalTime < minTime) {
            return 0;
        }
        double _caluclate = Math.floor((double)totalTime / (double)minTime) % unitTime;
        return (int)_caluclate;
    }
    private int ConvertTimeToMintes(int totalTime){
        return ConvertTime(60000, 60, totalTime);
    }
    private int ConvertTimeToSecond(int totalTime){
        return ConvertTime(1000, 60, totalTime);
    }
    private int ConvertTimeToMilliSecond(int totalTime){
        return ConvertTime(1, 1000, totalTime);
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
        ///
        // TODO:動作確認用のログ出力
        boolean testOpenStart = getArguments().getBoolean("OpenStart");
        int testLoopPointStart = getArguments().getInt("LoopPointStart");
        int testLoopPointEnd = getArguments().getInt("LoopPointEnd");
        int testMusicLength = getArguments().getInt("MusicLength");
        Log.v("テスト", "ダイアログ値渡しテスト:" + testOpenStart + "/" + testLoopPointStart + "/" + testLoopPointEnd + "/" + testMusicLength);
        ///

        int _musicLength = getArguments().getInt("MusicLength");
        Log.v("テスト", "ダイアログ値渡しテスト:" + ConvertMaxTimeToMintes(_musicLength) + "/" + ConvertMaxTimeToSecond(_musicLength) + "/" + ConvertMaxTimeToMilliSecond(_musicLength));
        numberpickerMinute.setMaxValue(ConvertMaxTimeToMintes(_musicLength));
        numberpickerSecond.setMaxValue(ConvertMaxTimeToSecond(_musicLength));
        numberpickerMilliSecond.setMaxValue(ConvertMaxTimeToMilliSecond(_musicLength));

        // 現在の設定時間を反映
        String _loopPointKey = (getArguments().getBoolean("OpenStart") ? "LoopPointStart" : "LoopPointEnd");
        int _loopPoint = getArguments().getInt(_loopPointKey);
        Log.v("テスト", "ダイアログ値渡しテスト:" + ConvertTimeToMintes(_loopPoint) + "/" + ConvertTimeToSecond(_loopPoint) + "/" + ConvertTimeToMilliSecond(_loopPoint));
        numberpickerMinute.setValue(ConvertTimeToMintes(_loopPoint));
        numberpickerSecond.setValue(ConvertTimeToSecond(_loopPoint));
        numberpickerMilliSecond.setValue(ConvertTimeToMilliSecond(_loopPoint));

        return dialog;
    }
}

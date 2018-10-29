package com.example.yuken.musicplayer;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

public class NumberPickerDialogFragment extends BaseDialogFragment {

    private static final int UNIT_TIME_MINTES      = 60000;
    private static final int UNIT_TIME_SECOND      = 1000;
    private static final int UNIT_TIME_MILLISECOND = 1;

    private static final int RANGE_TIME_MINTES      = 60;
    private static final int RANGE_TIME_SECOND      = 60;
    private static final int RANGE_TIME_MILLISECOND = 1000;

    private MainActivity.LoopType loopType;
    private NumberPicker          numberpickerMinute;
    private NumberPicker          numberpickerSecond;
    private NumberPicker          numberpickerMilliSecond;

    /**
     * 設定できる最大時間の算出
     *
     * @param minTime      「分,秒,ミリ秒」の設定に必要な最小値をミリ秒に変換した値(ms)
     * @param maxRangeTime ナンバーピッカーに設定可能な最大値
     * @param totalTime    再生時間(ms)
     * @return 算出した時間
     */
    private int ConvertMaxTime(int minTime, int maxRangeTime, int totalTime) {
        if (minTime < 0 || maxRangeTime < 0 || totalTime < 0) {
            return 0;
        }
        if (totalTime < minTime) {
            return 0;
        }
        double _caluclate = Math.floor((double)totalTime / (double)minTime);
        if (_caluclate < maxRangeTime) {
            return (int)_caluclate;
        }
        // 実際に設定できるのは「0」からのため「-1」する
        return maxRangeTime - 1;
    }

    /**
     * 「分」の設定できる最大時間の算出
     *
     * @param totalTime 再生時間(ms)
     * @return 設定可能な時間(m)
     */
    private int ConvertMaxTimeToMintes(int totalTime) {
        return ConvertMaxTime(UNIT_TIME_MINTES, RANGE_TIME_MINTES, totalTime);
    }

    /**
     * 「秒」の設定できる最大時間の算出
     *
     * @param totalTime 再生時間(ms)
     * @return 設定可能な時間(s)
     */
    private int ConvertMaxTimeToSecond(int totalTime) {
        return ConvertMaxTime(UNIT_TIME_SECOND, RANGE_TIME_SECOND, totalTime);
    }

    /**
     * 「ミリ秒」の設定できる最大時間の算出
     *
     * @param totalTime 再生時間(ms)
     * @return 設定可能な時間(ms)
     */
    private int ConvertMaxTimeToMilliSecond(int totalTime) {
        return ConvertMaxTime(UNIT_TIME_MILLISECOND, RANGE_TIME_MILLISECOND, totalTime);
    }

    /**
     * 現在の時間の算出
     *
     * @param minTime      「分,秒,ミリ秒」の設定に必要な最小値をミリ秒に変換した値(ms)
     * @param maxRangeTime ナンバーピッカーに設定可能な最大値
     * @param currentTime  現在時刻(ms)
     * @return 算出した時間
     */
    private int ConvertTime(int minTime, int maxRangeTime, int currentTime) {
        if (minTime < 0 || maxRangeTime < 0 || currentTime < 0) {
            return 0;
        }
        if (currentTime < minTime) {
            return 0;
        }
        double _caluclate = Math.floor((double)currentTime / (double)minTime) % maxRangeTime;
        return (int)_caluclate;
    }

    /**
     * 「分」の現在の時間の算出
     *
     * @param currentTime 現在時刻(ms)
     * @return 現在時刻(m)
     */
    private int ConvertTimeToMintes(int currentTime) {
        return ConvertTime(UNIT_TIME_MINTES, RANGE_TIME_MINTES, currentTime);
    }

    /**
     * 「秒」の現在の時間の算出
     *
     * @param currentTime 現在時刻(ms)
     * @return 現在時刻(s)
     */
    private int ConvertTimeToSecond(int currentTime) {
        return ConvertTime(UNIT_TIME_SECOND, RANGE_TIME_SECOND, currentTime);
    }

    /**
     * 「ミリ秒」の現在の時間の算出
     *
     * @param currentTime 現在時刻(ms)
     * @return 現在時刻(ms)
     */
    private int ConvertTimeToMilliSecond(int currentTime) {
        return ConvertTime(UNIT_TIME_MILLISECOND, RANGE_TIME_MILLISECOND, currentTime);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivityNonNull());
        dialog.setContentView(R.layout.dialog_numberpicker);

        numberpickerMinute = dialog.findViewById(R.id.numberPickerMinute);
        numberpickerMinute.setMinValue(0);

        numberpickerSecond = dialog.findViewById(R.id.numberPickerSeconds);
        numberpickerSecond.setMinValue(0);

        numberpickerMilliSecond = dialog.findViewById(R.id.numberPickerMilliSecond);
        numberpickerMilliSecond.setMinValue(0);


        // 元画面から値を取得
        int _musicLength = getArgumentsNonNull().getInt("MusicLength");
        Log.v("テスト",
              "ダイアログ値渡しテスト:" + ConvertMaxTimeToMintes(_musicLength) + "/" + ConvertMaxTimeToSecond(
                  _musicLength) + "/" + ConvertMaxTimeToMilliSecond(_musicLength)
        );
        numberpickerMinute.setMaxValue(ConvertMaxTimeToMintes(_musicLength));
        numberpickerSecond.setMaxValue(ConvertMaxTimeToSecond(_musicLength));
        numberpickerMilliSecond.setMaxValue(ConvertMaxTimeToMilliSecond(_musicLength));

        // 現在の設定時間を反映
        loopType = MainActivity.fromOrdinal(MainActivity.LoopType.class,
                                            getArgumentsNonNull().getInt("LoopType")
        );
        String _loopPointKey = (loopType == MainActivity.LoopType.START
                                ? "LoopPointStart"
                                : "LoopPointEnd");
        int _loopPoint = getArgumentsNonNull().getInt(_loopPointKey);
        Log.v("テスト",
              "ダイアログ値渡しテスト:" + ConvertTimeToMintes(_loopPoint) + "/" + ConvertTimeToSecond(
                  _loopPoint) + "/" + ConvertTimeToMilliSecond(_loopPoint)
        );
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

    /**
     * ナンバーピッカーで設定した値を反映する
     */
    private void SetNumbetPickerrValue() {
        int _minute      = numberpickerMinute.getValue();
        int _second      = numberpickerSecond.getValue();
        int _milliSecond = numberpickerMilliSecond.getValue();
        int _total       = _minute * UNIT_TIME_MINTES + _second * UNIT_TIME_SECOND + _milliSecond * UNIT_TIME_MILLISECOND;

        // TODO:このタイミングで設定できるか判定する

        Log.v("テスト", "SetNumbetPickerrValue:" + loopType);

        MainActivity mainActivity = (MainActivity)getActivityNonNull();
        mainActivity.UpdateNumbetPickerr(loopType, _total);
    }
}

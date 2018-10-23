package com.example.yuken.musicplayer;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity implements Runnable {

    private static final String PREFERENCES_TITLE    = "LoopSettingBGM";
    // リクエストコード:
    private static final int    REQUEST_PERMISSION   = 1000;
    // ボタン連打対策:ボタンタップ後にタップできない間隔(ms)
    private static final int    CLICK_EVENT_INTERVAL = 500;
    // ループポイント設定に必要最低限のBGM長(ms)
    private static final int    MUSIC_LENGTH_MIN     = 10000;
    // ループポイントの始点・終点設定に必要な間隔(ms)
    private static final int    LOOP_POINT_INTERVAL  = 5000;
    // 更新処理の間隔(ms)
    private static final int    updateIntarval       = 5;
    // ループポイント(終点)の受付猶予(ms)
    private static final int    receptionEndPoint    = 30;

    private static long clickTime = 0;

    public enum LoopType {
        START,
        END
    }

    private enum SeekBarType {
        NOW_TIME,
        LOOP_POINT_START,
        LOOP_POINT_END,
        END
    }

    private enum ButtonType {
        LOAD,
        START,
        STOP,
        LOOP_CHECKING,
        NUMBER_PICKER_START,
        NUMBER_PICKER_END,
        END
    }

    /**
     * ordinal から指定した Enum の要素に変換する汎用関数
     */
    public static <E extends Enum<E>> E fromOrdinal(Class<E> enumClass, int ordinal) {
        E[] enumArray = enumClass.getEnumConstants();
        return enumArray[ordinal];
    }

    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.SS", Locale.JAPAN);

    private Handler       m_handler;
    private MediaPlayer   mediaPlayer;
    private MediaPlayer[] arrayMediaPlayer;

    private TextView nowTimeText;
    private TextView loopPointStartText;
    private TextView loopPointEndText;

    private Map<SeekBarType, SeekBar> seekBarMap;

    private Map<ButtonType, Button> buttonMap;

    //
    private int  loopPointStart    = 0; // ms
    private int  loopPointEnd      = 0; // ms
    private long preTime           = 0;
    private int  musicLength       = 0;
    private int  playTime          = 0;    // 現在の再生時刻
    private int  playNumber        = 0;    // 再生中のメディアプレイヤー番号
    private int  prevProgressStart = 0;
    private int  prevProgressEnd   = 0;

    private TrackDialogFragment        trackDialogFragment;
    private NumberPickerDialogFragment numberpickerDialogFragment;

    private boolean numberpickerUpdate = false;
    private boolean loadCompletedBGM   = false;
    private boolean operationTimeBar   = false;
    private boolean permissionGranted  = false;
    private boolean loopChecking       = false;
    private boolean loopCheckingAfter  = false;

    private List<Boolean> prevSeekBarEnabled;
    private List<Boolean> prevButtonEnabled;

    private String BGMTitle;
    ///

    /**
     * メディアプレイヤーが設定済みか
     *
     * @return true:未設定がある / false:設定済み
     */
    private boolean IsNotMediaPlayer() {
        if (arrayMediaPlayer == null) {
            return true;
        }
        for (MediaPlayer _media : arrayMediaPlayer) {
            if (_media != null) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 再生中のメディアプレイヤーがあるか
     *
     * @return true:再生中 / false:未再生
     */
    private boolean IsPlayingMediaPlayer() {
        if (IsNotMediaPlayer()) {
            return false;
        }

        for (MediaPlayer _media : arrayMediaPlayer) {
            if (_media.isPlaying()) {
                return true;
            }
        }
        return false;
    }

    /**
     * シークバーの位置(メモリ)から時間に変換する
     *
     * @param progressValue 現在のメモリの値
     * @param progressMax   シークバーのメモリの最大値
     * @param musicLength   曲の再生時間
     * @return 現在の再生時間
     */
    private int CalculateProgressToTime(int progressValue, int progressMax, int musicLength) {
        if (progressValue <= 0 || progressMax <= 0 || musicLength <= 0) {
            Log.v("テスト", "[CalculateProgressToTime] = 0");
            return 0;
        }

        if (progressMax <= progressValue) {
            Log.v("テスト", "[CalculateProgressToTime] = " + musicLength + " / " + progressMax);
            return musicLength;
        }

        double _percent   = (double)progressValue / (double)progressMax;
        double _calculate = (double)musicLength * _percent;
        Log.v("テスト",
              "[CalculateProgressToTime:" + _calculate + " = " + musicLength + " * (" + progressValue + " / " + progressMax + ")"
        );
        return (int)_calculate;
    }

    /**
     * 時間からシークバーの位置(メモリ)に変換する
     *
     * @param musicValue  現在の再生時間
     * @param musicLength 曲の再生時間
     * @param progressMax シークバーのメモリの最大値
     * @return シークバーのメモリの値
     */
    private int CalculateTimeToProgress(int musicValue, int musicLength, int progressMax) {
        if (musicValue <= 0 || progressMax <= 0 || musicLength <= 0) {
            Log.v("テスト", "[CalculateTimeToProgress] = 0");
            return 0;
        }

        if (musicLength <= musicValue) {
            Log.v("テスト", "[CalculateTimeToProgress] = " + musicLength + " / " + progressMax);
            return progressMax;
        }

        double _percent   = (double)musicValue / (double)musicLength;
        double _calculate = (double)progressMax * _percent;
        Log.v("テスト",
              "[CalculateTimeToProgress:" + _calculate + " = " + musicLength + " * (" + musicValue + " / " + progressMax + ")"
        );
        return (int)_calculate;
    }

    /**
     * 再生時間等のクリア
     */
    public void ClearMediaPlayerInfo() {
        if (IsNotMediaPlayer()) {
            Toast.makeText(getApplication(),
                           "Error: Call timing is incorrect [ClearMediaPlayerInfo()]",
                           Toast.LENGTH_SHORT
            ).show();
            return;
        }

        musicLength = arrayMediaPlayer[0].getDuration();
        loopPointStart = 0;
        loopPointEnd = musicLength;
        preTime = 0;
        playTime = 0;
        playNumber = 0;
        prevProgressStart = 0;
        prevProgressEnd = 0;
        numberpickerUpdate = false;

        UpdatePrevLoopPoint();
        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * 前回のループポイントを更新
     */
    private void UpdatePrevLoopPoint() {
        prevProgressStart = loopPointStart;
        prevProgressEnd = loopPointEnd;
    }

    /**
     * ループポイントのテキスト更新
     */
    private void UpdateLoopPointText() {
        if (loopPointStartText == null || loopPointEndText == null) {
            return;
        }

        loopPointStartText.setText(dataFormat.format(loopPointStart));
        loopPointEndText.setText(dataFormat.format(loopPointEnd));
    }

    /**
     * シークバー位置をループポイントに対応させる
     */
    private void UpdateLoopPointSeekbar() {
        if (seekBarMap.get(SeekBarType.LOOP_POINT_START) == null || seekBarMap.get(SeekBarType.LOOP_POINT_END) == null) {
            return;
        }

        SeekBar loopPointStartSeekBar = seekBarMap.get(SeekBarType.LOOP_POINT_START);
        SeekBar loopPointEndSeekBar   = seekBarMap.get(SeekBarType.LOOP_POINT_END);

        int _max = loopPointStartSeekBar.getMax();
        loopPointStartSeekBar.setProgress(CalculateTimeToProgress(loopPointStart,
                                                                  musicLength,
                                                                  _max
        ));
        loopPointEndSeekBar.setProgress(CalculateTimeToProgress(loopPointEnd, musicLength, _max));
    }

    /**
     * 現在の時間更新
     */
    private void UpdateNowTime() {
        if (nowTimeText == null || seekBarMap.get(SeekBarType.NOW_TIME) == null) {
            return;
        }

        // ループ確認中の場合シークバー操作を禁止する
        SeekBar nowTimeSeekBar = seekBarMap.get(SeekBarType.NOW_TIME);
        if (!IsLoopChecking()) {
            nowTimeSeekBar.setEnabled(loadCompletedBGM);
        }

        int _max = nowTimeSeekBar.getMax();
        if (IsNotMediaPlayer()) {
            String _text = dataFormat.format(0) + "/" + dataFormat.format(musicLength);
            nowTimeText.setText(_text);
            nowTimeSeekBar.setProgress(CalculateTimeToProgress(0, musicLength, _max));
            return;
        }

        int _nowPosition = arrayMediaPlayer[playNumber].getCurrentPosition();

        if (operationTimeBar) {
            int _now = CalculateProgressToTime(nowTimeSeekBar.getProgress(),
                                               nowTimeSeekBar.getMax(),
                                               musicLength
            );
            String _text = dataFormat.format(_now) + "/" + dataFormat.format(musicLength);
            nowTimeText.setText(_text);
            return;
        }

        String _text = dataFormat.format(_nowPosition) + "/" + dataFormat.format(musicLength);
        nowTimeText.setText(_text);
        nowTimeSeekBar.setProgress(CalculateTimeToProgress(_nowPosition, musicLength, _max));
    }

    /**
     * メディアプレイヤーをすべて開放
     */
    private void ReleaseMediaPlayer() {
        if (arrayMediaPlayer == null) {
            return;
        }
        for (int i = 0; i < arrayMediaPlayer.length; i++) {
            if (arrayMediaPlayer[i] == null) {
                continue;
            }

            if (arrayMediaPlayer[i].isPlaying()) {
                arrayMediaPlayer[i].stop();
            }
            arrayMediaPlayer[i].stop();
            arrayMediaPlayer[i].reset();
            arrayMediaPlayer[i].release();
            arrayMediaPlayer[i] = null;
        }
        arrayMediaPlayer = null;
    }

    /**
     * permissionの確認
     */
    public void checkPermission() {
        // Android 6, API 23以上でパーミッシンの確認
        if (Build.VERSION.SDK_INT < 23) {
            permissionGranted = true;
            return;
        }

        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this,
                                               Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
        }
        // 拒否していた場合
        else {
            permissionGranted = false;
            requestLocationPermission();
        }
    }

    /**
     * permissionのアクセス許可を求める
     */
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )) {
            ActivityCompat.requestPermissions(MainActivity.this,
                                              new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                              REQUEST_PERMISSION
            );
        }
        else {
            Toast toast = Toast.makeText(this, "許可してください", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,
                                              new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                                              REQUEST_PERMISSION
            );
        }
    }

    /**
     * ボタン連打対策
     *
     * @return true:タップ制限中 / false:タップ可能
     */
    public static boolean IsNotClickEvent() {
        long time = System.currentTimeMillis();
        if (time - clickTime < CLICK_EVENT_INTERVAL) {
            return true;
        }
        clickTime = time;
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_handler = new Handler();
        m_handler.postDelayed(this, updateIntarval);
        setContentView(R.layout.activity_main);

        seekBarMap = new HashMap<SeekBarType, SeekBar>();
        buttonMap = new HashMap<ButtonType, Button>();

        // permissionの確認
        checkPermission();

        UpdatePrevLoopPoint();

        // ループポイントの時間
        nowTimeText = findViewById(R.id.textNowTime);
        loopPointStartText = findViewById(R.id.loopPointStart);
        loopPointEndText = findViewById(R.id.loopPointEnd);
        UpdateLoopPointText();

        SeekBar nowTimeSeekBar = findViewById(R.id.seekBarNowTime);
        nowTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                operationTimeBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                operationTimeBar = false;
                // 現在時刻を更新
                SeekBar nowTimeSeekBar = seekBarMap.get(SeekBarType.NOW_TIME);
                int _nowTime = CalculateProgressToTime(nowTimeSeekBar.getProgress(),
                                                       nowTimeSeekBar.getMax(),
                                                       musicLength
                );
                arrayMediaPlayer[playNumber].seekTo(_nowTime);
                playTime = _nowTime;
            }
        });
        seekBarMap.put(SeekBarType.NOW_TIME, nowTimeSeekBar);

        // ループポイントの時間
        SeekBar loopPointStartSeekBar = findViewById(R.id.seekBarStart);
        loopPointStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // ナンバーピッカーから設定した直後は処理しない
                if (numberpickerUpdate) {
                    return;
                }

                UpdateSeekbar(LoopType.START);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                UpdateSeekbar(LoopType.START);
            }
        });
        seekBarMap.put(SeekBarType.LOOP_POINT_START, loopPointStartSeekBar);

        SeekBar loopPointEndSeekBar = findViewById(R.id.seekBarEnd);
        loopPointEndSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // ナンバーピッカーから設定した直後は処理しない
                if (numberpickerUpdate) {
                    return;
                }

                UpdateSeekbar(LoopType.END);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                UpdateSeekbar(LoopType.END);
            }
        });
        seekBarMap.put(SeekBarType.LOOP_POINT_END, loopPointEndSeekBar);
        //

        // 曲選択ダイアログ表示ボタン
        trackDialogFragment = new TrackDialogFragment();
        Button buttonLoad = findViewById(R.id.loadButton);
        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if (trackDialogFragment.isResumed()) {
                    return;
                }
                if (MainActivity.IsNotClickEvent()) {
                    return;
                }
                if (!permissionGranted) {
                    return;
                }
                trackDialogFragment.show(getSupportFragmentManager(),
                                         NumberPickerDialogFragment.class.getSimpleName()
                );
            }
        });
        buttonMap.put(ButtonType.LOAD, buttonLoad);
        ///

        // 音楽開始ボタン
        Button buttonStart = findViewById(R.id.start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.IsNotClickEvent()) {
                    return;
                }
                // 音楽再生
                audioPlay();
            }
        });
        buttonMap.put(ButtonType.START, buttonStart);

        // 音楽停止ボタン
        Button buttonStop = findViewById(R.id.stop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.IsNotClickEvent()) {
                    return;
                }
                // ループ確認中の場合ループ確認を停止する
                if (IsLoopChecking()) {
                    TerminateLoopChecking();
                    return;
                }

                // 音楽停止
                audioStop();
            }
        });
        buttonMap.put(ButtonType.STOP, buttonStop);

        // ループテストボタン
        Button buttonLoopChecking = findViewById(R.id.loopTest);
        buttonLoopChecking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.IsNotClickEvent()) {
                    return;
                }
                SetupLoopChecking();
            }
        });
        buttonMap.put(ButtonType.LOOP_CHECKING, buttonLoopChecking);

        // DialogFragment表示をボタンに登録
        numberpickerDialogFragment = new NumberPickerDialogFragment();
        Button buttonNumberPickerStart = findViewById(R.id.btnLoopPointStart);
        buttonNumberPickerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numberpickerDialogFragment.isResumed()) {
                    return;
                }
                if (MainActivity.IsNotClickEvent()) {
                    return;
                }
                // ダイアログに値を渡す
                Bundle bundle = new Bundle();
                bundle.putInt("LoopType", LoopType.START.ordinal());
                bundle.putInt("LoopPointStart", loopPointStart);
                bundle.putInt("LoopPointEnd", loopPointEnd);
                bundle.putInt("MusicLength", musicLength);
                numberpickerDialogFragment.setArguments(bundle);

                numberpickerDialogFragment.show(getSupportFragmentManager(),
                                                NumberPickerDialogFragment.class.getSimpleName()
                );
            }
        });
        buttonMap.put(ButtonType.NUMBER_PICKER_START, buttonNumberPickerStart);

        // ループポイント[End]
        Button buttonNumberPickerEnd = findViewById(R.id.btnLoopPointEnd);
        buttonNumberPickerEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numberpickerDialogFragment.isResumed()) {
                    return;
                }
                if (MainActivity.IsNotClickEvent()) {
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("LoopType", LoopType.END.ordinal());
                bundle.putInt("LoopPointStart", loopPointStart);
                bundle.putInt("LoopPointEnd", loopPointEnd);
                bundle.putInt("MusicLength", musicLength);
                numberpickerDialogFragment.setArguments(bundle);

                numberpickerDialogFragment.show(getSupportFragmentManager(),
                                                NumberPickerDialogFragment.class.getSimpleName()
                );
            }
        });
        buttonMap.put(ButtonType.NUMBER_PICKER_END, buttonNumberPickerEnd);
    }

    /**
     * permissionのアクセス許可の結果受け取り
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != REQUEST_PERMISSION) {
            return;
        }

        // 使用が許可された
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
        }
        else {
            permissionGranted = false;
            // それでも拒否された時の対応
            Toast toast = Toast.makeText(this, "何もできません", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * 周期的に呼ばれる更新処理
     */
    @Override
    public void run() {

        // ここで毎フレームの処理
//        Log.v("テスト", "これはメッセージです「～」終わり");
/*
        if (mediaPlayer == null) {
            m_handler.postDelayed(this, this.updateIntarval);
            return;
        }
*/
        UpdateNowTime();

        if (!IsPlayingMediaPlayer()) {
            m_handler.postDelayed(this, updateIntarval);
            return;
        }

        numberpickerUpdate = false;

        this.playTime += System.currentTimeMillis() - this.preTime;

        UpdateLoopChecking();

        if ((this.loopPointEnd - receptionEndPoint) <= this.playTime) {
            this.playTime = this.loopPointStart;
            ///
            this.arrayMediaPlayer[this.playNumber].seekTo(this.loopPointStart);
            this.arrayMediaPlayer[this.playNumber].pause();

            int _nextID = (this.playNumber + 1) % this.arrayMediaPlayer.length;
            this.arrayMediaPlayer[_nextID].start();
            this.playNumber = _nextID;
            ///

            if (IsLoopChecking()) {
                loopChecking = false;
                loopCheckingAfter = true;
            }
        }
        this.preTime = System.currentTimeMillis();

        m_handler.postDelayed(this, updateIntarval);
    }

    /**
     * ナンバーピッカーに設定した値で更新
     *
     * @param dialogType 表示中のナンバーピッカー(スタート/エンド)
     * @param loopPoint  ループポイント
     */
    public void UpdateNumbetPickerr(LoopType dialogType, int loopPoint) {
        numberpickerUpdate = true;

        UpdatePrevLoopPoint();

        if (dialogType == LoopType.START) {
            this.loopPointStart = loopPoint;
        }
        else if (dialogType == LoopType.END) {
            this.loopPointEnd = loopPoint;
        }

        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * シークバー操作でループポイント更新
     */
    private void UpdateSeekbar(LoopType loopType) {
        if (musicLength <= MUSIC_LENGTH_MIN) {
            return;
        }

        SeekBar loopPointStartSeekBar = seekBarMap.get(SeekBarType.LOOP_POINT_START);
        SeekBar loopPointEndSeekBar   = seekBarMap.get(SeekBarType.LOOP_POINT_END);

        int _max        = loopPointStartSeekBar.getMax();
        int _nowStart   = loopPointStartSeekBar.getProgress();
        int _nowEnd     = loopPointEndSeekBar.getProgress();
        int _difference = CalculateProgressToTime(Math.abs(_nowEnd - _nowStart), _max, musicLength);

        Log.v("テスト",
              "[difference" + "(" + (_difference <= LOOP_POINT_INTERVAL) + "):" + _difference + " = " + _nowEnd + " - " + _nowStart + "][start:" + prevProgressStart + ">>" + loopPointStart + "][end:" + prevProgressEnd + ">>" + loopPointEnd + "]"
        );

        // ループポイントの始点・終点の間隔が短すぎる場合前回の値に戻す
        if (_difference <= LOOP_POINT_INTERVAL) {
            switch (loopType) {
            case START:
                loopPointStart = prevProgressStart;
                break;
            case END:
                loopPointEnd = prevProgressEnd;
                break;
            }
            UpdateLoopPointSeekbar();
        }
        else {
            UpdatePrevLoopPoint();
            switch (loopType) {
            case START:
                loopPointStart = CalculateProgressToTime(_nowStart, _max, musicLength);
                break;
            case END:
                loopPointEnd = CalculateProgressToTime(_nowEnd, _max, musicLength);
                break;
            }
        }

        UpdateLoopPointText();
    }

    /**
     * BGMを実際にロードする
     *
     * @param uri 音楽ファイルのパス
     * @return 成否
     */
    public boolean LoadBGM(Uri uri) {
        loadCompletedBGM = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
            return false;
        }

        ReleaseMediaPlayer();

        // インタンスを生成
        arrayMediaPlayer = new MediaPlayer[]{
            new MediaPlayer(),
            new MediaPlayer()
        };

        // URIから音楽ファイルを読み込む
        try {
            Log.v("テスト", "[LoadBGM:try]");
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // MediaPlayerに読み込む音楽ファイルを指定
            for (MediaPlayer anArrayMediaPlayer : arrayMediaPlayer) {
                anArrayMediaPlayer.setLooping(true);
                anArrayMediaPlayer.setDataSource(getApplicationContext(), uri);
                anArrayMediaPlayer.prepare();
            }
            loadCompletedBGM = true;
        }
        catch (IOException e1) {
            Log.v("テスト", "[LoadBGM:catch]");
            ReleaseMediaPlayer();
            e1.printStackTrace();
            return false;
        }
        Log.v("テスト", "[LoadBGM:success]");
        return true;
    }

    /**
     * BGM再生開始
     */
    private void audioPlay() {
        if (!loadCompletedBGM) {
            Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (IsPlayingMediaPlayer()) {
            Toast.makeText(getApplication(),
                           "Error: playing MediaPlayer.",
                           Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // 再生する
        this.arrayMediaPlayer[this.playNumber].start();

        for (MediaPlayer _media : arrayMediaPlayer) {
            if (_media == this.arrayMediaPlayer[this.playNumber]) {
                continue;
            }

            _media.seekTo(this.loopPointStart);
        }

        this.preTime = System.currentTimeMillis();

        SaveLoopPointDate(BGMTitle);

        // 終了を検知するリスナー
/*
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
            Log.d("debug","end of audio");
            audioStop();
            }
        });
*/
        this.arrayMediaPlayer[this.playNumber].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("debug", "end of audio");
                audioStop();
            }
        });

        SetupAudioPlay();
    }

    /**
     * 通常のBGM再生
     * ボタン、シークバーの操作制限を行う
     */
    private void SetupAudioPlay() {
        if (IsLoopChecking()) {
            return;
        }

        for (SeekBarType key : seekBarMap.keySet()) {
            if (key == SeekBarType.NOW_TIME) {
                continue;
            }
            SeekBar _seekBar = seekBarMap.get(key);
            _seekBar.setEnabled(false);
        }

        // ボタンの操作禁止
        for (ButtonType key : buttonMap.keySet()) {
            if (key == ButtonType.STOP) {
                continue;
            }
            Button btn = buttonMap.get(key);
            btn.setClickable(false);
        }
    }

    /**
     * BGM停止
     */
    private void audioStop() {
        if (!loadCompletedBGM) {
            Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!IsPlayingMediaPlayer()) {
            return;
        }

        this.arrayMediaPlayer[this.playNumber].pause();

        TeardownAudioPlay();
    }

    /**
     * 通常再生時のBGM停止
     * タン、シークバーの操作制限を行う
     */
    private void TeardownAudioPlay() {
        if (IsLoopChecking()) {
            return;
        }

        for (SeekBarType key : seekBarMap.keySet()) {
            SeekBar _seekBar = seekBarMap.get(key);
            _seekBar.setEnabled(true);
        }

        // ボタンの操作禁止
        for (ButtonType key : buttonMap.keySet()) {
            Button btn = buttonMap.get(key);
            btn.setClickable(true);
        }
    }

    /**
     * ループ確認中
     *
     * @return true:実行中 / false:停止中
     */
    private boolean IsLoopChecking() {
        return (loopChecking || loopCheckingAfter);
    }

    /**
     * ループ確認用の初期化
     */
    private void SetupLoopChecking() {
        if (!loadCompletedBGM) {
            return;
        }

        loopChecking = true;
        loopCheckingAfter = false;

        // シークバーの操作禁止
        prevSeekBarEnabled = new ArrayList<Boolean>();
        for (SeekBarType key : seekBarMap.keySet()) {
            SeekBar _seekBar = seekBarMap.get(key);
            prevSeekBarEnabled.add(_seekBar.isEnabled());
            _seekBar.setEnabled(false);
        }

        // ボタンの操作禁止
        prevButtonEnabled = new ArrayList<Boolean>();
        for (ButtonType key : buttonMap.keySet()) {
            Button btn = buttonMap.get(key);
            prevButtonEnabled.add(btn.isEnabled());
            if (key == ButtonType.STOP) {
                continue;
            }
            btn.setClickable(false);
        }

        int _testStart = this.loopPointEnd - LOOP_POINT_INTERVAL;
        if (_testStart < 0) {
            _testStart = 0;
        }
        arrayMediaPlayer[this.playNumber].seekTo(_testStart);
        playTime = _testStart;

        audioPlay();
    }

    /**
     * ループ確認中の操作制御等の更新
     */
    private void UpdateLoopChecking() {
        if (!loopCheckingAfter) {
            return;
        }

        if (this.playTime < (this.loopPointStart + LOOP_POINT_INTERVAL)) {
            return;
        }

        TerminateLoopChecking();
    }

    /**
     * ループ確認の終了処理
     */
    private void TerminateLoopChecking() {
        this.arrayMediaPlayer[this.playNumber].pause();

        // シークバーの操作禁止解除
        int _index = 0;
        for (SeekBarType key : seekBarMap.keySet()) {
            SeekBar _seekBar = seekBarMap.get(key);
            _seekBar.setEnabled(prevSeekBarEnabled.get(_index));
        }
        while (prevSeekBarEnabled.remove((Integer)2)) {
        }
        prevSeekBarEnabled = null;

        // ボタンの操作禁止解除
        _index = 0;
        for (ButtonType key : buttonMap.keySet()) {
            Button btn = buttonMap.get(key);
            btn.setClickable(prevButtonEnabled.get(_index));
            _index++;
        }
        while (prevButtonEnabled.remove((Integer)2)) {
        }
        prevButtonEnabled = null;

        loopChecking = false;
        loopCheckingAfter = false;
    }

    /**
     * @param title BGM名
     */
    private void SaveLoopPointDate(String title) {
        SharedPreferences        pref = getSharedPreferences(PREFERENCES_TITLE, MODE_PRIVATE);
        SharedPreferences.Editor e    = pref.edit();
        e.putInt(title + "_Start", loopPointStart);
        e.putInt(title + "_End", loopPointEnd);
        e.commit();
    }

    /**
     * ループポイントが保存されていれば読み込む
     *
     * @param title BGM名
     */
    public void LoadLoopPointDate(String title) {
        Log.v("テスト", "[LoadLoopPointDate:" + title + "]");

        BGMTitle = title;

        Boolean           _load = false;
        SharedPreferences pref  = getSharedPreferences(PREFERENCES_TITLE, MODE_PRIVATE);

        int _loopPointStart = pref.getInt(title + "_Start", 0);
        if (0 <= _loopPointStart) {
            loopPointStart = _loopPointStart;
            _load = true;
        }

        int _loopPointEnd = pref.getInt(title + "_End", musicLength);
        if (0 <= _loopPointEnd) {
            loopPointEnd = _loopPointEnd;
            _load = true;
        }

        // ループポイントが保存されていなければ終了
        if (!_load) {
            return;
        }
        UpdatePrevLoopPoint();
        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }
}

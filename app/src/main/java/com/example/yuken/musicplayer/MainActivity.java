package com.example.yuken.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.edmodo.rangebar.RangeBar;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

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

    private static final String PREFERENCES_TITLE         = "LoopSettingBGM";
    // リクエストコード:
    private static final int    REQUEST_PERMISSION        = 1000;
    // ボタン連打対策:ボタンタップ後にタップできない間隔(ms)
    private static final int    CLICK_EVENT_INTERVAL      = 500;
    // ループポイント設定に必要最低限のBGM長(ms)
    private static final int    MUSIC_LENGTH_MIN          = 10000;
    // ループポイントの始点・終点設定に必要な間隔(ms)
    private static final int    LOOP_POINT_INTERVAL       = 5000;
    // ループ確認用の再生時間(ms)
    private static final int    LOOP_POINT_CHECK_INTERVAL = 3000;
    // 更新処理の間隔(ms)
    private static final int    updateIntarval            = 5;
    // ループポイント(終点)の受付猶予(ms)
    private static final int    receptionEndPoint         = 30;
    // シークバー関連の最大値
    private static final int    MAX_SEEKBAR_COUNT         = 1000;
    // rangebarで実際に設定できる範囲は[0 ～ max - 1]のため
    private static final int    CALCULATE_SEEKBAR_COUNT   = MAX_SEEKBAR_COUNT - 1;

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
        NONE,
        LOAD,
        START,
        STOP,
        LOOP_CHECKING,
        NUMBER_PICKER_START,
        NUMBER_PICKER_END,
        LICENSE,
        END
    }

    private enum TextType {
        NOW_TIME,
        TITLE,
        ARTIST,
        ALBUM,
        LOOP_START,
        LOOP_END,
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
    private MediaPlayer[] arrayMediaPlayer;

    private Map<SeekBarType, SeekBar> seekBarMap;
    private RangeBar                  loopPointRangeBar;

    private Map<ButtonType, ImageView>   imageViewMap;
    private Map<ButtonType, ImageButton> buttonMap;
    private Map<TextType, TextView>      textMap;

    private ImageButton mediaPlayerButton;
    private ButtonType mediaPlayerType = ButtonType.NONE;

    //
    private int  loopPointStart = 0; // ms
    private int  loopPointEnd   = 0; // ms
    private long preTime        = 0;
    private int  musicLength    = 0;
    private int  playTime       = 0;    // 現在の再生時刻
    private int  playNumber     = 0;    // 再生中のメディアプレイヤー番号

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
     * 該当キーがmapに設定済みかどうか
     *
     * @param map  確認対象のmap
     * @param keys キー
     * @param <T>  ジェネリクス:マップのキー
     * @param <U>  ジェネリクス:マップの値
     * @return 追加の有無 / true:未設定, false:設定済み
     */
    private <T, U> boolean IsNotMapConfigured(Map<T, U> map, T[] keys) {
        if (map == null) {
            DebugLog.v("テスト", "[IsMapDuplicate:map == null]");
            return true;
        }
        for (T key : keys) {
            if (key == null) {
                DebugLog.v("テスト", "[IsMapDuplicate:key == null]");
                return true;
            }
            if (map.get(key) == null) {
                DebugLog.v("テスト", "[IsMapDuplicate:map.get(" + key + ") == null]");
                return true;
            }
        }
        return false;
    }

    /**
     * 確認対象のマップに値を追加できるか確認
     *
     * @param map 確認対象のmap
     * @param key キー
     * @param <T> ジェネリクス:マップのキー
     * @param <U> ジェネリクス:マップの値
     * @return 追加の有無 / true:不可能, false:可能
     */
    private <T, U> boolean CanNotAddMap(Map<T, U> map, T key) {
        if (map == null) {
            DebugLog.v("テスト", "[CanNotAddMap:map == null]");
            return true;
        }
        if (map.get(key) != null) {
            DebugLog.v("テスト", "[CanNotAddMap:map.get(" + key + ") == null]");
            return true;
        }
        return false;
    }

    /**
     * ID指定でTextView配列に追加
     *
     * @param type 指定する列挙型ID
     * @param id   TextViewのID
     */
    //Map<TextType, TextView> textMap
    private void AddTextMap(TextType type, int id) {
        if (CanNotAddMap(textMap, type)) {
            return;
        }

        TextView _textView = findViewById(id);
        // 文字列を表示し切れないときにはスクロールする
        _textView.setSelected(true);
        textMap.put(type, _textView);
    }

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
            DebugLog.v("テスト", "[CalculateProgressToTime] = 0");
            return 0;
        }

        if (progressMax <= progressValue) {
            DebugLog.v("テスト", "[CalculateProgressToTime] = " + musicLength + " / " + progressMax);
            return musicLength;
        }

        double _percent   = (double)progressValue / (double)progressMax;
        double _calculate = (double)musicLength * _percent;
        DebugLog.v("テスト",
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
            DebugLog.v("テスト", "[CalculateTimeToProgress] = 0");
            return 0;
        }

        if (musicLength <= musicValue) {
            DebugLog.v("テスト", "[CalculateTimeToProgress] = " + musicLength + " / " + progressMax);
            return progressMax;
        }

        double _percent   = (double)musicValue / (double)musicLength;
        double _calculate = (double)progressMax * _percent;
        DebugLog.v("テスト",
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
                           this.getString(R.string.fatal_error_no_set_media_player),
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
        numberpickerUpdate = false;

        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * ループポイントのテキスト更新
     */
    private void UpdateLoopPointText() {
        TextType[] _keys = {
            TextType.LOOP_START,
            TextType.LOOP_END
        };
        if (IsNotMapConfigured(textMap, _keys)) {
            return;
        }

        TextView loopPointStartText = textMap.get(TextType.LOOP_START);
        TextView loopPointEndText   = textMap.get(TextType.LOOP_END);
        loopPointStartText.setText(dataFormat.format(loopPointStart));
        loopPointEndText.setText(dataFormat.format(loopPointEnd));
    }

    /**
     * シークバー位置をループポイントに対応させる
     */
    private void UpdateLoopPointSeekbar() {
        if (loopPointRangeBar == null) {
            return;
        }

        int _max   = CALCULATE_SEEKBAR_COUNT;
        int _start = CalculateTimeToProgress(loopPointStart, musicLength, _max);
        int _end   = CalculateTimeToProgress(loopPointEnd, musicLength, _max);
        loopPointRangeBar.setThumbIndices(_start, _end);
    }

    /**
     * 現在の時間更新
     */
    private void UpdateNowTime() {
        TextType[] _keysTextType = {
            TextType.NOW_TIME,
            TextType.TITLE,
            TextType.ARTIST,
            TextType.ALBUM,
            };
        if (IsNotMapConfigured(textMap, _keysTextType)) {
            return;
        }

        SeekBarType[] _keysSeekBarType = {
            SeekBarType.NOW_TIME,
            };
        if (IsNotMapConfigured(seekBarMap, _keysSeekBarType)) {
            return;
        }

        // ループ確認中の場合シークバー操作を禁止する
        TextView nowTimeText    = textMap.get(TextType.NOW_TIME);
        SeekBar  nowTimeSeekBar = seekBarMap.get(SeekBarType.NOW_TIME);
        if (!IsLoopChecking()) {
            nowTimeSeekBar.setEnabled(loadCompletedBGM);
        }

        int _max = nowTimeSeekBar.getMax();
        if (IsNotMediaPlayer()) {
            String _text = dataFormat.format(0) + "/" + dataFormat.format(musicLength);
            nowTimeText.setText(_text);
            textMap.get(TextType.TITLE).setText("");
            textMap.get(TextType.ARTIST).setText("");
            textMap.get(TextType.ALBUM).setText("");
            nowTimeSeekBar.setProgress(CalculateTimeToProgress(0, musicLength, _max));
            return;
        }

        if (operationTimeBar) {
            int _now = CalculateProgressToTime(nowTimeSeekBar.getProgress(),
                                               nowTimeSeekBar.getMax(),
                                               musicLength
            );
            String _text = dataFormat.format(_now) + "/" + dataFormat.format(musicLength);
            nowTimeText.setText(_text);
            return;
        }

        int    _nowPosition = arrayMediaPlayer[playNumber].getCurrentPosition();
        String _text        = dataFormat.format(_nowPosition) + "/" + dataFormat.format(musicLength);
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
            Toast toast = Toast.makeText(this,
                                         this.getString(R.string.permission_request),
                                         Toast.LENGTH_SHORT
            );
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

    /**
     * 音楽操作ボタンの制御処理
     */
    private void ControlMediaPlayer() {
        if (MainActivity.IsNotClickEvent()) {
            return;
        }

        switch (mediaPlayerType) {
        case START:
            // 音楽再生
            audioPlay();
            break;
        case STOP:
            // ループ確認中の場合ループ確認を停止する
            if (IsLoopChecking()) {
                TerminateLoopChecking();
                return;
            }
            // 音楽停止
            audioStop();
            break;
        }
    }

    /**
     * ナンバーピッカーダイアログの実行
     *
     * @param type ボタンタイプ
     */
    private void ExecuteNumberPickerDialog(ButtonType type) {
        if (MainActivity.IsNotClickEvent()) {
            return;
        }
        if (numberpickerDialogFragment.isResumed()) {
            return;
        }

        // ダイアログに値を渡す
        Bundle bundle = new Bundle();

        // 念のためナンバーピッカー以外のものが来たら処理を終える
        switch (type) {
        case NUMBER_PICKER_START:
            bundle.putInt("LoopType", LoopType.START.ordinal());
            break;
        case NUMBER_PICKER_END:
            bundle.putInt("LoopType", LoopType.END.ordinal());
            break;
        default:
            return;
        }

        bundle.putInt("LoopPointStart", loopPointStart);
        bundle.putInt("LoopPointEnd", loopPointEnd);
        bundle.putInt("MusicLength", musicLength);
        numberpickerDialogFragment.setArguments(bundle);
        numberpickerDialogFragment.show(getSupportFragmentManager(),
                                        NumberPickerDialogFragment.class.getSimpleName()
        );
    }

    /**
     * 曲選択ダイアログの実行
     */
    private void ExecuteTrackDialog() {
        if (trackDialogFragment.isResumed()) {
            return;
        }
        if (MainActivity.IsNotClickEvent()) {
            return;
        }
        if (!permissionGranted) {
            checkPermission();
            return;
        }

        trackDialogFragment.show(getSupportFragmentManager(),
                                 NumberPickerDialogFragment.class.getSimpleName()
        );
    }

    /**
     * 音楽操作ボタンの更新処理
     *
     * @param type ボタンタイプ
     */
    private void UpdateMediaPlayerButton(ButtonType type) {
        mediaPlayerType = type;
        int _resource;
        switch (type) {
        case START:
            _resource = R.drawable.ic_play_circle_outline;
            break;
        case STOP:
            _resource = R.drawable.ic_pause_circle_outline;
            break;
        default:
            return;
        }
        mediaPlayerButton.setImageResource(_resource);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_handler = new Handler();
        m_handler.postDelayed(this, updateIntarval);
        setContentView(R.layout.activity_main);

        seekBarMap = new HashMap<>();
        imageViewMap = new HashMap<>();
        buttonMap = new HashMap<>();
        textMap = new HashMap<>();

        // permissionの確認
        checkPermission();

        // ループポイントの時間
        AddTextMap(TextType.NOW_TIME, R.id.textNowTime);
        AddTextMap(TextType.TITLE, R.id.title);
        AddTextMap(TextType.ARTIST, R.id.artist);
        AddTextMap(TextType.ALBUM, R.id.album);
        AddTextMap(TextType.LOOP_START, R.id.loopPointStart);
        AddTextMap(TextType.LOOP_END, R.id.loopPointEnd);
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
        loopPointRangeBar = findViewById(R.id.rangeBar);
        loopPointRangeBar.setTickCount(MAX_SEEKBAR_COUNT);
        loopPointRangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int leftThumbIndex, int rightThumbIndex) {
                UpdateSeekbar(leftThumbIndex, rightThumbIndex);
            }
        });

        // 曲選択ダイアログ表示ボタン
        trackDialogFragment = new TrackDialogFragment();
        ImageView buttonLoad = findViewById(R.id.loadButton);
        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                ExecuteTrackDialog();
            }
        });
        imageViewMap.put(ButtonType.LOAD, buttonLoad);
        ///

        // 音楽操作ボタン
        ImageButton buttonStart = findViewById(R.id.control);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ControlMediaPlayer();
            }
        });
        mediaPlayerButton = buttonStart;

        // ループテストボタン
        ImageButton buttonLoopChecking = findViewById(R.id.loopTest);
        buttonLoopChecking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetupLoopChecking();
            }
        });
        buttonMap.put(ButtonType.LOOP_CHECKING, buttonLoopChecking);

        // DialogFragment表示をボタンに登録
        numberpickerDialogFragment = new NumberPickerDialogFragment();
        ImageButton buttonNumberPickerStart = findViewById(R.id.btnLoopPointStart);
        buttonNumberPickerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExecuteNumberPickerDialog(ButtonType.NUMBER_PICKER_START);
            }
        });
        buttonMap.put(ButtonType.NUMBER_PICKER_START, buttonNumberPickerStart);

        // ループポイント[End]
        ImageButton buttonNumberPickerEnd = findViewById(R.id.btnLoopPointEnd);
        buttonNumberPickerEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExecuteNumberPickerDialog(ButtonType.NUMBER_PICKER_END);
            }
        });
        buttonMap.put(ButtonType.NUMBER_PICKER_END, buttonNumberPickerEnd);

        SetBulkEnabled(false);

        // 広告用
        // バナー広告
        MobileAds.initialize(getApplicationContext(), this.getString(R.string.admob_app_id));
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)  // エミュレーター
            .addTestDevice(this.getString(R.string.admob_test_device_id_01)).build();
        mAdView.loadAd(adRequest);

        // ライセンス表示用ボタン
        ImageButton btn_oss = findViewById(R.id.btn_oss);
        btn_oss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowLicenseActivity();
            }
        });
        buttonMap.put(ButtonType.LICENSE, btn_oss);
    }

    /**
     * permissionのアクセス許可の結果受け取り
     *
     * @param requestCode  requestPermissionsの第3引数で指定した値
     * @param permissions  requestPermissionsの第2引数で指定した値
     * @param grantResults permmisionが許可されたかどうか
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
            Toast toast = Toast.makeText(this,
                                         this.getString(R.string.permission_denied),
                                         Toast.LENGTH_SHORT
            );
            toast.show();
        }
    }

    /**
     * 周期的に呼ばれる更新処理
     */
    @Override
    public void run() {
        // ここで毎フレームの処理
        UpdateNowTime();

        numberpickerUpdate = false;

        if (!IsPlayingMediaPlayer()) {
            m_handler.postDelayed(this, updateIntarval);
            return;
        }

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
        // 曲の再生時間超えたら更新しない
        if (loopPoint < 0 || musicLength < loopPoint) {
            String _errorText = this.getString(R.string.error_numberpicker_too_time);
            _errorText += "\n[" + dataFormat.format(musicLength);
            _errorText += " > " + dataFormat.format(loopPoint) + "]";
            Toast.makeText(getApplication(), _errorText, Toast.LENGTH_SHORT).show();
            return;
        }

        numberpickerUpdate = true;
        boolean _replacement = false;
        if (dialogType == LoopType.START) {
            _replacement = (loopPointEnd < loopPoint);
        }
        else if (dialogType == LoopType.END) {
            _replacement = (loopPoint < loopPointStart);
        }

        if (dialogType == LoopType.START) {
            if (_replacement) {
                this.loopPointStart = this.loopPointEnd;
                this.loopPointEnd = loopPoint;
            }
            else {
                this.loopPointStart = loopPoint;
            }
        }
        else if (dialogType == LoopType.END) {
            if (_replacement) {
                this.loopPointEnd = this.loopPointStart;
                this.loopPointStart = loopPoint;
            }
            else {
                this.loopPointEnd = loopPoint;
            }
        }

        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * シークバー操作でループポイント更新
     */
    private void UpdateSeekbar(int leftThumbIndex, int rightThumbIndex) {
        // ナンバーピッカーから設定した直後は処理しない
        if (numberpickerUpdate) {
            return;
        }
        if (musicLength <= MUSIC_LENGTH_MIN) {
            return;
        }
        int _max = CALCULATE_SEEKBAR_COUNT;

        DebugLog.v("テスト", "[UpdateSeekbar:" + leftThumbIndex + " - " + rightThumbIndex + "]");

        loopPointStart = CalculateProgressToTime(leftThumbIndex, _max, musicLength);
        loopPointEnd = CalculateProgressToTime(rightThumbIndex, _max, musicLength);

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

        ReleaseMediaPlayer();

        // インタンスを生成
        arrayMediaPlayer = new MediaPlayer[]{
            new MediaPlayer(),
            new MediaPlayer()
        };

        // URIから音楽ファイルを読み込む
        try {
            DebugLog.v("テスト", "[LoadBGM:try]");
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // MediaPlayerに読み込む音楽ファイルを指定
            for (MediaPlayer anArrayMediaPlayer : arrayMediaPlayer) {
                anArrayMediaPlayer.setLooping(true);
                anArrayMediaPlayer.setDataSource(getApplicationContext(), uri);
                anArrayMediaPlayer.prepare();
            }
            loadCompletedBGM = true;
            UpdateMediaPlayerButton(ButtonType.START);

            SetBulkEnabled(true);
        }
        catch (IOException e1) {
            DebugLog.v("テスト", "[LoadBGM:catch]");
            ReleaseMediaPlayer();
            e1.printStackTrace();
            return false;
        }
        DebugLog.v("テスト", "[LoadBGM:success]");
        return true;
    }

    /**
     * BGM再生開始
     */
    private void audioPlay() {
        if (!loadCompletedBGM) {
            Toast.makeText(getApplication(),
                           this.getString(R.string.error_bgm_loading_uncompleted),
                           Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int _difference = loopPointEnd - loopPointStart;
        if (_difference < LOOP_POINT_INTERVAL) {
            // ループ間隔が不十分
            String _errorText = this.getString(R.string.error_bgm_Insufficient_loop_interval_01);
            _errorText += "\n" + this.getString(R.string.error_bgm_Insufficient_loop_interval_02);
            _errorText += (double)(LOOP_POINT_INTERVAL / 1000);
            _errorText += this.getString(R.string.error_bgm_Insufficient_loop_interval_03);
            Toast.makeText(getApplication(), _errorText, Toast.LENGTH_SHORT).show();
            return;
        }
        ///

        if (IsPlayingMediaPlayer()) {
            Toast.makeText(getApplication(),
                           this.getString(R.string.error_bgm_is_played),
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

        this.arrayMediaPlayer[this.playNumber].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                DebugLog.d("debug", "end of audio");
                audioStop();
            }
        });

        UpdateMediaPlayerButton(ButtonType.STOP);

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
        loopPointRangeBar.setEnabled(false);

        // ボタンの操作禁止
        for (ButtonType key : imageViewMap.keySet()) {
            ImageView btn = imageViewMap.get(key);
            btn.setClickable(false);
            btn.setColorFilter(getResources().getColor(R.color.btnGrayOut));
        }
        for (ButtonType key : buttonMap.keySet()) {
            if (key == ButtonType.STOP) {
                continue;
            }
            ImageButton btn = buttonMap.get(key);
            btn.setClickable(false);
            btn.setColorFilter(getResources().getColor(R.color.btnGrayOut));
        }
    }

    /**
     * BGM停止
     */
    private void audioStop() {
        if (!loadCompletedBGM) {
            Toast.makeText(getApplication(),
                           this.getString(R.string.error_bgm_loading_uncompleted),
                           Toast.LENGTH_SHORT
            ).show();
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
     * ボタン、シークバーの操作制限を行う
     */
    private void TeardownAudioPlay() {
        if (IsLoopChecking()) {
            return;
        }

        for (SeekBarType key : seekBarMap.keySet()) {
            SeekBar _seekBar = seekBarMap.get(key);
            _seekBar.setEnabled(true);
        }
        loopPointRangeBar.setEnabled(true);

        // ボタンの操作禁止
        for (ButtonType key : imageViewMap.keySet()) {
            ImageView btn = imageViewMap.get(key);
            btn.setClickable(true);
            btn.clearColorFilter();
        }
        for (ButtonType key : buttonMap.keySet()) {
            ImageButton btn = buttonMap.get(key);
            btn.setClickable(true);
            btn.clearColorFilter();
        }

        UpdateMediaPlayerButton(ButtonType.START);
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
        if (MainActivity.IsNotClickEvent()) {
            return;
        }

        if (!loadCompletedBGM) {
            return;
        }

        loopChecking = true;
        loopCheckingAfter = false;

        // シークバーの操作禁止
        prevSeekBarEnabled = new ArrayList<>();
        for (SeekBarType key : seekBarMap.keySet()) {
            SeekBar _seekBar = seekBarMap.get(key);
            prevSeekBarEnabled.add(_seekBar.isEnabled());
            _seekBar.setEnabled(false);
        }
        prevSeekBarEnabled.add(loopPointRangeBar.isEnabled());
        loopPointRangeBar.setEnabled(false);

        // ボタンの操作禁止
        prevButtonEnabled = new ArrayList<>();
        for (ButtonType key : imageViewMap.keySet()) {
            ImageView btn = imageViewMap.get(key);
            prevButtonEnabled.add(btn.isEnabled());
            btn.setClickable(false);
            btn.setColorFilter(getResources().getColor(R.color.btnGrayOut));
        }
        for (ButtonType key : buttonMap.keySet()) {
            ImageButton btn = buttonMap.get(key);
            prevButtonEnabled.add(btn.isEnabled());
            if (key == ButtonType.STOP) {
                continue;
            }
            btn.setClickable(false);
            btn.setColorFilter(getResources().getColor(R.color.btnGrayOut));
        }

        int _testStart = this.loopPointEnd - LOOP_POINT_CHECK_INTERVAL;
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

        if (this.playTime < (this.loopPointStart + LOOP_POINT_CHECK_INTERVAL)) {
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
            _index++;
        }
        loopPointRangeBar.setEnabled(prevSeekBarEnabled.get(_index));
        while (prevSeekBarEnabled.remove((Integer)2)) {
        }
        prevSeekBarEnabled = null;

        // ボタンの操作禁止解除
        _index = 0;
        for (ButtonType key : imageViewMap.keySet()) {
            ImageView btn = imageViewMap.get(key);
            btn.setClickable(prevButtonEnabled.get(_index));
            btn.clearColorFilter();
            _index++;
        }
        for (ButtonType key : buttonMap.keySet()) {
            ImageButton btn = buttonMap.get(key);
            btn.setClickable(prevButtonEnabled.get(_index));
            btn.clearColorFilter();
            _index++;
        }
        while (prevButtonEnabled.remove((Integer)2)) {
        }
        prevButtonEnabled = null;

        loopChecking = false;
        loopCheckingAfter = false;

        UpdateMediaPlayerButton(ButtonType.START);
    }

    /**
     * ループポイントを保存する
     *
     * @param title BGM名
     */
    private void SaveLoopPointDate(String title) {
        SharedPreferences        pref = getSharedPreferences(PREFERENCES_TITLE, MODE_PRIVATE);
        SharedPreferences.Editor e    = pref.edit();
        e.putInt(title + "_Start", loopPointStart);
        e.putInt(title + "_End", loopPointEnd);
        e.apply();
    }

    /**
     * ループポイントが保存されていれば読み込む
     *
     * @param title BGM名
     */
    public void LoadLoopPointDate(String title) {
        DebugLog.v("テスト", "[LoadLoopPointDate:" + title + "]");

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
        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * メイン画面用の曲情報更新処理
     *
     * @param title  選択した曲名
     * @param artist 選択した曲のアーティスト
     * @param album  選択した曲が収録されてるアルバム名
     */
    public void UpdateMusicData(String title, String artist, String album, String albumArt) {
        TextType[] _keysTextType = {
            TextType.TITLE,
            TextType.ARTIST,
            TextType.ALBUM,
            };
        if (IsNotMapConfigured(textMap, _keysTextType)) {
            return;
        }

        textMap.get(TextType.TITLE).setText(title);
        textMap.get(TextType.ARTIST).setText(artist);
        textMap.get(TextType.ALBUM).setText(album);

        // アルバムアートを更新する
        ButtonType[] _buttonType = {
            ButtonType.LOAD,
            };
        if (IsNotMapConfigured(imageViewMap, _buttonType)) {
            return;
        }
        ImageView _artworkImageView = imageViewMap.get(ButtonType.LOAD);
        _artworkImageView.setTag(albumArt);
        ImageGetTask task = new ImageGetTask(_artworkImageView);
        ImageGetTask.Params _param = new ImageGetTask.Params(albumArt,
                                                             ImageGetTask.Params.Type.DEFAULT
        );
        task.execute(_param);
    }

    /**
     * 一括で有効設定の更新を行う
     *
     * @param enabled 設定を有効かするかどうか
     */
    private void SetBulkEnabled(boolean enabled) {
        SetBulkSeekBarInfo(enabled);
        SetBulkImageButtonInfo(enabled);
    }

    /**
     * seekbarの一括設定
     *
     * @param enabled 設定を有効かするかどうか
     */
    private void SetBulkSeekBarInfo(boolean enabled) {
        for (SeekBarType key : seekBarMap.keySet()) {
            SeekBar _seekBar = seekBarMap.get(key);
            _seekBar.setEnabled(enabled);
        }
        loopPointRangeBar.setEnabled(enabled);
    }

    /**
     * ImageButtonの一括設定
     *
     * @param clickable タップを有効かするかどうか
     */
    private void SetBulkImageButtonInfo(boolean clickable) {
        List<ImageButton> btnList = new ArrayList<>();
        btnList.add(mediaPlayerButton);
        for (ButtonType key : buttonMap.keySet()) {
            btnList.add(buttonMap.get(key));
        }

        for (ImageButton btn : btnList) {
            btn.setClickable(clickable);
            if (clickable) {
                btn.clearColorFilter();
            }
            else {
                btn.setColorFilter(getResources().getColor(R.color.btnGrayOut));
            }
        }
    }

    /**
     * ライセンス表示用アクティビティ表示処理
     */
    private void ShowLicenseActivity() {
        if (MainActivity.IsNotClickEvent()) {
            return;
        }
        Intent intent = new Intent(MainActivity.this, LicenseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}

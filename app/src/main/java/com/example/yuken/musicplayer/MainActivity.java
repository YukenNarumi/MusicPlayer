package com.example.yuken.musicplayer;

import android.Manifest;
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
import java.util.List;
import java.util.Locale;

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity implements Runnable {

    private int REQUEST_PERMISSION = 1000;

    public enum LoopType {
        START,
        END
    };

    /**
     * ordinal から指定した Enum の要素に変換する汎用関数
     */
    public static <E extends Enum<E>> E fromOrdinal(Class<E> enumClass, int ordinal) {
        E[] enumArray = enumClass.getEnumConstants();
        return enumArray[ordinal];
    }

    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.SS", Locale.JAPAN);

    private Handler m_handler;
    private MediaPlayer mediaPlayer;
    private MediaPlayer[] arrayMediaPlayer;

    private TextView nowTimeText;
    private TextView loopPointStartText;
    private TextView loopPointEndText;

    private SeekBar nowTimeSeekBar;
    private SeekBar loopPointStartSeekBar;
    private SeekBar loopPointEndSeekBar;

    private List<Button> buttonList;

    //
    // ループポイント設定に必要最低限のBGM長(ms)
    private int MUSIC_LENGTH_MIN = 10000;

    // ループポイントの始点・終点設定に必要な間隔(ms)
    private int LOOP_POINT_INTERVAL = 5000;

    // 更新処理の間隔(ms)
    private int updateIntarval = 5;
    // ループポイント(終点)の受付猶予(ms)
    private int receptionEndPoint = 30;

    private int loopPointStart  = 0; // ms
    private int loopPointEnd    = 0; // ms
    private long preTime        = 0;
    private int musicLength     = 0;
    private int playTime        = 0;    // 現在の再生時刻
    private int playNumber      = 0;    // 再生中のメディアプレイヤー番号

    private int prevProgressStart = 0;
    private int prevProgressEnd = 0;

    private TrackDialogFragment trackDialogFragment;
    private NumberPickerDialogFragment numberpickerDialogFragment;

    private boolean numberpickerUpdate = false;

    private boolean loadCompletedBGM = false;

    private boolean operationTimeBar = false;

    private boolean permissionGranted = false;

    private boolean loopChecking = false;
    private boolean loopCheckingAfter = false;
    private List<Boolean> prevSeekBarEnabled;
    private List<Boolean> prevButtonEnabled;
    ///

    /**
     * メディアプレイヤーが設定済みか
     *
     * @return true:設定済み / false:未設定がある
     */
    private boolean IsMediaPlayer(){
        if(arrayMediaPlayer == null){
            return false;
        }
        for(MediaPlayer _media : arrayMediaPlayer){
            if(_media != null){
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * 再生中のメディアプレイヤーがあるか
     *
     * @return true:再生中 / false:未再生
     */
    private boolean IsPlayingMediaPlayer(){
        if(!IsMediaPlayer()){
            return false;
        }

        for(MediaPlayer _media : arrayMediaPlayer){
            if(_media.isPlaying()){
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
     * @return              現在の再生時間
     */
    private int CalculateProgressToTime(int progressValue, int progressMax, int musicLength){
        if(progressValue <= 0 || progressMax <= 0 || musicLength <= 0) {
            Log.v("テスト", "[CalculateProgressToTime] = 0");
            return 0;
        }

        if(progressMax <= progressValue){
            Log.v("テスト", "[CalculateProgressToTime] = " + musicLength + " / " + progressMax);
            return musicLength;
        }

        double _percent = (double)progressValue / (double)progressMax;
        double _calculate = (double)musicLength * _percent;
        Log.v("テスト", "[CalculateProgressToTime:" + _calculate + " = " + musicLength + " * (" + progressValue + " / " + progressMax + ")");
        return (int)_calculate;
    }

    /**
     * 時間からシークバーの位置(メモリ)に変換する
     *
     * @param musicValue    現在の再生時間
     * @param musicLength   曲の再生時間
     * @param progressMax   シークバーのメモリの最大値
     * @return              シークバーのメモリの値
     */
    private int CalculateTimeToProgress(int musicValue, int musicLength, int progressMax){
        if(musicValue <= 0 || progressMax <= 0 || musicLength <= 0) {
            Log.v("テスト", "[CalculateTimeToProgress] = 0");
            return 0;
        }

        if(musicLength <= musicValue){
            Log.v("テスト", "[CalculateTimeToProgress] = " + musicLength + " / " + progressMax);
            return progressMax;
        }

        double _percent = (double)musicValue / (double)musicLength;
        double _calculate = (double)progressMax * _percent;
        Log.v("テスト", "[CalculateTimeToProgress:" + _calculate + " = " + musicLength + " * (" + musicValue + " / " + progressMax + ")");
        return (int)_calculate;
    }

    /**
     * 再生時間等のクリア
     */
    public void ClearMediaPlayerInfo(){
        if(!IsMediaPlayer()){
            Toast.makeText(getApplication(), "Error: Call timing is incorrect [ClearMediaPlayerInfo()]", Toast.LENGTH_SHORT).show();
            return;
        }

        musicLength         = arrayMediaPlayer[0].getDuration();
        loopPointStart      = 0;
        loopPointEnd        = musicLength;
        preTime             = 0;
        playTime            = 0;
        playNumber          = 0;
        prevProgressStart   = 0;
        prevProgressEnd     = 0;
        numberpickerUpdate  = false;

        UpdatePrevLoopPoint();
        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * 前回のループポイントを更新
     */
    private void UpdatePrevLoopPoint(){
        prevProgressStart   = loopPointStart;
        prevProgressEnd     = loopPointEnd;
    }

    /**
     * ループポイントのテキスト更新
     */
    private void UpdateLoopPointText(){
        if(loopPointStartText == null || loopPointEndText == null){
            return;
        }

        loopPointStartText.setText(dataFormat.format(loopPointStart));
        loopPointEndText.setText(dataFormat.format(loopPointEnd));
    }

    /**
     * シークバー位置をループポイントに対応させる
     */
    private void UpdateLoopPointSeekbar(){
        if(loopPointStartSeekBar == null || loopPointEndSeekBar == null){
            return;
        }

        int _max = loopPointStartSeekBar.getMax();
        loopPointStartSeekBar.setProgress(CalculateTimeToProgress(loopPointStart, musicLength, _max));
        loopPointEndSeekBar.setProgress(CalculateTimeToProgress(loopPointEnd, musicLength, _max));
    }

    /**
     * 現在の時間更新
     */
    private void UpdateNowTime() {
        if(nowTimeText == null || nowTimeSeekBar == null){
            return;
        }

        // ループ確認中の場合シークバー操作を禁止する
        if(!IsLoopChecking()){
            nowTimeSeekBar.setEnabled(loadCompletedBGM);
        }

        int _max = nowTimeSeekBar.getMax();
        if(!IsMediaPlayer()) {
            String _text = dataFormat.format(0) + "/" + dataFormat.format(musicLength);
            nowTimeText.setText(_text);
            nowTimeSeekBar.setProgress(CalculateTimeToProgress(0, musicLength, _max));
            return;
        }

        int _nowPosition = arrayMediaPlayer[playNumber].getCurrentPosition();

        if(operationTimeBar){
            int _now = CalculateProgressToTime(nowTimeSeekBar.getProgress(), nowTimeSeekBar.getMax(), musicLength);
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
    private void ReleaseMediaPlayer(){
        if(arrayMediaPlayer == null){
            return;
        }
        for(int i = 0; i < arrayMediaPlayer.length; i++){
            if(arrayMediaPlayer[i] == null){
                continue;
            }

            if(arrayMediaPlayer[i].isPlaying()) {
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
        if(Build.VERSION.SDK_INT < 23){
            permissionGranted = true;
            return;
        }

        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            permissionGranted = true;
        }
        // 拒否していた場合
        else{
            permissionGranted = false;
            requestLocationPermission();
        }
    }

    /**
     * permissionのアクセス許可を求める
     */
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
        else {
            Toast toast = Toast.makeText(this, "許可してください", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                    REQUEST_PERMISSION);
        }
    }

    /**
     *
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_handler = new Handler();
        m_handler.postDelayed(this, this.updateIntarval);
        setContentView(R.layout.activity_main);

        buttonList = new ArrayList<Button>();

        // permissionの確認
        checkPermission();

        UpdatePrevLoopPoint();

        // ループポイントの時間
        nowTimeText = findViewById(R.id.textNowTime);
        loopPointStartText = findViewById(R.id.loopPointStart);
        loopPointEndText = findViewById(R.id.loopPointEnd);
        UpdateLoopPointText();

        nowTimeSeekBar = findViewById(R.id.seekBarNowTime);
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
                int _nowTime = CalculateProgressToTime(nowTimeSeekBar.getProgress(), nowTimeSeekBar.getMax(), musicLength);
                arrayMediaPlayer[playNumber].seekTo(_nowTime);
                playTime = _nowTime;
            }
        });

        // ループポイントの時間
        loopPointStartSeekBar = findViewById(R.id.seekBarStart);
        loopPointStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // ナンバーピッカーから設定した直後は処理しない
                if(numberpickerUpdate) {
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

        loopPointEndSeekBar = findViewById(R.id.seekBarEnd);
        loopPointEndSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // ナンバーピッカーから設定した直後は処理しない
                if(numberpickerUpdate) {
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

        //

        // 曲選択ダイアログ表示ボタン
        trackDialogFragment = new TrackDialogFragment();
        Button buttonLoad = findViewById(R.id.loadButton);
        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if(!permissionGranted){
                    return;
                }
                trackDialogFragment.show(getSupportFragmentManager(), NumberPickerDialogFragment.class.getSimpleName());
            }
        });
        buttonList.add(buttonLoad);
        ///

        // 音楽開始ボタン
        Button buttonStart = findViewById(R.id.start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 音楽再生
                audioPlay();
            }
        });
        buttonList.add(buttonStart);

        // 音楽停止ボタン
        Button buttonStop = findViewById(R.id.stop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ループ確認中の場合ループ確認を停止する
                if(IsLoopChecking()){
                    TerminateLoopChecking();
                    return;
                }

                // 音楽停止
                audioStop();
            }
        });

        // ループテストボタン
        Button buttonLoopChecking = findViewById(R.id.loopTest);
        buttonLoopChecking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetupLoopChecking();
            }
        });
        buttonList.add(buttonLoopChecking);

        // DialogFragment表示をボタンに登録
        numberpickerDialogFragment = new NumberPickerDialogFragment();
        Button buttonNumberPickerStart = findViewById(R.id.btnLoopPointStart);
        buttonNumberPickerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ダイアログに値を渡す
                Bundle bundle = new Bundle();
                bundle.putInt("LoopType", LoopType.START.ordinal());
                bundle.putInt("LoopPointStart", loopPointStart);
                bundle.putInt("LoopPointEnd", loopPointEnd);
                bundle.putInt("MusicLength", musicLength);
                numberpickerDialogFragment.setArguments(bundle);

                numberpickerDialogFragment.show(getSupportFragmentManager(), NumberPickerDialogFragment.class.getSimpleName());
            }
        });
        buttonList.add(buttonNumberPickerStart);

        // ループポイント[End]
        Button buttonNumberPickerEnd = findViewById(R.id.btnLoopPointEnd);
        buttonNumberPickerEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putInt("LoopType", LoopType.END.ordinal());
                bundle.putInt("LoopPointStart", loopPointStart);
                bundle.putInt("LoopPointEnd", loopPointEnd);
                bundle.putInt("MusicLength", musicLength);
                numberpickerDialogFragment.setArguments(bundle);

                numberpickerDialogFragment.show(getSupportFragmentManager(), NumberPickerDialogFragment.class.getSimpleName());
            }
        });
        buttonList.add(buttonNumberPickerEnd);
    }

    /**
     * permissionのアクセス許可の結果受け取り
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

        if(!IsPlayingMediaPlayer()) {
            m_handler.postDelayed(this, this.updateIntarval);
            return;
        }

        numberpickerUpdate = false;

        this.playTime += System.currentTimeMillis() - this.preTime;

        UpdateLoopChecking();

        if((this.loopPointEnd - this.receptionEndPoint) <= this.playTime){
            this.playTime = this.loopPointStart;
            ///
            this.arrayMediaPlayer[this.playNumber].seekTo(this.loopPointStart);
            this.arrayMediaPlayer[this.playNumber].pause();

            int _nextID = (this.playNumber + 1) % this.arrayMediaPlayer.length;
            this.arrayMediaPlayer[_nextID].start();
            this.playNumber = _nextID;
            ///

            if(IsLoopChecking()){
                loopChecking        = false;
                loopCheckingAfter   = true;
            }
        }
        this.preTime = System.currentTimeMillis();

        m_handler.postDelayed(this, this.updateIntarval);
    }

    /**
     * ナンバーピッカーに設定した値で更新
     *
     * @param dialogType    表示中のナンバーピッカー(スタート/エンド)
     * @param loopPoint     ループポイント
     */
    public void UpdateNumbetPickerr(LoopType dialogType, int loopPoint){
        numberpickerUpdate  = true;

        UpdatePrevLoopPoint();

        if(dialogType == LoopType.START){
            this.loopPointStart = loopPoint;
        }
        else if(dialogType == LoopType.END){
            this.loopPointEnd   = loopPoint;
        }

        UpdateLoopPointText();
        UpdateLoopPointSeekbar();
    }

    /**
     * シークバー操作でループポイント更新
     */
    private void UpdateSeekbar(LoopType loopType){
        if(musicLength <= MUSIC_LENGTH_MIN){ return; }

        int _max        = loopPointStartSeekBar.getMax();
        int _nowStart   = loopPointStartSeekBar.getProgress();
        int _nowEnd     = loopPointEndSeekBar.getProgress();
        int _difference = CalculateProgressToTime(Math.abs(_nowEnd - _nowStart), _max, musicLength);

        Log.v("テスト", "[difference" +"(" + (_difference <= LOOP_POINT_INTERVAL) + "):" + _difference + " = " + _nowEnd + " - " + _nowStart + "][start:" + prevProgressStart + ">>" + loopPointStart + "][end:" + prevProgressEnd + ">>" + loopPointEnd + "]");

        // ループポイントの始点・終点の間隔が短すぎる場合前回の値に戻す
        if(_difference <= LOOP_POINT_INTERVAL){
            switch(loopType){
            case START: loopPointStart  = prevProgressStart;    break;
            case END:   loopPointEnd    = prevProgressEnd;      break;
            }
            UpdateLoopPointSeekbar();
        }
        else{
            UpdatePrevLoopPoint();
            switch(loopType){
            case START: loopPointStart  = CalculateProgressToTime(_nowStart, _max, musicLength);    break;
            case END:   loopPointEnd    = CalculateProgressToTime(_nowEnd, _max, musicLength);      break;
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
        arrayMediaPlayer = new MediaPlayer[] { new MediaPlayer(), new MediaPlayer() };

        // URIから音楽ファイルを読み込む
        try {
            Log.v("テスト", "[LoadBGM:try]");
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // MediaPlayerに読み込む音楽ファイルを指定
            for(int i = 0; i < arrayMediaPlayer.length; i++){
                arrayMediaPlayer[i].setLooping(true);
                arrayMediaPlayer[i].setDataSource(getApplicationContext(), uri);
                arrayMediaPlayer[i].prepare();
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
        if(!loadCompletedBGM){
            Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
            return;
        }

        if(IsPlayingMediaPlayer()) {
            Toast.makeText(getApplication(), "Error: playing MediaPlayer.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 再生する
        this.arrayMediaPlayer[this.playNumber].start();

        for(MediaPlayer _media : arrayMediaPlayer){
            if(_media == this.arrayMediaPlayer[this.playNumber]) { continue; }

            _media.seekTo(this.loopPointStart);
        }

        this.preTime = System.currentTimeMillis();

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
                Log.d("debug","end of audio");
                audioStop();
            }
        });
    }

    /**
     * BGM停止
     */
    private void audioStop() {
        if(!loadCompletedBGM){
            Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!IsPlayingMediaPlayer()) {
            return;
        }

        this.arrayMediaPlayer[this.playNumber].pause();
    }

    /**
     * ループ確認中
     *
     * @return true:実行中 / false:停止中
     */
    private boolean IsLoopChecking(){
        return (loopChecking || loopCheckingAfter);
    }

    /**
     * ループ確認用の初期化
     */
    private void SetupLoopChecking() {
        if(!loadCompletedBGM){
            return;
        }

        loopChecking        = true;
        loopCheckingAfter   = false;

        // シークバーの操作禁止
        prevSeekBarEnabled = new ArrayList<Boolean>();
        prevSeekBarEnabled.add(nowTimeSeekBar.isEnabled());
        prevSeekBarEnabled.add(loopPointStartSeekBar.isEnabled());
        prevSeekBarEnabled.add(loopPointEndSeekBar.isEnabled());

        nowTimeSeekBar.setEnabled(false);
        loopPointStartSeekBar.setEnabled(false);
        loopPointEndSeekBar.setEnabled(false);

        // ボタンの操作禁止
        prevButtonEnabled = new ArrayList<Boolean>();
        for(Button it : buttonList){
            prevButtonEnabled.add(it.isEnabled());
            it.setClickable(false);
        }

        int _testStart = this.loopPointEnd - LOOP_POINT_INTERVAL;
        if(_testStart < 0){
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
        if(!loopCheckingAfter){
            return;
        }

        if(this.playTime < (this.loopPointStart + LOOP_POINT_INTERVAL)){
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
        nowTimeSeekBar.setEnabled(prevSeekBarEnabled.get(0));
        loopPointStartSeekBar.setEnabled(prevSeekBarEnabled.get(1));
        loopPointEndSeekBar.setEnabled(prevSeekBarEnabled.get(2));

        while(prevSeekBarEnabled.remove((Integer)2)){}
        prevSeekBarEnabled = null;

        // ボタンの操作禁止解除
        int _index = 0;
        for(Boolean it : prevButtonEnabled){
            buttonList.get(_index).setClickable(it);
            _index++;
        }

        while(prevButtonEnabled.remove((Integer)2)){}
        prevButtonEnabled = null;

        loopChecking        = false;
        loopCheckingAfter   = false;
    }
}

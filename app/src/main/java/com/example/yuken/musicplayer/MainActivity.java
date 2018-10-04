/*
始点・終点を指定したBGMループ処理
BGM、始点、終点決め打ち
    完ぺきではないが終点についたら始点に戻ってBGM再生が行われることが確認できた

    後はアプリ内でのBGM、始点、終点の設定を行えるようになればOK
 */


package com.example.yuken.musicplayer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.SeekBar;
import android.content.res.AssetFileDescriptor;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity implements Runnable {

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
    private int playTime        = 0;
    private int playNumber      = 0;

    private int prevProgressStart = 0;
    private int prevProgressEnd = 0;

    private NumberPickerDialogFragment numberpickerDialogFragment;

    private boolean numberpickerUpdate = false;

    private boolean loadCompletedBGM = false;

    private boolean operationTimeBar = false;
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
     * シークバーの位置から時間に変換する
     *
     * @param progressValue
     * @param progressMax
     * @param musicLength
     * @return
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
     * 時間からシークバーの位置に変換する
     *
     * @param musicValue
     * @param musicLength
     * @param progressMax
     * @return
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
     * 初期化処理
     */
    private void Initialize(){
        if(!IsMediaPlayer()){
            Toast.makeText(getApplication(), "Error: Call timing is incorrect [Initialize()]", Toast.LENGTH_SHORT).show();
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
    private void UpdateNowTime(){
        if(nowTimeText == null || nowTimeSeekBar == null){
            return;
        }

        nowTimeSeekBar.setEnabled(loadCompletedBGM);

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
                // TODO:操作中のシークバーから時間変更
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                operationTimeBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO:操作を終えた時点で再生時間変更
                operationTimeBar = false;
                arrayMediaPlayer[playNumber].seekTo(CalculateProgressToTime(nowTimeSeekBar.getProgress(), nowTimeSeekBar.getMax(), musicLength));
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

        // BGMロードボタン
        Button buttonLoad = findViewById(R.id.loadButton);
        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                // BGMロードに成功したら初期化
                loadCompletedBGM = LoadBGM();
                if(!loadCompletedBGM){
                    return;
                }
                Initialize();
            }
        });

        // 音楽開始ボタン
        Button buttonStart = findViewById(R.id.start);

        // リスナーをボタンに登録
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // 音楽再生
            audioPlay();
            }
        });

        // 音楽停止ボタン
        Button buttonStop = findViewById(R.id.stop);

        // リスナーをボタンに登録
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/*
            if (mediaPlayer != null) {
                // 音楽停止
                audioStop();
            }
*/
            // 音楽停止
            audioStop();
            }
        });


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

        if((this.loopPointEnd - this.receptionEndPoint) <= this.playTime){
            this.playTime = this.loopPointStart;
            ///
            this.arrayMediaPlayer[this.playNumber].seekTo(this.loopPointStart);
            this.arrayMediaPlayer[this.playNumber].pause();

            int _nextID = (this.playNumber + 1) % this.arrayMediaPlayer.length;
            this.arrayMediaPlayer[_nextID].start();
            this.playNumber = _nextID;
            ///
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
     * @return true:成功 / false:失敗
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean audioSetup(){
        boolean fileCheck = false;

        // インタンスを生成
        arrayMediaPlayer = new MediaPlayer[] { new MediaPlayer(), new MediaPlayer() };

        //音楽ファイル名, あるいはパス
        String filePath = "am_white.mp3";

        // assetsから mp3 ファイルを読み込み
        try(AssetFileDescriptor afdescripter = getAssets().openFd(filePath))
        {
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // MediaPlayerに読み込んだ音楽ファイルを指定
            for(MediaPlayer _media : arrayMediaPlayer){
                _media.setLooping(true);
                _media.setDataSource(afdescripter.getFileDescriptor(),
                    afdescripter.getStartOffset(),
                    afdescripter.getLength());
                _media.prepare();
            }

            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return fileCheck;
    }

    /**
     * BGMを実際にロードする
     *
     * @return 成否
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean LoadBGM(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
            return false;
        }

        ReleaseMediaPlayer();

        // インタンスを生成
        arrayMediaPlayer = new MediaPlayer[] { new MediaPlayer(), new MediaPlayer() };

        //音楽ファイル名, あるいはパス
        String filePath = "am_white.mp3";

        // assetsから mp3 ファイルを読み込み
        try(AssetFileDescriptor afdescripter = getAssets().openFd(filePath))
        {
            Log.v("テスト", "[LoadBGM:try]");
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // MediaPlayerに読み込んだ音楽ファイルを指定
            for(int i = 0; i < arrayMediaPlayer.length; i++){
                arrayMediaPlayer[i].setLooping(true);
                arrayMediaPlayer[i].setDataSource(afdescripter.getFileDescriptor(),
                        afdescripter.getStartOffset(),
                        afdescripter.getLength());
                arrayMediaPlayer[i].prepare();
            }
        } catch (IOException e1) {
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
        this.musicLength = this.arrayMediaPlayer[this.playNumber].getDuration();
            // [MediaPlayer.getDuration()] = 読み込んだファイルの全体時間を取得 のはず
        this.playTime = 0;

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

        // TODO:そもそもBGM止めたとしても完全に開放する必要ない
        this.arrayMediaPlayer[this.playNumber].pause();

        /*
        this.preTime = 0;
        this.musicLength = 0;
        this.playTime = 0;
        this.playNumber = 0;

        // foreachからnullクリアする場合はコピーされた[_media]がクリアされるだけで
        // 実際の配列内のメディアプレイヤーはnullクリアされてなかった
        // 明示的に配列指定でのnullクリアでなければnullにならなかった
        for(int i = 0; i < arrayMediaPlayer.length; i++){
            if(arrayMediaPlayer[i] == null) { continue; }
            arrayMediaPlayer[i].stop();      // 再生終了
            arrayMediaPlayer[i].reset();     // リセット
            arrayMediaPlayer[i].release();   // リソースの解放
            arrayMediaPlayer[i] = null;
        }
        */
    }
}

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

public class MainActivity extends AppCompatActivity implements Runnable {

    private SimpleDateFormat dataFormat =
            new SimpleDateFormat("mm:ss.SS", Locale.JAPAN);

    private Handler m_handler;
    private MediaPlayer mediaPlayer;
    private MediaPlayer[] arrayMediaPlayer;

    private TextView loopPointStartText;
    private TextView loopPointEndText;

    private SeekBar loopPointStartSeekBar;
    private SeekBar loopPointEndSeekBar;

    //
    // ループポイント設定に必要最低限のBGM長(ms)
    private int MUSIC_LENGTH_MIN = 5000;

    // ループポイントの始点・終点設定に必要な間隔(ms)
    private int LOOP_POINT_INTERVAL = 1000;

    // 更新処理の間隔(ms)
    private int updateIntarval = 5;
    // ループポイント(終点)の受付猶予(ms)
    private int receptionEndPoint = 30;

    private int loopPointStart = 5943; // ms
    private int loopPointEnd = 10943; // ms
    private long preTime;
    private int musicLength;
    private int playTime;
    private int playNumber = 0;

    private int prevProgressStart = 0;
    private int prevProgressEnd = 0;
    ///

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_handler = new Handler();
        m_handler.postDelayed(this, this.updateIntarval);
        setContentView(R.layout.activity_main);

        // ループポイントの時間
        loopPointStartText = findViewById(R.id.loopPointStart);
        loopPointStartText.setText(dataFormat.format(loopPointStart));
        loopPointEndText = findViewById(R.id.loopPointEnd);
        loopPointEndText.setText(dataFormat.format(loopPointEnd));

        // ループポイントの時間
        loopPointStartSeekBar = findViewById(R.id.seekBarStart);
        loopPointEndSeekBar = findViewById(R.id.seekBarEnd);

        loopPointStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //

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
    }

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
        boolean _runEnd = (this.arrayMediaPlayer == null);
        if(!_runEnd){ _runEnd = (this.arrayMediaPlayer[this.playNumber] == null); }
        if(!_runEnd){ _runEnd = (this.musicLength <= 0); }
        if(_runEnd) {
            m_handler.postDelayed(this, this.updateIntarval);
            return;
        }

        int prg = loopPointStartSeekBar.getProgress();
        Log.v("テスト", "[LoopPointStart] = " + this.musicLength + " / " + prg);
        loopPointStartText.setText(dataFormat.format(this.musicLength / prg));

        this.playTime += System.currentTimeMillis() - this.preTime;

        if((this.loopPointEnd - this.receptionEndPoint) <= this.playTime){
            Log.v("テスト", "[" + this.playNumber + "] = " + this.loopPointEnd + " / " + this.playTime);

//                mediaPlayer.seekTo(5943);
//                this.arrayMediaPlayer[this.playNumber].seekTo(5943);

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

    // ループポイントを更新できるか確認
    private void UpdateLoopPoint(){
        if(this.musicLength <= this.MUSIC_LENGTH_MIN){ return; }

        int _max = loopPointStartSeekBar.getMax();
        int _nowStart = loopPointStartSeekBar.getProgress();
        int _nowEnd = loopPointEndSeekBar.getProgress();
        int _difference = Math.abs(_nowEnd - _nowStart);

        // ループポイントの始点・終点の間隔が短すぎる場合前回の値に戻す
        if(_difference <= this.LOOP_POINT_INTERVAL){
            this.loopPointStartSeekBar.setProgress(this.prevProgressStart);
            this.loopPointEndSeekBar.setProgress(this.prevProgressEnd);
        }

        this._updateLoopPoint(this.loopPointStartSeekBar, this.loopPointStartText);
        this._updateLoopPoint(this.loopPointEndSeekBar, this.loopPointEndText);
    }

    // シークバーを動かした場合のループポイント更新
    private void _updateLoopPoint(SeekBar seekBar, TextView textView){
        int _now = seekBar.getProgress();
        if(_now == 0){
            Log.v("テスト", "[LoopPointStart] = " + this.musicLength + " / " + _now);
            textView.setText(dataFormat.format(_now));
            return;
        }

        int _max = seekBar.getMax();
        int _value = this.musicLength * (_now / _max);
        Log.v("テスト", "[LoopPointStart] = " + this.musicLength + " / " + _value);
        textView.setText(dataFormat.format(_value));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean audioSetup(){
        boolean fileCheck = false;

        // インタンスを生成
/*
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);
*/
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
/*
            mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
                    afdescripter.getStartOffset(),
                    afdescripter.getLength());
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
*/

            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return fileCheck;
    }

    private void audioPlay() {

        Log.v("テスト", "playNumber:" + this.playNumber);

//        if (mediaPlayer == null) {
        boolean _mediaUnset = true;
        if (this.arrayMediaPlayer != null) {
            _mediaUnset = false;
            for(MediaPlayer _media : arrayMediaPlayer){

                Log.v("テスト", " _media:" + (_media != null));

                if(_media != null){ continue; }
                _mediaUnset = true;
                break;
            }
        }
        if (_mediaUnset) {
            // audio ファイルを読出し
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (audioSetup()){
                    Toast.makeText(getApplication(), "Rread audio file", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        else{
/*
            // 繰り返し再生する場合
            mediaPlayer.stop();
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();
*/
            Log.v("テスト", " arrayMediaPlayer[" + this.playNumber + "]:" + (this.arrayMediaPlayer[this.playNumber] != null));

            // TODO:ここいらないのでは？
            /*
            // 繰り返し再生する場合
            this.arrayMediaPlayer[this.playNumber].stop();
            this.arrayMediaPlayer[this.playNumber].reset();
            // リソースの解放
            this.arrayMediaPlayer[this.playNumber].release();
            */
        }

        // 再生する
//        mediaPlayer.start();
        this.arrayMediaPlayer[this.playNumber].start();

        for(MediaPlayer _media : arrayMediaPlayer){
            if(_media == this.arrayMediaPlayer[this.playNumber]) { continue; }

            _media.seekTo(this.loopPointStart);
        }

        //
        this.preTime = System.currentTimeMillis();
//        this.musicLength = mediaPlayer.getDuration();
        this.musicLength = this.arrayMediaPlayer[this.playNumber].getDuration();
            // [MediaPlayer.getDuration()] = 読み込んだファイルの全体時間を取得 のはず
        this.playTime = 0;
        //

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

    private void audioStop() {

        //
        this.preTime = 0;
        this.musicLength = 0;
        this.playTime = 0;
        this.playNumber = 0;
        //
/*
        // 再生終了
        mediaPlayer.stop();
        // リセット
        mediaPlayer.reset();
        // リソースの解放
        mediaPlayer.release();

        mediaPlayer = null;
*/
        // foreachからnullクリアする場合はコピーされた[_media]がクリアされるだけで
        // 実際の配列内のメディアプレイヤーはnullクリアされてなかった
        // 明示的に配列指定でのnullクリアでなければnullにならなかった
        for(MediaPlayer _media : arrayMediaPlayer){
            if(_media == null) { continue; }
            _media.stop();      // 再生終了
            _media.reset();     // リセット
            _media.release();   // リソースの解放
            _media = null;
        }
        Log.v("テスト", "MediaPlayer Stop / arrayMediaPlayer = " + (arrayMediaPlayer == null));
        for(MediaPlayer _media : arrayMediaPlayer){
            Log.v("テスト", "  _media = " + (_media == null));
        }


        for(int i = 0; i < arrayMediaPlayer.length; i++){
            if(arrayMediaPlayer[i] == null) { continue; }
            arrayMediaPlayer[i] = null;
        }
        Log.v("テスト", "MediaPlayer Stop / arrayMediaPlayer = " + (arrayMediaPlayer == null));
        for(MediaPlayer _media : arrayMediaPlayer){
            Log.v("テスト", "  _media = " + (_media == null));
        }
    }
}

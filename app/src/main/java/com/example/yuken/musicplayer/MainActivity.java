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
import android.content.res.AssetFileDescriptor;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Runnable {

    // 更新処理の間隔(ms)
    private int updateIntarval = 1;
    // ループポイント(終点)の受付猶予(ms)
    private int receptionEndPoint = 30;

    private Handler m_handler;
    private MediaPlayer mediaPlayer;

    private long preTime;
    private int musicLength;
    private int playTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_handler = new Handler();
        m_handler.postDelayed(this, this.updateIntarval);
        setContentView(R.layout.activity_main);

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
            if (mediaPlayer != null) {
                // 音楽停止
                audioStop();
            }
            }
        });
    }

    @Override
    public void run() {

        // ここで毎フレームの処理
//        Log.v("テスト", "これはメッセージです「～」終わり");
        if (mediaPlayer == null) {
            Log.v("テスト", "mediaPlayer == null");
            m_handler.postDelayed(this, this.updateIntarval);
            return;
        }

        if(0 < this.musicLength){
            this.playTime += System.currentTimeMillis() - this.preTime;

            if((15943 - this.receptionEndPoint) <= this.playTime){
                Log.v("テスト", "これはメッセージです「" + this.musicLength + " / " + this.playTime + " / " + this.preTime + "」終わり");

                mediaPlayer.seekTo(5943);
                this.playTime = 5943;
            }
            this.preTime = System.currentTimeMillis();
        }

        m_handler.postDelayed(this, this.updateIntarval);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean audioSetup(){
        boolean fileCheck = false;

        // インタンスを生成
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);

        //音楽ファイル名, あるいはパス
        String filePath = "am_white.mp3";

        // assetsから mp3 ファイルを読み込み
        try(AssetFileDescriptor afdescripter = getAssets().openFd(filePath))
        {
            // MediaPlayerに読み込んだ音楽ファイルを指定
            mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
                    afdescripter.getStartOffset(),
                    afdescripter.getLength());
            // 音量調整を端末のボタンに任せる
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();

            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return fileCheck;
    }

    private void audioPlay() {

        if (mediaPlayer == null) {
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
            // 繰り返し再生する場合
            mediaPlayer.stop();
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();
        }

        // 再生する
        mediaPlayer.start();

        //
        this.preTime = System.currentTimeMillis();
        this.musicLength = mediaPlayer.getDuration();
            // [MediaPlayer.getDuration()] = 読み込んだファイルの全体時間を取得 のはず
        this.playTime = 0;
        //

        // 終了を検知するリスナー
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
            Log.d("debug","end of audio");
            audioStop();
            }
        });
    }

    private void audioStop() {
        // 再生終了
        mediaPlayer.stop();
        // リセット
        mediaPlayer.reset();
        // リソースの解放
        mediaPlayer.release();

        //
        this.preTime = 0;
        this.musicLength = 0;
        this.playTime = 0;
        //

        mediaPlayer = null;
    }
}

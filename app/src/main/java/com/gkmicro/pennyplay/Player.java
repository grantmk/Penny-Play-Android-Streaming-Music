package com.gkmicro.pennyplay;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Created by grant on 08/02/2016.
 */
public class Player {

    String url = "";
    MediaPlayer mediaPlayer = new MediaPlayer();
    public static Player player;

    public Player () {
        this.player = this;
    }

    public void playStream (String url) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    playPlayer();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    MainActivity.flipPlayPauseButton(false);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pausePlayer () {
        try {
            mediaPlayer.pause();
            MainActivity.flipPlayPauseButton(false);
        }
        catch (Exception e){
            Log.d("EXCEPTION", "failed to pause mediaPlayer");
        }
    }

    public void playPlayer () {
        try {
            mediaPlayer.start();
            MainActivity.flipPlayPauseButton(true);
        }
        catch (Exception e){
            Log.d("EXCEPTION", "failed to start mediaPlayer");
        }
    }

    public void togglePlayer () {
        try {
            if(mediaPlayer.isPlaying())
                pausePlayer();
            else
                playPlayer();
        }
        catch (Exception e){
            Log.d("EXCEPTION", "failed to toggle mediaPlayer");
        }
    }
}











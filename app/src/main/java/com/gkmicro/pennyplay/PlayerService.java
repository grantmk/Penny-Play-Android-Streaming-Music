package com.gkmicro.pennyplay;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.os.Handler;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PlayerService extends Service {

    MediaPlayer mediaPlayer = new MediaPlayer();

    private final IBinder mBinder = new MyBinder();

    public class MyBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }

    public PlayerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getStringExtra("url") != null)
            playStream(intent.getStringExtra("url"));
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            showNotification();
            Toast.makeText(this, "Player Started!", Toast.LENGTH_SHORT).show();

        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
            Toast.makeText(this, "Last Track", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            Toast.makeText(this, "Pause / Play", Toast.LENGTH_SHORT).show();
            togglePlayer();
        } else if (intent.getAction().equals(Constants.ACTION.NEXT_ACTION)) {
            Toast.makeText(this, "Next Track", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i("info", "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
         //       | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent previousIntent = new Intent(this, PlayerService.class);
        previousIntent.setAction(Constants.ACTION.PREV_ACTION);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent nextIntent = new Intent(this, PlayerService.class);
        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
        PendingIntent pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.penny);

        int playPauseId = android.R.drawable.ic_media_play;
        if (mediaPlayer.isPlaying())
            playPauseId = android.R.drawable.ic_media_pause;

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("TutorialsFace Music Player")
                .setTicker("TutorialsFace Music Player")
                .setContentText("My song")
                .setSmallIcon(R.drawable.penny)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous, "Previous",
                        ppreviousIntent)
                .addAction(playPauseId, "Play",
                        pplayIntent)
                .addAction(android.R.drawable.ic_media_next, "Next",
                        pnextIntent).build();
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                notification);
    }

    public void playStream (String url) {
        if(mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            }
            catch (Exception e){

            }
            mediaPlayer = null;
        }
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
                    flipPlayPauseButton(false);
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
            flipPlayPauseButton(false);
            showNotification();
        }
        catch (Exception e){
            Log.d("EXCEPTION", "failed to pause mediaPlayer");
        }
    }

    public void playPlayer () {
        try {
            getAudioFocusAndPlay();
            flipPlayPauseButton(true);
            showNotification();
        }
        catch (Exception e){
            Log.d("EXCEPTION", "failed to start mediaPlayer");
        }
    }

    private AudioManager am;
    private boolean playingBeforeInterruption = false;
    //private boolean mReceiverRegistered = false;

    public void getAudioFocusAndPlay () {
        am = (AudioManager) this.getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        // Request audio focus for playback
        int result = am.requestAudioFocus( afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.start();
            registerReceiver(noisyAudioStreamReceiver, intentFilter);
        }
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        // Pause playback
                        pausePlayer();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // Resume playback
                        playPlayer();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        Log.i("info", "focus changed");
                        pausePlayer();
                        am.abandonAudioFocus(afChangeListener);
                        // Stop playback
                    }
                }
            };

    public void flipPlayPauseButton (boolean isPlaying) {
        //code to communicate with our main thread via 'Broadcasts'
        Intent intent = new Intent("changePlayButton");
        //add data
        intent.putExtra("isPlaying", isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //noisy earhones
    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
            }
        }
    }

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private NoisyAudioStreamReceiver noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

}

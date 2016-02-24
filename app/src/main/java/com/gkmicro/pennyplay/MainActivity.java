package com.gkmicro.pennyplay;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    static FloatingActionButton playPauseButton;
    PlayerService mBoundService;
    boolean mServiceBound = false;

    List<Song> songs= new ArrayList<>();

    ListView listView;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.MyBinder myBinder = (PlayerService.MyBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        playPauseButton = (FloatingActionButton) findViewById(R.id.fab);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mServiceBound)
                    mBoundService.togglePlayer();
            }
        });
        listView = (ListView) findViewById(R.id.listView);
        fetchSongsFromWeb();
    }

    private void startStreamingService (String url) {
        Intent i = new Intent(this, PlayerService.class);
        i.putExtra("url", url);
        i.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(i);
        bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void flipPlayPauseButton (boolean isPlaying) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("changePlayButton"));
    }

    // handler for received Intents for the button change event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
            flipPlayPauseButton(isPlaying);
        }
    };

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void fetchSongsFromWeb () {
        Log.i("INFO", "geting songs");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL("http://learnappdevelopment.com/music_app/getmusic.php");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");


                    int statusCode = urlConnection.getResponseCode();
                    /* 200 represents HTTP OK */
                    if (statusCode ==  200) {
                        inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        String response = convertInputStreamToString(inputStream);
                        Log.i("GOT RESPONSE", response);
                        parseIntoSongs(response);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));

        String line = "";
        String result = "";

        while((line = bufferedReader.readLine()) != null){
            result += line;
        }

        if(null!=inputStream){
            inputStream.close();
        }

        return result;
    }

    private void parseIntoSongs (String data) {

        String[] dataArray = data.split("\\*");

        int i;

        for (i = 0; i < dataArray.length; i++) {
            //split string frther into component parts
            String[] songArray = dataArray[i].split(",");
            Song song = new Song(songArray[0], songArray[1], songArray[2], songArray[3]);
            songs.add(song);
        }

        for (i = 0; i < songs.size(); i++) {
            Log.i("GOT SONG", songs.get(i).getTitle());
        }

        populateListView();
    }

    private void populateListView () {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Getting adapter by passing xml data ArrayList
                SongListAdapter adapter = new SongListAdapter(MainActivity.this, songs);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Song song = songs.get(position);
                        String songAddress = "http://learnappdevelopment.com/music_app/" + song.getTitle();
                        startStreamingService(songAddress);
                        markSongPlayed(song.getId());
                        askForLikes(song);
                    }
                });
            }
        });
    }


    public void markSongPlayed (final int id) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL("http://learnappdevelopment.com/music_app/add_play.php?id=" + Integer.toString(id));
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");


                    int statusCode = urlConnection.getResponseCode();
                    /* 200 represents HTTP OK */
                    if (statusCode ==  200) {
                        inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        String response = convertInputStreamToString(inputStream);
                        Log.i("played song id: ", response);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    public void askForLikes (final Song song) {
        new AlertDialog.Builder(this)
                .setTitle(song.getTitle())
                .setMessage("Do you like ths song?")
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        likeSong(song.getId());
                    }
                })
                .setNegativeButton("No :(", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void likeSong (final int id) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL("http://learnappdevelopment.com/music_app/add_like.php?id=" + Integer.toString(id));
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.getResponseCode();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }
}








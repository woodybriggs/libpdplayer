package uk.co.woodybriggs.libpdport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ListView;
import android.os.Environment;
import android.widget.ArrayAdapter;


public class MainActivity extends ListActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private int patch;

    private static final String TAG = "Pd Test";

    private PdService pdService = null;

    private Toast toast = null;

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
                }
                toast.setText(TAG + ": " + msg);
                toast.show();
            }
        });
    }

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            initPd(Environment.getExternalStorageDirectory().getPath() + "/Music/" + "osc.pd");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // this method will never be called
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);

        updateListView();

        playPause = (ImageButton) findViewById(R.id.PlayPause);
        playPause.setImageResource(R.drawable.play);
    };

    List<String> songs = new ArrayList<String>();
    ImageButton playPause;

    private void updateListView() {

        File home = new File(Environment.getExternalStorageDirectory().getPath() + "/Music/");
        if (home.listFiles(new PdFilter()).length > 0) {
            for (File file : home.listFiles(new PdFilter())) {
                songs.add(file.getName());
            }
        }

        ArrayAdapter<String> songList = new ArrayAdapter<String>(this, R.layout.song_item, songs);

        ListView lv = (ListView) findViewById(android.R.id.list);

        try {
            lv.setAdapter(songList);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {

        initPd(Environment.getExternalStorageDirectory().getPath() + "/Music/" + songs.get(position));
        startAudio();
        setPlayButton(true);
    }

    public void playPauseAction(View view) {
        if (pdService.isRunning()) {
            stopAudio();
            setPlayButton(false);
        } else {
            startAudio();
            setPlayButton(true);
        }
    }

    public void setPlayButton(boolean state) {
        if (state == true) {
            playPause.setImageResource(R.drawable.pause);
        } else {
            playPause.setImageResource(R.drawable.play);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (pdService.isRunning()) {
            startAudio();
        }
    }

    private void initPd(String path) {

        try {
            PdBase.closePatch(patch);
            patch = PdBase.openPatch(path);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            finish();
        }
    }

    private void startAudio() {
        String name = getResources().getString(R.string.app_name);
        try {
            pdService.initAudio(-1, -1, -1, -1);   // negative values will be replaced with defaults/preferences
            pdService.startAudio(new Intent(this, MainActivity.class), R.drawable.icon, name, "Return to " + name + ".");
        } catch (IOException e) {
            toast(e.toString());
        }
    }

    private void stopAudio() {
        pdService.stopAudio();
    }

    private void cleanup() {
        try {
            unbindService(pdConnection);
        } catch (IllegalArgumentException e) {
            // already unbound
            pdService = null;
        }
    }

}




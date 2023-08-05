package fengzihuachuan.capybara;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import fengzihuachuan.capybara.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    static String TAG = "MainActivity";

    /* 注意workmode和OptionsMenu中的序列相关联 */
    static final public int WORKMODE_REPEAT = 0;
    static final public int WORKMODE_PLAY = 1;
    static final public int WORKMODE_EDIT = 2;
    static public int workmode = WORKMODE_REPEAT;

    static SharedPreferences sharedPref = null;
    public int lastPos;
    public String lastFile;

    static public Point screenPoint;

    private ActivityMainBinding binding;

    public VideoPlayer videoPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        screenPoint = new Point();
        defaultDisplay.getSize(screenPoint);

        setSupportActionBar(binding.toolbar);
        binding.fab.setOnClickListener(fabOnClickListener);

        checkPermission();

        FileUtils.init();
        videoPlayer = new VideoPlayer();

        resumeLast();
    }

    private void resumeLast() {
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        lastPos = sharedPref.getInt("lastPos", 0);
        lastFile = sharedPref.getString("lastFile", null);
        Log.i(TAG, "lastFile == " + lastFile);

        File lf = null;
        try {
            lf = new File(FileUtils.getResPath(lastFile, FileUtils.SearchT_VIDEOPATH));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (lastFile != null && lf != null && lf.exists()) {
            loadVideo(lastFile, lastPos);
        }
    }

    private void loadVideo(String vidfile, int pos) {
        String t = vidfile.substring(0, Math.min(vidfile.length(), 36)) + "...";
        ((TextView)findViewById(R.id.videoname)).setText(t);

        videoPlayer.init(MainActivity.this, findViewById(R.id.videosfc), vidfile);
        ListViewAdapter.initSubtitle(MainActivity.this, vidfile, findViewById(R.id.subtitlelist), videoPlayer);

        if (pos != 0) {
            Message msg = new Message();
            msg.what = lastPos;
            ListViewAdapter.listHandler.sendMessage(msg);
        }

        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("lastFile", vidfile);
            editor.apply();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        videoPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (VideoPlayer.isSurfaceCreated) {
                    FileUtils.init();
                    resumeLast();
                }
            }
        }, 10);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        videoPlayer.release();
    }

    public Handler screenHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    };
    public Handler subtitleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ListViewAdapter.freshSubtitle();
        }
    };
    public Handler playHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ProgressBar progressbar = findViewById(R.id.Progressbar);
            TextView timer = findViewById(R.id.timer);
            int current = msg.what;
            progressbar.setProgress(current * 100 / videoPlayer.getDuration());
            timer.setText(TimeFmt.strFromMs(current));
        }
    };

    public void checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }

            return;
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean isGranted = true;
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //如果没有写sd卡权限
                isGranted = false;
            }
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            Log.i(TAG, "isGranted == " + isGranted);
            if (!isGranted) {
                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission
                                .ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        102);
            }
        }
    }

    private View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("请选择文件");

            final String[] filesShow =  new String[FileUtils.fileslist.size()];
            for(int i = 0; i < FileUtils.fileslist.size(); i++) {
                String t = FileUtils.fileslist.get(i).videoSubDir + "  " + FileUtils.fileslist.get(i).videoName;
                filesShow[i] = t.substring(0, Math.min(t.length(), 36)) + "...";
            }

            builder.setSingleChoiceItems(filesShow, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String vfile = FileUtils.fileslist.get(which).videoName;
                    if (vfile == null) {
                        Toast.makeText(getApplicationContext(), "Files.VideoFiles NULL", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getApplicationContext(), vfile, Toast.LENGTH_LONG).show();
                        dialog.dismiss();

                        loadVideo(vfile, 0);
                    }
                }
            });
            builder.show();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Menu.FIRST + 0, 0, "1").setIcon(R.drawable.ic_launcher_background);
        menu.add(Menu.NONE, Menu.FIRST + 1, 0, "1").setIcon(R.drawable.ic_launcher_background);
        menu.add(Menu.NONE, Menu.FIRST + 2, 0, "1").setIcon(R.drawable.ic_launcher_background);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String[] m = {"复读模式", "播放模式", "编辑模式"};
        for (int i = 0; i < menu.size(); i++) {
            if (workmode == i) {
                menu.getItem(i).setTitle("> " + m[i]);
            } else {
                menu.getItem(i).setTitle(m[i]);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        LinearLayout ibarlyt = findViewById(R.id.ibarlyt);
        Log.i(TAG, "ibarlyt == " + ibarlyt);

        if (id == Menu.FIRST + 0) {
            workmode = WORKMODE_REPEAT;
            Toast.makeText(getApplicationContext(), "进入 复读模式", Toast.LENGTH_LONG).show();
            ibarlyt.setVisibility(View.GONE);
        } else if (id == Menu.FIRST + 1) {
            workmode = WORKMODE_PLAY;
            Toast.makeText(getApplicationContext(), "进入 播放模式", Toast.LENGTH_LONG).show();
            ibarlyt.setVisibility(View.GONE);
        } else if (id == Menu.FIRST + 2) {
            workmode = WORKMODE_EDIT;
            Toast.makeText(getApplicationContext(), "进入 编辑模式", Toast.LENGTH_LONG).show();
            ibarlyt.setVisibility(View.VISIBLE);
        }

        return super.onOptionsItemSelected(item);
    }
}
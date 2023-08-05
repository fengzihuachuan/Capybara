package fengzihuachuan.capybara;

import static java.lang.Thread.sleep;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class VideoPlayer implements SurfaceHolder.Callback,
        View.OnTouchListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener
{
    static String TAG = "VideoPlayer";

    static MainActivity main;
    static String videopath;
    SurfaceView videoSuf;
    static SurfaceHolder surfaceHolder;
    static MediaPlayer mPlayer = null;

    static Boolean isSurfaceCreated = false;

    static int startPt = 0, endPt = 0;

    void init(MainActivity m, SurfaceView sfv, String vp) {
        main = m;
        videopath = FileUtils.getResPath(vp, FileUtils.SearchT_VIDEOPATH);
        videoSuf = sfv;

        videoSuf.setOnTouchListener(this);

        surfaceHolder = videoSuf.getHolder();
        surfaceHolder.addCallback(this);

        try {
            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }

            Log.e(TAG, "returns: " + videopath);
            Uri playUri = Uri.parse(videopath);
            mPlayer = MediaPlayer.create(main, playUri);

            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnInfoListener(this);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnSeekCompleteListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);

            mPlayer.setSurface(videoSuf.getHolder().getSurface());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
        }
    }

    public int getDuration() {
        return mPlayer.getDuration();
    }

    public void play(int start, int end) {
        startPt = start;
        endPt = end;
        try {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                sleep(100);
            }

            mPlayer.setDisplay(surfaceHolder);
            mPlayer.seekTo(start, MediaPlayer.SEEK_CLOSEST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "play - start: " + TimeFmt.strFromMs(start) + " end: " + TimeFmt.strFromMs(end));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId() == R.id.videosfc) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
            }
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceCreated = false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared: ");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion: ");
        Toast.makeText(main, "视频播放完毕", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete: ");
        mPlayer.start();

        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Thread: ");

                    // keep screen on
                    Message screenMsg = new Message();
                    screenMsg.what = 1;
                    main.screenHandler.sendMessage(screenMsg);

                    while (mPlayer.isPlaying()) {
                        int current = mPlayer.getCurrentPosition();
                        if (endPt == -1) {
                            ListViewAdapter.playUpdate(current);
                        } else {
                            if (current > endPt) {
                                mPlayer.pause();
                                break;
                            }
                            Message updateIbarMsg = new Message();
                            updateIbarMsg.what = current;
                            main.playHandler.sendMessage(updateIbarMsg);

                        }
                        sleep(100);
                    }

                    // quit keeping screen on
                    screenMsg = new Message();
                    screenMsg.what = 0;
                    main.screenHandler.sendMessage(screenMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {}

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

}


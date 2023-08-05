package fengzihuachuan.capybara;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

import fengzihuachuan.capybara.subtitle.IOClass;
import fengzihuachuan.capybara.subtitle.TimedTextObject;

public class FileUtils {
    static String TAG = "FileUtils";

    static String RootDirName = "CPBRs";

    static String resRootDir;
    static String subtitleDir;

    static ArrayList<FilesInfo> fileslist = new ArrayList<>();

    public static final int SearchT_VIDOE = 0;
    public static final int SearchT_VIDEOPATH = 1;
    public static final int SearchT_SUBT = 2;
    public static final int SearchT_SUBTPATH = 3;

    public static void init() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "getExternalStorageState - ");
            return;
        }
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            resRootDir = sdCard.getCanonicalPath() + "/" + RootDirName +  "/";
            File f = new File(resRootDir);
            f.mkdirs();
            subtitleDir = resRootDir + "subtitles/";
            f = new File(subtitleDir);
            f.mkdirs();
            Log.d(TAG, "init dir - videoDirPath " + resRootDir);
            Log.d(TAG, "init dir - subtitleDirPath " + subtitleDir);

            getVideoFiles();
        } catch (Exception e) {
            Log.e(TAG, "init dir - error");
        }
    }

    public static void saveSub(String subtitle, TimedTextObject tto) {
        String s = subtitleDir + subtitle;
        IOClass.writeFileTxt(s.substring(0, s.lastIndexOf('.')) + ".srt", tto.toSRT());
    }

    public static void getVideoFiles() {
        ArrayList<FilesInfo> unsortflist = new ArrayList<>();
        try {
            File file = new File(resRootDir);
            File[] list = file.listFiles();
            Log.d(TAG, "list - " + list);

            Files.walkFileTree(Paths.get(resRootDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String videoDir = file.toString().substring(0, file.toString().lastIndexOf('/') + 1);
                    String videoName = file.toString().substring(file.toString().lastIndexOf('/') + 1, file.toString().length());
                    if ( videoName.endsWith(".mp4") || videoName.endsWith(".mkv") ||
                            videoName.endsWith(".avi") || videoName.endsWith(".webm")) {

                        //Log.d(TAG, "videoPath - " + videoDir);
                        //Log.d(TAG, "videoName - " + videoName);

                        String basename = videoName.substring(0, videoName.lastIndexOf('.'));
                        String videoSubdir = videoDir.substring(videoDir.lastIndexOf(RootDirName) + RootDirName.length() + 1, videoDir.length());
                        //Log.d(TAG, "basename - " + basename);
                        //Log.d(TAG, "videoSubdir - " + videoSubdir);

                        String sbttName = basename + ".srt";
                        File sf = new File(subtitleDir + sbttName);
                        if (sf.exists()) {
                            //Log.d(TAG, "exists - srt");
                            unsortflist.add(new FilesInfo(videoDir, videoSubdir, videoName, subtitleDir, sbttName));
                        } else {
                            sf = new File(videoDir + sbttName);
                            if (sf.exists()) {
                                //Log.d(TAG, "exists - srt");
                                unsortflist.add(new FilesInfo(videoDir, videoSubdir, videoName, videoDir, sbttName));
                            }
                        }

                        sbttName = basename + ".ass";
                        sf = new File(subtitleDir + sbttName);
                        if (sf.exists()) {
                            //Log.d(TAG, "exists - ass");
                            unsortflist.add(new FilesInfo(videoDir, videoSubdir, videoName, subtitleDir, sbttName));
                        } else {
                            sf = new File(videoDir + sbttName);
                            if (sf.exists()) {
                                //Log.d(TAG, "exists - ass");
                                unsortflist.add(new FilesInfo(videoDir, videoSubdir, videoName, videoDir, sbttName));
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            fileslist.clear();
            fileslist = sortList(unsortflist);
            return;
        } catch (Exception e) {
            return;
        }
    }

    private static ArrayList<FilesInfo> sortList(ArrayList<FilesInfo> unsortlist) {
        unsortlist.sort(Comparator.comparing(FilesInfo::getVideoSubDir).thenComparing(FilesInfo::getVideoName));
        return unsortlist;
    }

    public static String getResPath(String resName, int type) {
        if (resName.endsWith(".mp4") || resName.endsWith(".mkv") ||
                resName.endsWith(".avi") || resName.endsWith(".webm")) {
            for (FilesInfo fi : fileslist) {
                if (resName.equals(fi.videoName)) {
                    if (type == SearchT_VIDOE) {
                        return fi.videoName;
                    } else if (type == SearchT_VIDEOPATH) {
                        return fi.videoDir + fi.videoName;
                    } else if (type == SearchT_SUBT) {
                        return fi.sbtName;
                    } else if (type == SearchT_SUBTPATH) {
                        return fi.sbtDir + fi.sbtName;
                    } else {
                        return null;
                    }
                }
            }
        } else if (resName.endsWith(".ass") || resName.endsWith(".srt")) {
            for (FilesInfo fi : fileslist) {
                if (resName.equals(fi.sbtName)) {
                    if (type == SearchT_VIDOE) {
                        return fi.videoName;
                    } else if (type == SearchT_VIDEOPATH) {
                        return fi.videoDir + fi.videoName;
                    } else if (type == SearchT_SUBT) {
                        return fi.sbtName;
                    } else if (type == SearchT_SUBTPATH) {
                        return fi.sbtDir + fi.sbtName;
                    } else {
                        return null;
                    }
                }
            }
        } else {
            return null;
        }

        return null;
    }

    static public class FilesInfo {
        String videoDir;
        String videoSubDir;
        String videoName;
        String sbtDir;
        String sbtName;

        FilesInfo(String vd, String vsd, String v, String sd, String s) {
            videoDir = vd;
            videoSubDir = vsd;
            videoName = v;
            sbtDir = sd;
            sbtName = s;
        }

        String getVideoSubDir() {
            return videoSubDir;
        }

        String getVideoName() {
            return videoName;
        }
    }
}

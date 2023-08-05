package fengzihuachuan.capybara;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import fengzihuachuan.capybara.subtitle.Caption;
import fengzihuachuan.capybara.subtitle.FormatASS;
import fengzihuachuan.capybara.subtitle.FormatSRT;
import fengzihuachuan.capybara.subtitle.TimedTextFileFormat;
import fengzihuachuan.capybara.subtitle.TimedTextObject;


public class Subtitle {
    static String TAG = "Subtitle";

    static TimedTextObject tto;
    static String subtlPath;
    static String subtlName;

    public static ArrayList<ListItem> subtitleLists = null;

    static ArrayList<ListItem> init(String vfile) {
        if (subtitleLists != null) {
            subtitleLists.clear();
        }
        subtitleLists = new ArrayList<ListItem>();

        subtlPath = FileUtils.getResPath(vfile, FileUtils.SearchT_SUBTPATH);
        subtlName = FileUtils.getResPath(vfile, FileUtils.SearchT_SUBT);

        File file = new File(subtlPath);
        try {
            //Log.d(TAG, "ttff file: " + file);

            TimedTextFileFormat ttff;
            if (subtlPath.substring(subtlPath.lastIndexOf('.')).equals(".srt")) {
                ttff = new FormatSRT();
                //Log.d(TAG, "ttff : FormatSRT");
            } else {
                ttff = new FormatASS();
                //Log.d(TAG, "ttff : FormatASS");
            }


            InputStream is = new FileInputStream(file);
            tto = ttff.parseFile(subtlPath, is);
            is.close();

            for (Integer key : tto.captions.keySet()) {
                //Log.d(TAG, "key: " + key);
                //Log.d(TAG, "value: " + tto.captions.get(key));
                tto.captions.get(key).content = tto.captions.get(key).content.replaceAll("<[^>]+>", ""); //删除html标签
                ListItem i = new ListItem(key, tto.captions.get(key).start, 0, tto.captions.get(key).content, tto.captions.get(key).end, 0);
                subtitleLists.add(i);
            }
            return subtitleLists;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void edit(int idx, String text, String start, String end) {
        tto.captions.get(subtitleLists.get(idx).getKey()).content = text;

        subtitleLists.get(idx).setSubContent(text);
        subtitleLists.get(idx).setBackward(TimeFmt.timeFromStr(start).getMseconds() - subtitleLists.get(idx).getSubStart().getMseconds());
        subtitleLists.get(idx).setForward(TimeFmt.timeFromStr(end).getMseconds() - subtitleLists.get(idx).getSubEnd().getMseconds());
    }

    static void edit(int idx, String text, int start, int end) {
        tto.captions.get(subtitleLists.get(idx).getKey()).content = text;

        subtitleLists.get(idx).setSubContent(text);
        subtitleLists.get(idx).setBackward(start - subtitleLists.get(idx).getSubStart().getMseconds());
        subtitleLists.get(idx).setForward(end - subtitleLists.get(idx).getSubEnd().getMseconds());
    }

    static void clean() {
        for (ListItem i : subtitleLists) {
            i.setBackward(0);
            i.setForward(0);
        }
    }

    static void delete(int idx) {
        tto.captions.remove(subtitleLists.get(idx).getKey());
        subtitleLists.remove(subtitleLists.get(idx));
    }

    static void add(int idx) {
        Caption cap = new Caption();
        cap.start = tto.captions.get(subtitleLists.get(idx).getKey()).end;
        cap.end = tto.captions.get(subtitleLists.get(idx+1).getKey()).start;
        cap.content = "";
        cap.region = null;
        cap.style = null;
        tto.captions.put(cap.start.getMseconds(), cap);
        FileUtils.saveSub(subtlName, tto);

        int key = cap.start.getMseconds();
        subtitleLists.add(idx + 1, new ListItem(key, cap.start, 0, "", cap.end, 0));
    }

    static void add(int idx, String content, int start, int end) {
        Caption cap = new Caption();
        cap.start = TimeFmt.timeFromMs(start);
        cap.end = TimeFmt.timeFromMs(end);
        cap.content = content;
        cap.region = null;
        cap.style = null;
        tto.captions.put(cap.start.getMseconds(), cap);
        FileUtils.saveSub(subtlName, tto);

        int key = cap.start.getMseconds();
        subtitleLists.add(idx + 1, new ListItem(key, cap.start, 0, cap.content, cap.end, 0));
    }

    static void save() {
        for (ListItem i : subtitleLists) {
            if (subtitleLists.indexOf(i) != 0) {
                int thisStart = i.getSubStart().getMseconds() + i.getBackward();
                int lastEnd = subtitleLists.get(subtitleLists.indexOf(i) - 1).getSubEnd().getMseconds() + subtitleLists.get(subtitleLists.indexOf(i) - 1).getForward();
                if (thisStart < lastEnd) {
                    i.setBackward(lastEnd - thisStart);
                }
            }
        }

        for (ListItem i : subtitleLists) {
            if (i.getBackward() != 0) {
                //Log.d(TAG, "getBackward " + i.getBackward());
                tto.captions.get(i.getKey()).start.setMseconds(tto.captions.get(i.getKey()).start.getMseconds() + i.getBackward());
                i.setBackward(0);
            }
            if (i.getForward() != 0) {
                //Log.d(TAG, "getForward " + i.getForward());
                tto.captions.get(i.getKey()).end.setMseconds(tto.captions.get(i.getKey()).end.getMseconds() + i.getForward());
                i.setForward(0);
            }
        }

        FileUtils.saveSub(subtlName, tto);
    }
}

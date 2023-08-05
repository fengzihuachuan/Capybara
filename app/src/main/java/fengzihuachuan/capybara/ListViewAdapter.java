package fengzihuachuan.capybara;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;

import java.util.List;


public class ListViewAdapter extends ArrayAdapter<ListItem> {
    static String TAG = "ListViewAdapter";

    private int resourceId;

    private static int selectIdx = -1;
    private static MainActivity context;
    private static ListViewAdapter listViewAdapter;
    private static ListView listView;
    private static VideoPlayer videoPlayer;

    AlertDialog alertDialog;
    boolean merged;

    public ListViewAdapter(Context ctx, int resourceId1, List<ListItem> listItems) {
        super(context, resourceId1, listItems);
        resourceId = resourceId1;
    }

    public static Handler listHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //ListItem i = Subtitle.subtitleLists.get(msg.what);
            listView.setSelection(msg.what - 1);
            listViewAdapter.setSelectItem(msg.what);
            listViewAdapter.notifyDataSetInvalidated();
        }
    };

    public void setSelectItem(int s) {
        this.selectIdx = s;
    }

    static void initSubtitle(Context ctx, String vfile, ListView lv, VideoPlayer vp) {
        context = (MainActivity)ctx;
        listView = lv;
        videoPlayer = vp;

        Subtitle.init(vfile);

        freshSubtitle();
    }

    static void freshSubtitle() {
        listViewAdapter = new ListViewAdapter(context, R.layout.listview, Subtitle.subtitleLists);
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectIdx = position;

                Message msg = new Message();
                msg.what = selectIdx;
                ListViewAdapter.listHandler.sendMessage(msg);

                if (MainActivity.sharedPref != null) {
                    SharedPreferences.Editor editor = MainActivity.sharedPref.edit();
                    editor.putInt("lastPos", selectIdx);
                    editor.apply();
                }

                ListItem i = Subtitle.subtitleLists.get(selectIdx);
                if (context.workmode == context.WORKMODE_PLAY) {
                    videoPlayer.play(i.getSubStart().getMseconds() + i.getBackward(), -1);
                } else {
                    videoPlayer.play(i.getSubStart().getMseconds() + i.getBackward(), i.getSubEnd().getMseconds() + i.getForward());
                }
            }
        });
    }

    static void playUpdate(int current) {
        if (current > Subtitle.subtitleLists.get(selectIdx).getSubEnd().getMseconds() + Subtitle.subtitleLists.get(selectIdx).getForward()) {
            Message updateSelectMsg = new Message();
            updateSelectMsg.what = selectIdx + 1;
            ListViewAdapter.listHandler.sendMessage(updateSelectMsg);
            selectIdx = selectIdx + 1;
        }
        Message updateIbarMsg = new Message();
        updateIbarMsg.what = current;
        context.playHandler.sendMessage(updateIbarMsg);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;

        ListItem listItem = getItem(position);
        //当用户为第一次访问的时候
        if (convertView == null) {
            //将item_activity布局解析成显示界面
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);
            viewHolder = new ViewHolder();
            //获取item_activity.xml中控件，并将其保存在viewHolder中
            viewHolder.substart = view.findViewById(R.id.substart);
            viewHolder.subcontent = view.findViewById(R.id.subcontent);
            viewHolder.subend = view.findViewById(R.id.subend);
            viewHolder.timelyt = view.findViewById(R.id.timelyt);
            viewHolder.leftlyt = view.findViewById(R.id.leftlyt);
            viewHolder.rightlyt = view.findViewById(R.id.rightlyt);
            viewHolder.contentbnt = view.findViewById(R.id.contentBnt);
            //设置将数据进行缓存
            view.setTag(viewHolder);
        } else {
            //第二次访问直接读取第一次访问使存取的数据
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        //将数据返回到item_activity.xml中的每一个空间中
        viewHolder.substart.setText(listItem.getSubStart().toString());
        viewHolder.subcontent.setText(listItem.getSubContent());
        viewHolder.subend.setText(listItem.getSubEnd().toString());

        if (selectIdx == position) {
            viewHolder.timelyt.setVisibility(View.VISIBLE);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

            if (MainActivity.workmode == MainActivity.WORKMODE_EDIT) {
                viewHolder.contentbnt.setVisibility(View.VISIBLE);
                viewHolder.contentbnt.setOnClickListener(contentbntOnClickListener);

                ((TextView)(context.findViewById(R.id.ibarstart))).setText(listItem.getSubStart().toString());
                ((TextView)(context.findViewById(R.id.ibarend))).setText(listItem.getSubEnd().toString());

                updateIbarView(true, Subtitle.subtitleLists.get(selectIdx).getBackward());
                updateIbarView(false, Subtitle.subtitleLists.get(selectIdx).getForward());

                viewHolder.leftlyt.setOnTouchListener(timelytOnTouchListener);
                viewHolder.rightlyt.setOnTouchListener(timelytOnTouchListener);
            }
        } else {
            viewHolder.timelyt.setVisibility(View.GONE);
            viewHolder.contentbnt.setVisibility(View.GONE);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        }

        return view;
    }

    void updateIbarView(boolean isSubStart, int offsetMs) {
        if (isSubStart) {
            ((TextView) (context).findViewById(R.id.ibarbackward)).setText(String.format("%d", offsetMs));
            String b = TimeFmt.strFromMs(Subtitle.subtitleLists.get(selectIdx).getSubStart().getMseconds() + offsetMs);
            ((TextView) (context).findViewById(R.id.ibarstart)).setText(b);
        } else {
            ((TextView) (context).findViewById(R.id.ibarforward)).setText(String.format("%d", offsetMs));
            String f = TimeFmt.strFromMs(Subtitle.subtitleLists.get(selectIdx).getSubEnd().getMseconds() + offsetMs);
            ((TextView) (context).findViewById(R.id.ibarend)).setText(f);
        }
    }

    /**
     * 计算字符串中某个字符存在的个数
     */
    private static int countTarget(String originStr, String targetStr){
        int res = 0;
        int i = originStr.indexOf(targetStr);
        while (i != -1) {
            i = originStr.indexOf(targetStr,i+1);
            res++;
        }
        return res;
    }

    void diagEdit(String msg) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View editView = layoutInflater.inflate(R.layout.editdiag, null);

        EditText contentEdit = editView.findViewById(R.id.contentedit);
        contentEdit.setText(Subtitle.subtitleLists.get(selectIdx).getSubContent());

        EditText startEdit = editView.findViewById(R.id.startEdit);
        int start = Subtitle.subtitleLists.get(selectIdx).getSubStart().getMseconds() + Subtitle.subtitleLists.get(selectIdx).getBackward();
        startEdit.setText(TimeFmt.strFromMs(start));

        EditText endEdit = editView.findViewById(R.id.endEdit);
        int end = Subtitle.subtitleLists.get(selectIdx).getSubEnd().getMseconds() + Subtitle.subtitleLists.get(selectIdx).getForward();
        endEdit.setText(TimeFmt.strFromMs(end));

        merged = false;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(msg);
        dialogBuilder.setView(editView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, " content = " + contentEdit.getText().toString());
                Log.d(TAG, " start = " + startEdit.getText().toString() + "0");
                Log.d(TAG, " end = " + endEdit.getText().toString() + "0");

                String content = contentEdit.getText().toString().replaceAll("^[| ]*", "").replaceAll("[| ]*$", ""); //清除首尾
                int split = countTarget(content, "|"); //拆分
                Log.d(TAG, " split = " + split);
                if (split == 0) {
                    Subtitle.edit(selectIdx, content, startEdit.getText().toString() + "0", endEdit.getText().toString() + "0");
                } else {
                    int start = TimeFmt.timeFromStr(startEdit.getText().toString() + "0").getMseconds();
                    int end = TimeFmt.timeFromStr(endEdit.getText().toString() + "0").getMseconds();
                    int part = (end - start) / (split + 1);

                    Log.d(TAG, " start = " + start + ", end = " + end);
                    Log.d(TAG, " content = " + content);
                    String[] parts = content.split("\\|");
                    for (int i = 0; i < split + 1; i++) {
                        Log.d(TAG, " parts["+i+"] = " + parts[i]);
                    }

                    Subtitle.edit(selectIdx, parts[0].trim(), start, start + part);
                    for (int i = 1; i < split + 1; i++) {
                        Subtitle.add(selectIdx + i - 1, parts[i].trim(), start + part * i, start + part * (i+1));
                    }
                }

                if (merged) {
                    Subtitle.delete(selectIdx + 1);
                    merged = false;
                }

                Subtitle.save();

                Message msg = new Message();
                (context).subtitleHandler.sendMessage(msg);

                msg = new Message();
                msg.what = selectIdx;
                ListViewAdapter.listHandler.sendMessage(msg);
            }
        });
        dialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        final AlertDialog dialog = dialogBuilder.create();

        Button backBtn = editView.findViewById(R.id.backAlign);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int t = Subtitle.subtitleLists.get(selectIdx - 1).getSubEnd().getMseconds() + Subtitle.subtitleLists.get(selectIdx - 1).getForward();
                startEdit.setText(TimeFmt.strFromMs(t));
            }
        });

        Button forwBtn = editView.findViewById(R.id.forwAlign);
        forwBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int t = Subtitle.subtitleLists.get(selectIdx + 1).getSubStart().getMseconds() + Subtitle.subtitleLists.get(selectIdx + 1).getBackward();
                endEdit.setText(TimeFmt.strFromMs(t));
            }
        });

        Button mergenext = editView.findViewById(R.id.mergenext);
        mergenext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = Subtitle.subtitleLists.get(selectIdx).getSubContent() + " " + Subtitle.subtitleLists.get(selectIdx + 1).getSubContent();
                contentEdit.setText(s);

                int t = Subtitle.subtitleLists.get(selectIdx + 1).getSubEnd().getMseconds() + Subtitle.subtitleLists.get(selectIdx + 1).getForward();
                endEdit.setText(TimeFmt.strFromMs(t));

                merged = true;
            }
        });

        Button delete = editView.findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                diagConfirm(4, "删除当前字幕吗?");
            }
        });

        Button reset = editView.findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentEdit.setText(Subtitle.subtitleLists.get(selectIdx).getSubContent());
                startEdit.setText(TimeFmt.strFromMs(start));
                endEdit.setText(TimeFmt.strFromMs(end));
            }
        });


        dialog.show();
    }

    View.OnClickListener contentbntOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final String[] items = {"保存", "编辑", "增加", "清除"};
            final String[] msgs = {"保存当前全部字幕吗?", "编辑当前字幕 用 | 表示分割", "新增一条字幕吗?", "清除当前的改动吗?"};
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setTitle("请选择操作");
            alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 1) {
                        diagEdit(msgs[i]);
                    } else {
                        diagConfirm(i, msgs[i]);
                    }
                }
            });

            alertDialog = alertBuilder.create();
            alertDialog.show();
        }
    };

    void diagConfirm(int i, String msg) {
        new AlertDialog.Builder(context).setTitle("确认")
                .setMessage(msg)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (i == 0) {
                            Subtitle.save();
                            Message msg = new Message();
                            (context).subtitleHandler.sendMessage(msg);
                        } else if (i == 1) {
                            //diagEdit
                        } else if (i == 2) {
                            Subtitle.add(selectIdx);
                            Message msg = new Message();
                            (context).subtitleHandler.sendMessage(msg);
                        } else if (i == 3) {
                            Subtitle.clean();
                            Message msg = new Message();
                            (context).subtitleHandler.sendMessage(msg);
                        } else if (i == 4) {
                            Subtitle.delete(selectIdx);
                            Message msg = new Message();
                            (context).subtitleHandler.sendMessage(msg);
                        }
                        Message msg = new Message();
                        msg.what = selectIdx;
                        ListViewAdapter.listHandler.sendMessage(msg);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }
                ).show();
    }

    View.OnTouchListener timelytOnTouchListener = new View.OnTouchListener() {
        private float startX, startY, offsetX, offsetY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    offsetX = event.getRawX() - startX;
                    offsetY = event.getRawY() - startY;
                    if (Math.abs(offsetX) > Math.abs(offsetY)) {

                        //Log.d(TAG, "point.x " + point.x);
                        //Log.d(TAG, "startX " + startX);
                        ListItem i = Subtitle.subtitleLists.get(selectIdx);

                        if (startX > MainActivity.screenPoint.x / 2.0) { //right half
                            updateIbarView(false, 10 * (int) offsetX);
                            i.setForward(10 * (int) offsetX);
                        } else { //left half
                            updateIbarView(true, 10 * (int) offsetX);
                            i.setBackward(10 * (int) offsetX);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    class ViewHolder {
        private LinearLayoutCompat timelyt, leftlyt, rightlyt;
        private TextView substart, subcontent, subend;
        private Button contentbnt;
    }
}
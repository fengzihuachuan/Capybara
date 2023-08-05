package fengzihuachuan.capybara;

import fengzihuachuan.capybara.subtitle.Time;

public class ListItem {
    private int key;
    private Time substart;
    private int backward;
    private String subcontent;
    private Time subend;
    private int forward;

    public ListItem(int key, Time substart, int backward, String subcontent, Time subend, int forward) {
        this.key = key;
        this.substart = substart;
        this.backward = backward;
        this.subcontent = subcontent;
        this.subend = subend;
        this.forward = forward;
    }

    public int getKey() {
        return key;
    }

    public Time getSubStart() {
        return substart;
    }

    public int getBackward() {
        return backward;
    }

    public void setBackward(int b) {
        backward = b;
    }

    public String getSubContent() {
        return subcontent;
    }

    public void setSubContent(String s) {
        subcontent = s;
    }

    public Time getSubEnd() {
        return subend;
    }

    public int getForward() {
        return forward;
    }

    public void setForward(int f) {
        forward = f;
    }
}
package com.jikexueyuan.cnote.data;

import cn.bmob.v3.BmobObject;

/**
 * Created by Mr.Z on 2016/4/14 0014.
 */
public class Notes extends BmobObject {

    private MyUser author;

    private String noteTitle;

    private String noteContent;

    public MyUser getAuthor() {
        return author;
    }

    public void setAuthor(MyUser author) {
        this.author = author;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

}

package com.jikexueyuan.cnote.data;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by Mr.Z on 2016/4/18 0018.
 */
public class MediaFile extends BmobObject {

    private BmobFile file;

    private Notes note;

    public MediaFile(){

    }

    public MediaFile(Notes note,BmobFile file){
        this.note = note;
        this.file = file;
    }

    public BmobFile getFile() {
        return file;
    }

    public void setFile(BmobFile file) {
        this.file = file;
    }

    public Notes getNote() {
        return note;
    }

    public void setNote(Notes note) {
        this.note = note;
    }
}

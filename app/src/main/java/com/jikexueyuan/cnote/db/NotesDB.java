package com.jikexueyuan.cnote.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Mr.Z on 2016/4/24 0024.
 */
public class NotesDB extends SQLiteOpenHelper {

    public NotesDB(Context context) {
        super(context, "notes", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE notes(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user TEXT DEFAULT \"\"," +             //   1
                "objectId TEXT DEFAULT \"\"," +         //   2
                "noteTitle TEXT DEFAULT \"\"," +        //   3
                "noteContent TEXT DEFAULT \"\"," +      //   4
                "noteDate TEXT DEFAULT \"\"," +         //   5
                "tag TEXT DEFAULT \"\")");              //   6

        db.execSQL("CREATE TABLE del(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user TEXT DEFAULT \"\"," +
                "delObjId TEXT DEFAULT \"\"," +
                "includeMedia TEXT DEFAULT \"\")");     //用T和F表示待删除的日志是否含有媒体文件


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

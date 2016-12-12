package com.jikexueyuan.cnote.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.adapter.DBAdapter;
import com.jikexueyuan.cnote.data.MyUser;
import com.jikexueyuan.cnote.db.NotesDB;
import com.jikexueyuan.cnote.manager.DataManager;

import cn.bmob.v3.BmobUser;


public class NoteFragment extends Fragment {

    public NoteFragment() {
        // Required empty public constructor
    }

    private ImageButton btnAccount, btnSync;
    private FloatingActionButton fab;
    private ListView noteListView;
    private MyUser currentUser;
    private SQLiteDatabase dbWrite, dbRead;
    private DBAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_note, container, false);

        btnAccount = (ImageButton) root.findViewById(R.id.btnAccount);//登陆后的用户头像

        btnSync = (ImageButton) root.findViewById(R.id.btnRefresh);//同步数据

        noteListView = (ListView) root.findViewById(R.id.noteListView);

        fab = (FloatingActionButton) root.findViewById(R.id.fab);//添加日志

        currentUser = BmobUser.getCurrentUser(getContext(), MyUser.class);//获取当前用户

        NotesDB db = new NotesDB(getContext());
        dbRead = db.getReadableDatabase();
        dbWrite = db.getWritableDatabase();

        Cursor c = dbRead.query(DataManager.DB_NOTES, null,
                "user = ?",
                new String[]{currentUser.getUsername()}, null, null, null);
        adapter = new DBAdapter(getContext(), c);
        noteListView.setAdapter(adapter);

        refreshListView();

        //手动同步数据
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUser != null) {
                    if (DataManager.isNetworkAvailable(getContext())) {
                        //同步
                        DataManager.syncDBData(getContext(), currentUser);
                        //数据传输可能需要时间  设置一个进程对话框 延时2s
                        final ProgressDialog progressDialog = new ProgressDialog(getContext());
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setMessage("正在加载数据...");
                        progressDialog.show();
                        final Handler handler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                if (msg.what == 1) {
                                    refreshListView();
                                }
                            }
                        };
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                progressDialog.dismiss();

                                Message msg = new Message();
                                msg.what = 1;
                                handler.sendMessage(msg);
                            }
                        }.start();
                        //刷新ListView

                    } else {
                        Toast.makeText(getContext(), "网络异常，请检查您的网络！", Toast.LENGTH_LONG).show();
                    }


                }
            }
        });

        //进入用户界面
        btnAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), AccountActivity.class));
            }
        });


        //进入添加日志界面
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), AddActivity.class));
            }
        });


        noteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor cursor = dbRead.query(DataManager.DB_NOTES, null,
                        "user = ?",
                        new String[]{currentUser.getUsername()}, null, null, null);
                cursor.moveToPosition(position);

                Intent i = new Intent(getContext(), NoteActivity.class);
                i.putExtra("localId", cursor.getInt(0));
                i.putExtra("user", cursor.getString(1));
                i.putExtra("objectId", cursor.getString(2));
                i.putExtra("noteTitle", cursor.getString(3));
                i.putExtra("noteContent", cursor.getString(4));
                i.putExtra("tag", cursor.getString(6));
                startActivity(i);
            }
        });

        noteListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                new AlertDialog.Builder(getContext())
                        .setTitle("确认删除？").setMessage("删除后无法恢复")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Cursor cursor = dbRead.query(DataManager.DB_NOTES, null,
                                        "user = ?",
                                        new String[]{currentUser.getUsername()}, null, null, null);
                                cursor.moveToPosition(position);

                                if (cursor.getString(6).equals(DataManager.TAG_UP)) {               //日志未上传
                                    dbWrite.delete(DataManager.DB_NOTES, "_id = ?", new String[]{cursor.getInt(0) + ""});
                                } else {                                                            //已上传的日志

                                    if (DataManager.isNetworkAvailable(getContext())) {             //网络连接可用
                                        //服务器端删除
                                        DataManager.deleteNote(getContext(), cursor.getString(2), cursor.getString(4));
                                        //本地删除
                                        dbWrite.delete(DataManager.DB_NOTES, "_id = ?", new String[]{cursor.getInt(0) + ""});
                                    } else {                                                        //网络不可用
                                        //添加到待删除数据库
                                        ContentValues cv = new ContentValues();
                                        cv.put("user", currentUser.getUsername());
                                        cv.put("delObjId", cursor.getString(2));
                                        if (cursor.getString(4).contains("<img src='")) {
                                            cv.put("includeMedia", "T");
                                        } else {
                                            cv.put("includeMedia", "F");
                                        }
                                        dbWrite.insert("del", null, cv);

                                        //本地删除
                                        dbWrite.delete(DataManager.DB_NOTES, "_id = ?", new String[]{cursor.getInt(0) + ""});
                                    }
                                }
                                refreshListView();
                            }
                        }).show();
                return true;
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        //获取用户
        currentUser = BmobUser.getCurrentUser(getContext(), MyUser.class);
        if (currentUser != null) {

            btnAccount.setImageResource(R.drawable.login_account_32);//用户登录 改变用户头像
            refreshListView();
        }

    }

    //刷新ListView
    public void refreshListView() {
        Cursor cursor = dbRead.query(DataManager.DB_NOTES, null,
                "user = ?",
                new String[]{currentUser.getUsername()}, null, null, null);
        adapter = new DBAdapter(getContext(), cursor);
        adapter.notifyDataSetChanged();
        noteListView.setAdapter(adapter);
    }



}

package com.jikexueyuan.cnote.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.data.MediaFile;
import com.jikexueyuan.cnote.db.NotesDB;
import com.jikexueyuan.cnote.manager.DataManager;
import com.jikexueyuan.cnote.manager.LinkMovementMethodExt;
import com.jikexueyuan.cnote.manager.MessageSpan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;

public class NoteActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton btnBack, btnEdit, btnDelete;
    private TextView tvNoteTitle, tvNote;
    private String user, objectId, noteTitle, noteContent, tag;
    private int localId;
    private int width; //屏幕宽度
    private SQLiteDatabase dbWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        btnBack = (ImageButton) findViewById(R.id.btnNoteBack);

        btnEdit = (ImageButton) findViewById(R.id.btnEdit);

        btnDelete = (ImageButton) findViewById(R.id.btnDelete);

        tvNoteTitle = (TextView) findViewById(R.id.tvNoteTitle);

        tvNote = (TextView) findViewById(R.id.tvNote);

        WindowManager wm = this.getWindowManager();
        width = wm.getDefaultDisplay().getWidth();

        NotesDB db = new NotesDB(this);
        dbWrite = db.getWritableDatabase();


        user = getIntent().getStringExtra("user");
        objectId = getIntent().getStringExtra("objectId");
        noteTitle = getIntent().getStringExtra("noteTitle");
        noteContent = getIntent().getStringExtra("noteContent");
        tag = getIntent().getStringExtra("tag");
        localId = getIntent().getIntExtra("localId", -1);

        //图片点击事件实现   如果是图片则条用系统图片浏览器显示图片，如果是视频则播放视频
        Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (what == 200) {
                    MessageSpan ms = (MessageSpan) msg.obj;
                    Object[] spans = (Object[]) ms.getObj();

                    for (Object span : spans) {
                        if (span instanceof ImageSpan) {
                            File file = new File(((ImageSpan) span).getSource());
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (((ImageSpan) span).getSource().endsWith("jpg")) {
                                intent.setDataAndType(Uri.fromFile(file), "image/*");
                            } else if (((ImageSpan) span).getSource().endsWith("mp4")) {
                                intent.setDataAndType(Uri.fromFile(file), "video/*");
                            }
                            startActivity(intent);
                        }
                    }
                }
            }

        };

        tvNote.setMovementMethod(LinkMovementMethodExt.getInstance(handler, ImageSpan.class));


        //弹出加载对话框
        DataManager.showProgressDialog(this, "正在加载...", 1500);

        tvNoteTitle.setText(noteTitle);

        if (noteContent.contains("<img src='")) {                 //如果日志内容有媒体文件
            //获取含文件路径的字符串
            String filePathTemp[] = noteContent.split("<img src='");
            //创建文件是否存在的集合
            List<String> localFileExists = new ArrayList<>();
            //获取文件路径  判断在本机是否存在该文件
            for (int i = 1; i < filePathTemp.length; i++) {
                File localFile = new File(filePathTemp[i].substring(0, filePathTemp[i].indexOf("'/>")));
                if (localFile.exists()) {
                    localFileExists.add("true");
                } else {
                    localFileExists.add("false");
                }
            }

            if (localFileExists.contains("false")) {                              //当含有不存在的文件
                if (DataManager.isNetworkAvailable(getApplicationContext())) {
                    //有网就下载

                    /**
                     * V1版本作业 2）存在无法显示图片（一台设备上传了图片，另一台设备无法显示出来）
                     * 问题就是这里
                     * 文件下载所需时间超过2S，而2S后设置了tvNote.setText  无法显示图片
                     * 查看本地文件夹  还是会看到图片已下载
                     *
                     *
                     DataManager.downLoadMediaFile(getApplicationContext(), objectId);
                     //数据传输可能需要时间  设置一个进程对话框 延时2s
                     DataManager.showProgressDialog(this, "正在加载...", 2000);
                     */

                    //查询媒体数据
                    BmobQuery<MediaFile> fileBmobQuery = new BmobQuery<>();
                    fileBmobQuery.addWhereEqualTo("note", objectId);
                    fileBmobQuery.findObjects(getApplicationContext(), new FindListener<MediaFile>() {
                        @Override
                        public void onSuccess(final List<MediaFile> list) {
                            for (int i = 0; i < list.size(); i++) {
                                BmobFile file = list.get(i).getFile();
                                if (file != null) {
                                    //下载媒体文件.
                                    File saveFile = new File(DataManager.getMediaDir(), file.getFilename());
                                    if (!saveFile.exists()) {                                                   //本地不存在就下载  存在无需下载
                                        final int finalI = i;
                                        file.download(getApplicationContext(), saveFile, new DownloadFileListener() {
                                            @Override
                                            public void onSuccess(String s) {
                                                if (finalI == list.size() - 1) {
                                                    //媒体文件下载完毕  textView插入图片
                                                    tvNote.setText(DataManager.tvInsertPic(noteContent, width));
                                                }
                                            }

                                            @Override
                                            public void onFailure(int i, String s) {

                                            }
                                        });
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(int i, String s) {

                        }
                    });

                } else {
                    //没有网络 提示用户
                    tvNote.setText(noteContent);
                    Toast.makeText(this, "无法加载图片，请检查网络设置！", Toast.LENGTH_LONG).show();
                }
            } else {                                                               //媒体文件在本地都存在
                //直接插入图片
                tvNote.setText(DataManager.tvInsertPic(noteContent, width));
            }

        } else {
            tvNote.setText(noteContent);                                          //没有媒体文件直接显示文字
        }


        btnBack.setOnClickListener(this);
        btnEdit.setOnClickListener(this);
        btnDelete.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnNoteBack:
                finish();
                break;
            case R.id.btnEdit:

                Intent i = new Intent(getApplicationContext(), AddActivity.class);
                i.putExtra("localId", localId);
                i.putExtra("objectId", objectId);
                i.putExtra("noteTitle", noteTitle);
                i.putExtra("noteContent", noteContent);
                i.putExtra("tag", tag);
                startActivity(i);

                break;
            case R.id.btnDelete:

                new AlertDialog.Builder(NoteActivity.this)
                        .setTitle("确认删除？").setMessage("删除后无法恢复")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {

                                if (tag.equals(DataManager.TAG_UP)) {                               //日志未上传
                                    dbWrite.delete(DataManager.DB_NOTES, "_id = ?", new String[]{localId + ""});

                                    finish();
                                } else {                                                            //已上传的日志

                                    if (DataManager.isNetworkAvailable(getApplicationContext())) {  //网络连接可用
                                        //服务器端删除
                                        DataManager.deleteNote(getApplicationContext(), objectId, noteContent);
                                        //本地删除
                                        dbWrite.delete(DataManager.DB_NOTES, "_id = ?", new String[]{localId + ""});

                                        finish();
                                    } else {                                                        //网络不可用
                                        //添加到待删除数据库
                                        ContentValues cv = new ContentValues();
                                        cv.put("user", user);
                                        cv.put("delObjId", objectId);
                                        if (noteContent.contains("<img src='")) {
                                            cv.put("includeMedia", "T");
                                        } else {
                                            cv.put("includeMedia", "F");
                                        }
                                        dbWrite.insert("del", null, cv);

                                        //本地删除
                                        dbWrite.delete(DataManager.DB_NOTES, "_id = ?", new String[]{localId + ""});

                                        finish();
                                    }
                                }
                            }
                        }).show();

                break;
        }
    }
}

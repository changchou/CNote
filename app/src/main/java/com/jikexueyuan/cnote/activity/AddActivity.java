package com.jikexueyuan.cnote.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Toast;

import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.data.MediaFile;
import com.jikexueyuan.cnote.data.MyUser;
import com.jikexueyuan.cnote.data.Notes;
import com.jikexueyuan.cnote.db.NotesDB;
import com.jikexueyuan.cnote.manager.DataManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadBatchListener;

public class AddActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etNoteTitle, etNote;
    private ScrollView scrollView;
    private ImageButton btnBack, btnSave, btnPhoto, btnCamera, btnVideo;
    private File photoFile, imageFile, videoFile;

    private Intent intent;
    private static final int REQ_CODE_ADD_PHOTO = 1;
    private static final int REQ_CODE_ADD_CAMERA = 2;
    private static final int REQ_CODE_ADD_VIDEO = 3;

    private String objectId, noteTitle, noteContent, tag;
    private int localId = -1;
    private SQLiteDatabase dbWrite;
    private String newId;

    private ProgressDialog progressDialog;
    private int width; //屏幕宽度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        etNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        etNote = (EditText) findViewById(R.id.etNote);
        btnPhoto = (ImageButton) findViewById(R.id.btnPhoto);
        btnCamera = (ImageButton) findViewById(R.id.btnCamera);
        btnVideo = (ImageButton) findViewById(R.id.btnVideo);
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnSave = (ImageButton) findViewById(R.id.btnSave);

        NotesDB db = new NotesDB(this);
        dbWrite = db.getWritableDatabase();

        WindowManager wm = this.getWindowManager();
        width = wm.getDefaultDisplay().getWidth();

        //点击屏幕获得etNote的输入焦点及键盘
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            int x1;
            int x2;
            int y1;
            int y2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    x1 = (int) event.getX();
                    y1 = (int) event.getY();
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    x2 = (int) event.getX();
                    y2 = (int) event.getY();
                    if (Math.abs(x1 - x2) < 10 && Math.abs(y1 - y2) < 10) {
                        etNote.setFocusable(true);
                        etNote.setFocusableInTouchMode(true);
                        etNote.requestFocus();
                        etNote.findFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(etNote, 0);
                    }
                }

                return false;
            }
        });

        btnBack.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnPhoto.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
        btnVideo.setOnClickListener(this);

        //编辑界面
        localId = getIntent().getIntExtra("localId", -1);
        if (localId > -1) {

            objectId = getIntent().getStringExtra("objectId");
            noteTitle = getIntent().getStringExtra("noteTitle");
            noteContent = getIntent().getStringExtra("noteContent");
            tag = getIntent().getStringExtra("tag");

            etNoteTitle.setText(noteTitle);

            if (noteContent.contains("<img src='")) {                                               //插入图片
                String contentPiece[] = noteContent.split("<img src='");
                etNote.append(contentPiece[0]);
                for (int i = 1; i < contentPiece.length; i++) {
                    String insertFilePath = contentPiece[i].substring(0, contentPiece[i].indexOf("'/>"));
                    Bitmap bmp = null;
                    if (insertFilePath.endsWith("jpg")) {
                        BitmapFactory.Options op = new BitmapFactory.Options();
                        op.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(insertFilePath, op);
                        int wRatio = (int) Math.ceil(op.outWidth / width);
//                        int hRatio = (int) Math.ceil(op.outHeight / 500);

//                        //如果超出指定大小，则缩小相应的比例
//                        if (wRatio > 1 && hRatio > 1) {
//                            op.inSampleSize = wRatio > hRatio ? wRatio : hRatio;
//                        }
                        if (wRatio > 1) {
                            op.inSampleSize = wRatio;
                        }
                        op.inJustDecodeBounds = false;
                        bmp = BitmapFactory.decodeFile(insertFilePath, op);
                    } else if (insertFilePath.endsWith("mp4")) {
                        bmp = ThumbnailUtils.createVideoThumbnail(insertFilePath, MediaStore.Video.Thumbnails.MINI_KIND);
                    }
                    ImageSpan imageSpan = new ImageSpan(getApplicationContext(), bmp);
                    SpannableString spannableString = new SpannableString("<img src='" + contentPiece[i]);
                    spannableString.setSpan(imageSpan, 0, contentPiece[i].indexOf("'/>") + 13, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    etNote.append(spannableString);
                }

            } else {
                etNote.setText(noteContent);
            }

        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPhoto:

                intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQ_CODE_ADD_PHOTO);

                break;
            case R.id.btnCamera:
                //图片存储
                imageFile = new File(DataManager.getMediaDir(), System.currentTimeMillis() + ".jpg");
                if (!imageFile.exists()) {
                    try {
                        imageFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                startActivityForResult(intent, REQ_CODE_ADD_CAMERA);

                break;
            case R.id.btnVideo:
                //录像存储
                videoFile = new File(DataManager.getMediaDir(), System.currentTimeMillis() + ".mp4");
                if (!videoFile.exists()) {
                    try {
                        videoFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                startActivityForResult(intent, REQ_CODE_ADD_VIDEO);

                break;
            case R.id.btnBack:

                finish();

                break;
            case R.id.btnSave:

                final MyUser currentUser = BmobUser.getCurrentUser(getApplicationContext(), MyUser.class);

                //保存日志   内容不能为空
                if (!TextUtils.isEmpty(etNoteTitle.getText()) && !TextUtils.isEmpty(etNote.getText())) {

                    //更新日志
                    if (localId > -1) {

                        //先判断文章是否修改  标题或内容任一改变则视为要更新
                        if (!(etNote.getText().toString().trim().equals(noteContent)) ||
                                !(etNoteTitle.getText().toString().trim().equals(noteTitle))) {

                            if (DataManager.isNetworkAvailable(this)) {         //有网络

                                if (tag.equals("upload")) {                      //日志标记为未上传
                                    //服务器端上传日志
                                    final String content = etNote.getText().toString().trim();
                                    final Notes note = new Notes();
                                    note.setAuthor(currentUser);
                                    note.setNoteTitle(etNoteTitle.getText().toString().trim());
                                    note.setNoteContent(etNote.getText().toString().trim());
                                    note.save(getApplicationContext(), new SaveListener() {
                                        @Override
                                        public void onSuccess() {
                                            //含媒体文件上传媒体文件
                                            if (content.contains("<img src='")) {
                                                //上传文件
                                                //获取含文件路径的字符串
                                                String filePathTemp[] = content.split("<img src='");
                                                //创建文件路径集合
                                                final String filePath[] = new String[filePathTemp.length - 1];
                                                //添加文件路径
                                                for (int i = 1; i < filePathTemp.length; i++) {
                                                    filePath[i - 1] = filePathTemp[i].substring(0, filePathTemp[i].indexOf("'/>"));
                                                }
                                                //批量上传文件
                                                BmobFile.uploadBatch(getApplicationContext(), filePath, new UploadBatchListener() {
                                                    @Override
                                                    public void onSuccess(List<BmobFile> files, List<String> urls) {
                                                        //1、files-上传完成后的BmobFile集合
                                                        //2、urls-上传文件的完整url地址
                                                        //将上传的数据保存到表中  进行数据批量上传
                                                        List<BmobObject> mediaFileData = new ArrayList<>();
                                                        for (int i = 0; i < filePath.length; i++) {
                                                            if (urls.size() == i + 1) {     //urls == 1 第一个文件上传成功
                                                                MediaFile file = new MediaFile(note, files.get(i));
                                                                mediaFileData.add(file);
                                                            }
                                                        }
                                                        //数据批量上传
                                                        if (urls.size() == filePath.length) {  //文件全部上传完成
                                                            new BmobObject().insertBatch(getApplicationContext(), mediaFileData, new SaveListener() {
                                                                @Override
                                                                public void onSuccess() {

                                                                }

                                                                @Override
                                                                public void onFailure(int i, String s) {

                                                                }
                                                            });

                                                            //文件上传成功 停止progress
                                                            progressDialog.dismiss();
                                                            startActivity(new Intent(AddActivity.this, MainActivity.class));
                                                            finish();
                                                        }
                                                    }

                                                    @Override
                                                    public void onProgress(int i, int i1, int i2, int i3) {

                                                        progressDialog.setMessage("正在保存...");
                                                        progressDialog.show();
                                                    }

                                                    @Override
                                                    public void onError(int i, String s) {

                                                    }
                                                });


                                            }
                                        }

                                        @Override
                                        public void onFailure(int i, String s) {

                                        }
                                    });

                                    //本地更新
                                    ContentValues cv = new ContentValues();
                                    cv.put("noteTitle", etNoteTitle.getText().toString().trim());
                                    cv.put("noteContent", etNote.getText().toString().trim());
                                    cv.put("tag", DataManager.TAG_SYNC);
                                    dbWrite.update(DataManager.DB_NOTES, cv, "_id = ?", new String[]{localId + ""});


                                } else {                                                            //日志标记是同步或更新 表示服务器存在

                                    //在服务器更新
                                    final String content = etNote.getText().toString().trim();
                                    final Notes note = new Notes();
                                    note.setNoteTitle(etNoteTitle.getText().toString().trim());
                                    note.setNoteContent(etNote.getText().toString().trim());
                                    note.update(getApplicationContext(), objectId, new UpdateListener() {
                                        @Override
                                        public void onSuccess() {
                                            //含媒体文件上传媒体文件
                                            if (content.contains("<img src='")) {
                                                //上传文件
                                                //获取含文件路径的字符串
                                                String filePathTemp[] = content.split("<img src='");
                                                //创建文件路径集合
                                                final String filePath[] = new String[filePathTemp.length - 1];
                                                //添加文件路径
                                                for (int i = 1; i < filePathTemp.length; i++) {
                                                    filePath[i - 1] = filePathTemp[i].substring(0, filePathTemp[i].indexOf("'/>"));
                                                }
                                                //批量上传文件
                                                BmobFile.uploadBatch(getApplicationContext(), filePath, new UploadBatchListener() {
                                                    @Override
                                                    public void onSuccess(List<BmobFile> files, List<String> urls) {
                                                        //1、files-上传完成后的BmobFile集合
                                                        //2、urls-上传文件的完整url地址
                                                        //将上传的数据保存到表中  进行数据批量上传
                                                        List<BmobObject> mediaFileData = new ArrayList<>();
                                                        for (int i = 0; i < filePath.length; i++) {
                                                            if (urls.size() == i + 1) {     //urls == 1 第一个文件上传成功
                                                                MediaFile file = new MediaFile(note, files.get(i));
                                                                mediaFileData.add(file);
                                                            }
                                                        }
                                                        //数据批量上传
                                                        if (urls.size() == filePath.length) {  //文件全部上传完成
                                                            new BmobObject().insertBatch(getApplicationContext(), mediaFileData, new SaveListener() {
                                                                @Override
                                                                public void onSuccess() {

                                                                }

                                                                @Override
                                                                public void onFailure(int i, String s) {

                                                                }
                                                            });
                                                            //文件上传成功 停止progress
                                                            progressDialog.dismiss();
                                                            startActivity(new Intent(AddActivity.this, MainActivity.class));
                                                            finish();
                                                        }
                                                    }

                                                    @Override
                                                    public void onProgress(int i, int i1, int i2, int i3) {
                                                        progressDialog.setMessage("正在保存...");
                                                        progressDialog.show();
                                                    }

                                                    @Override
                                                    public void onError(int i, String s) {

                                                    }
                                                });


                                            }

                                        }

                                        @Override
                                        public void onFailure(int i, String s) {

                                        }
                                    });

                                    //本地更新
                                    ContentValues cv = new ContentValues();
                                    cv.put("noteTitle", etNoteTitle.getText().toString().trim());
                                    cv.put("noteContent", etNote.getText().toString().trim());
                                    cv.put("tag", DataManager.TAG_SYNC);
                                    dbWrite.update(DataManager.DB_NOTES, cv, "_id = ?", new String[]{localId + ""});


                                }
                            } else {                                                                //无网络

                                if (tag.equals("upload")) {                                         //未上传过的
                                    //本地更新
                                    ContentValues cv = new ContentValues();
                                    cv.put("noteTitle", etNoteTitle.getText().toString().trim());
                                    cv.put("noteContent", etNote.getText().toString().trim());
                                    cv.put("tag", DataManager.TAG_UP);
                                    dbWrite.update(DataManager.DB_NOTES, cv, "_id = ?", new String[]{localId + ""});

                                    startActivity(new Intent(AddActivity.this, MainActivity.class));
                                    finish();
                                } else {                                                             //服务器已存在的
                                    //本地更新
                                    ContentValues cv = new ContentValues();
                                    cv.put("noteTitle", etNoteTitle.getText().toString().trim());
                                    cv.put("noteContent", etNote.getText().toString().trim());
                                    cv.put("tag", DataManager.TAG_ED);
                                    dbWrite.update(DataManager.DB_NOTES, cv, "_id = ?", new String[]{localId + ""});

                                    startActivity(new Intent(AddActivity.this, MainActivity.class));
                                    finish();
                                }

                            }
                        } else {
                            finish();                                                               //文章未改变 退出
                        }

                    } else {
                        //保存新日志
                        if (DataManager.isNetworkAvailable(this)) {                                 //有网络

                            final Notes note = new Notes();
                            note.setAuthor(currentUser);
                            note.setNoteTitle(etNoteTitle.getText().toString().trim());
                            note.setNoteContent(etNote.getText().toString().trim());
                            note.save(getApplicationContext(), new SaveListener() {
                                @Override
                                public void onSuccess() {

                                    newId = note.getObjectId();
                                    //本地保存
                                    ContentValues cv = new ContentValues();
                                    cv.put("user", currentUser.getUsername());
                                    cv.put("objectId", newId);
                                    cv.put("noteTitle", etNoteTitle.getText().toString().trim());
                                    cv.put("noteContent", etNote.getText().toString().trim());
                                    cv.put("noteDate", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
                                    cv.put("tag", DataManager.TAG_SYNC);
                                    dbWrite.insert(DataManager.DB_NOTES, null, cv);

                                    String content = etNote.getText().toString().trim();
                                    //含媒体文件上传媒体文件
                                    if (content.contains("<img src='")) {
                                        //上传文件
                                        //获取含文件路径的字符串
                                        String filePathTemp[] = content.split("<img src='");
                                        //创建文件路径集合
                                        final String filePath[] = new String[filePathTemp.length - 1];
                                        //添加文件路径
                                        for (int i = 1; i < filePathTemp.length; i++) {
                                            filePath[i - 1] = filePathTemp[i].substring(0, filePathTemp[i].indexOf("'/>"));
                                        }
                                        //批量上传文件
                                        BmobFile.uploadBatch(getApplicationContext(), filePath, new UploadBatchListener() {
                                            @Override
                                            public void onSuccess(List<BmobFile> files, List<String> urls) {
                                                //1、files-上传完成后的BmobFile集合
                                                //2、urls-上传文件的完整url地址
                                                //将上传的数据保存到表中  进行数据批量上传
                                                List<BmobObject> mediaFileData = new ArrayList<>();
                                                for (int i = 0; i < filePath.length; i++) {
                                                    if (urls.size() == i + 1) {     //urls == 1 第一个文件上传成功
                                                        MediaFile file = new MediaFile(note, files.get(i));
                                                        mediaFileData.add(file);
                                                    }
                                                }
                                                //数据批量上传
                                                if (urls.size() == filePath.length) {  //文件全部上传完成
                                                    new BmobObject().insertBatch(getApplicationContext(), mediaFileData, new SaveListener() {
                                                        @Override
                                                        public void onSuccess() {

                                                        }

                                                        @Override
                                                        public void onFailure(int i, String s) {

                                                        }
                                                    });

                                                    //文件上传成功 停止progress
                                                    progressDialog.dismiss();
                                                    finish();
                                                }


                                            }

                                            @Override
                                            public void onProgress(int i, int i1, int i2, int i3) {
                                                progressDialog.setMessage("正在保存...");
                                                progressDialog.show();
                                            }

                                            @Override
                                            public void onError(int i, String s) {

                                            }
                                        });


                                    }


                                }

                                @Override
                                public void onFailure(int i, String s) {

                                }
                            });

                        } else {                                                                    //无网络

                            //本地保存
                            ContentValues cv = new ContentValues();
                            cv.put("user", currentUser.getUsername());

                            cv.put("noteTitle", etNoteTitle.getText().toString().trim());
                            cv.put("noteContent", etNote.getText().toString().trim());
                            cv.put("noteDate", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
                            cv.put("tag", DataManager.TAG_UP);
                            dbWrite.insert(DataManager.DB_NOTES, null, cv);

                            finish();
                        }

                    }
                } else {
                    Toast.makeText(this, "请输入内容", Toast.LENGTH_LONG).show();
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_ADD_PHOTO:

                if (data != null && resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();
                    photoFile = new File(picturePath);

                    //editText插入图片
                    DataManager.etInsertPic(getApplicationContext(), photoFile, etNote);

                }
                break;
            case REQ_CODE_ADD_CAMERA:
                if (resultCode == RESULT_OK) {

                    //editText插入图片
                    DataManager.etInsertPic(getApplicationContext(), imageFile, etNote);

                }
                break;
            case REQ_CODE_ADD_VIDEO:
                if (resultCode == RESULT_OK) {

                    //editText插入录像缩略图
                    DataManager.etInsertPic(getApplicationContext(), videoFile, etNote);

                }
                break;
        }
    }
}

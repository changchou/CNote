package com.jikexueyuan.cnote.manager;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.widget.EditText;

import com.jikexueyuan.cnote.data.MediaFile;
import com.jikexueyuan.cnote.data.MyUser;
import com.jikexueyuan.cnote.data.Notes;
import com.jikexueyuan.cnote.db.NotesDB;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DeleteListener;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadBatchListener;

/**
 * Created by Mr.Z on 2016/4/25 0025.
 */
public class DataManager {

    public static final String DB_NOTES = "notes";
    public static final String TAG_SYNC = "sync";
    public static final String TAG_UP = "upload";
    public static final String TAG_ED = "edit";


    /**
     * 检测当的网络状态
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 数据同步
     *
     * @param context
     * @param currentUser
     */
    public static void syncDBData(final Context context, final MyUser currentUser) {

        NotesDB db = new NotesDB(context);
        final SQLiteDatabase dbRead = db.getReadableDatabase();
        final SQLiteDatabase dbWrite = db.getWritableDatabase();

        //查找出SQL中当前用户待删除的内容 在服务器删除 再在本地删除
        //查询当前用户待删除数据
        final Cursor cursorD = dbRead.query("del", null,
                "user = ?",
                new String[]{currentUser.getUsername()}, null, null, null);
        while (cursorD.moveToNext()) {
            //在服务器删除
            final int id = cursorD.getInt(0);
            final String objId = cursorD.getString(2);
            final String includeMedia = cursorD.getString(3);
            Notes noteD = new Notes();
            noteD.setObjectId(objId);
            noteD.delete(context, new DeleteListener() {
                @Override
                public void onSuccess() {
                    //如果含有媒体文件则在服务器删除
                    if (includeMedia.equals("T")) {
                        //含有媒体 查询媒体文件数据
                        BmobQuery<MediaFile> fileBmobQuery = new BmobQuery<>();
                        fileBmobQuery.addWhereEqualTo("note", objId);
                        fileBmobQuery.findObjects(context, new FindListener<MediaFile>() {
                            @Override
                            public void onSuccess(List<MediaFile> list) {

                                for (MediaFile mediaFile : list) {

                                    //删除查询到的媒体数据
                                    MediaFile mfData = new MediaFile();
                                    mfData.setObjectId(mediaFile.getObjectId());
                                    mfData.delete(context);

                                    //删除查询到的媒体文件
                                    BmobFile file = mediaFile.getFile();
                                    if (file != null) {
                                        BmobFile bmobFile = new BmobFile();
                                        bmobFile.setUrl(file.getUrl());
                                        bmobFile.delete(context);
                                    }
                                }

                                //本地删除
                                dbWrite.delete("del", "_id = ?", new String[]{id + ""});

                            }

                            @Override
                            public void onError(int i, String s) {

                            }
                        });
                    } else {

                        //本地删除
                        dbWrite.delete("del", "_id = ?", new String[]{id + ""});
                    }

                }

                @Override
                public void onFailure(int i, String s) {

                }
            });
        }


        //查询SQL中待上传的数据 上传到服务器
        final Cursor cursorU = dbRead.query(DB_NOTES, null,
                "user = ? and tag = ?",
                new String[]{currentUser.getUsername(), TAG_UP}, null, null, null);
        while (cursorU.moveToNext()) {
            final int id = cursorU.getInt(0);
            String title = cursorU.getString(3);
            final String content = cursorU.getString(4);
            final Notes noteU = new Notes();
            noteU.setAuthor(currentUser);
            noteU.setNoteTitle(title);
            noteU.setNoteContent(content);
            noteU.save(context, new SaveListener() {
                @Override
                public void onSuccess() {
                    //如果有媒体文件  上传媒体文件
                    if (content.contains("<img src='")) {
                        //上传媒体文件
                        upLoadMediaFile(context, content, noteU);

                    }

                    //待上传的数据在本地没有保存objectId
                    //添加数据成功后，返回objectId为：noteU.getObjectId()
                    //将其更新到本地数据  并改变标签
                    ContentValues cvU = new ContentValues();
                    cvU.put("objectId", noteU.getObjectId());
                    cvU.put("tag", TAG_SYNC);
                    dbWrite.update(DB_NOTES, cvU, "_id = ?", new String[]{id + ""});
                }

                @Override
                public void onFailure(int i, String s) {

                }
            });
        }


        //查询SQL中待更新的数据 上传到服务器
        final Cursor cursorE = dbRead.query(DB_NOTES, null,
                "user = ? and tag = ?",
                new String[]{currentUser.getUsername(), TAG_ED}, null, null, null);
        while (cursorE.moveToNext()) {
            final int id = cursorE.getInt(0);
            String objId = cursorE.getString(2);
            String title = cursorE.getString(3);
            final String content = cursorE.getString(4);
            final Notes noteE = new Notes();
            noteE.setNoteTitle(title);
            noteE.setNoteContent(content);
            noteE.update(context, objId, new UpdateListener() {
                @Override
                public void onSuccess() {
                    //如果修改后的日志含有媒体文件  上传媒体文件
                    if (content.contains("<img src='")) {

                        upLoadMediaFile(context, content, noteE);

                    }
                    //修改日志上传成功  改变本地数据的TAG标签
                    ContentValues cvE = new ContentValues();
                    cvE.put("tag", TAG_SYNC);
                    dbWrite.update(DB_NOTES, cvE, "_id = ?", new String[]{id + ""});

                }

                @Override
                public void onFailure(int i, String s) {

                }
            });

        }

        //将服务器的数据下载到本地  此时本地的数据都是已经同步过的数据
        //查询当前用户的所有日志
        BmobQuery<Notes> notesBmobQuery = new BmobQuery<>();
        notesBmobQuery.addWhereEqualTo("author", currentUser);//查询当前用户所有日志
        notesBmobQuery.order("-createdAt");//根据createdAt字段降序显示数据
        notesBmobQuery.findObjects(context, new FindListener<Notes>() {
            @Override
            public void onSuccess(List<Notes> list) {
                //先查询本地所有当前用户的数据
                List<String> localData = new ArrayList<String>();
                Cursor c = dbRead.query(DB_NOTES, null,
                        "user = ?",
                        new String[]{currentUser.getUsername()}, null, null, null);
                while (c.moveToNext()) {
                    localData.add(c.getString(2));
                }
                //遍历所有下载的数据
                for (Notes note : list) {
                    if (!(localData.contains(note.getObjectId()))) {    //本地数据不含有下载的数据才添加到本地
                        ContentValues cv = new ContentValues();
                        cv.put("user", currentUser.getUsername());
                        cv.put("objectId", note.getObjectId());
                        cv.put("noteTitle", note.getNoteTitle());
                        cv.put("noteContent", note.getNoteContent());
                        cv.put("noteDate", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
                        cv.put("tag", TAG_SYNC);
                        dbWrite.insert(DB_NOTES, null, cv);
                    }
                }
            }

            @Override
            public void onError(int i, String s) {

            }
        });
    }


    /**
     * 上传媒体文件
     *
     * @param context
     * @param content
     * @param note
     */
    public static void upLoadMediaFile(final Context context, String content, final Notes note) {
        //获取含文件路径的字符串
        String filePathTemp[] = content.split("<img src='");
        //创建文件路径集合
        final String filePath[] = new String[filePathTemp.length - 1];
        //添加文件路径
        for (int i = 1; i < filePathTemp.length; i++) {
            filePath[i - 1] = filePathTemp[i].substring(0, filePathTemp[i].indexOf("'/>"));
        }
        //批量上传文件
        BmobFile.uploadBatch(context, filePath, new UploadBatchListener() {
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
                    new BmobObject().insertBatch(context, mediaFileData, new SaveListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure(int i, String s) {

                        }
                    });
                }
            }

            @Override
            public void onProgress(int i, int i1, int i2, int i3) {

            }

            @Override
            public void onError(int i, String s) {

            }
        });


    }


    /**
     * 下载媒体文件
     *
     * @param context
     * @param objId
     */
    public static void downLoadMediaFile(final Context context, String objId) {
        //查询媒体数据
        BmobQuery<MediaFile> fileBmobQuery = new BmobQuery<>();
        fileBmobQuery.addWhereEqualTo("note", objId);
        fileBmobQuery.findObjects(context, new FindListener<MediaFile>() {
            @Override
            public void onSuccess(List<MediaFile> list) {
                for (MediaFile mediaFile : list) {
                    BmobFile file = mediaFile.getFile();
                    if (file != null) {
                        //下载媒体文件.
                        File saveFile = new File(getMediaDir(), file.getFilename());

                        if (!saveFile.exists()) {                                                   //本地不存在就下载  存在无需下载
                            file.download(context, saveFile, new DownloadFileListener() {
                                @Override
                                public void onSuccess(String s) {

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
    }

    /**
     * 删除服务器日志
     *
     * @param context
     * @param objId
     * @param content
     */
    public static void deleteNote(final Context context, final String objId, final String content) {

        Notes note = new Notes();
        note.setObjectId(objId);
        note.delete(context, new DeleteListener() {
            @Override
            public void onSuccess() {
                //如果含有媒体文件
                if (content.contains("<img src='")) {
                    //查询媒体文件数据
                    BmobQuery<MediaFile> fileBmobQuery = new BmobQuery<>();
                    fileBmobQuery.addWhereEqualTo("note", objId);
                    fileBmobQuery.findObjects(context, new FindListener<MediaFile>() {
                        @Override
                        public void onSuccess(List<MediaFile> list) {
                            for (MediaFile mediaFile : list) {

                                //删除查询到的媒体数据
                                MediaFile mfData = new MediaFile();
                                mfData.setObjectId(mediaFile.getObjectId());
                                mfData.delete(context);

                                //删除查询到的媒体文件
                                BmobFile file = mediaFile.getFile();
                                if (file != null) {
                                    BmobFile bmobFile = new BmobFile();
                                    bmobFile.setUrl(file.getUrl());
                                    bmobFile.delete(context);
                                }
                            }
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
    }


    /**
     * TextView图文混排
     *
     * @param content
     * @param Width
     * @return
     */
    public static CharSequence tvInsertPic(String content, final int Width) {

        Html.ImageGetter imgGetter = new Html.ImageGetter() {

            @Override
            public Drawable getDrawable(String source) {
                Bitmap bmp = null;
                if (source.endsWith("jpg")) {
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(source, op);
                    int wRatio = (int) Math.ceil(op.outWidth / Width);
                    if (wRatio > 1) {
                        op.inSampleSize = wRatio;
                    }
                    op.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeFile(source, op);
                } else if (source.endsWith("mp4")) {
                    bmp = ThumbnailUtils.createVideoThumbnail(source, MediaStore.Video.Thumbnails.MINI_KIND);
                }
                Drawable d = new BitmapDrawable(bmp);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                return d;
            }
        };

        CharSequence charSequence = Html.fromHtml(content, imgGetter, null);

        return charSequence;

    }


    /**
     * EditText插入图片
     *
     * @param context
     * @param file
     * @param editText
     */
    public static void etInsertPic(Context context, File file, EditText editText) {
        Bitmap bmp = null;
        if (file.getAbsolutePath().endsWith("jpg")) {
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(Uri.fromFile(file).getPath(), op); //获取尺寸信息

            //获取比例大小

            int width = editText.getWidth();
            int wRatio = (int) Math.ceil(op.outWidth / width);

            //上面方法压缩后会显示两张图片？？？？

//            int wRatio = (int) Math.ceil(op.outWidth / 500);

            if (wRatio > 1) {
                op.inSampleSize = wRatio;
            }

            op.inJustDecodeBounds = false;
            bmp = BitmapFactory.decodeFile(Uri.fromFile(file).getPath(), op);
        } else if (file.getAbsolutePath().endsWith("mp4")) {
            bmp = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        }
        //插入图片
        //创建ImageSpan对象
        ImageSpan imageSpan = new ImageSpan(context, bmp);
        String temUri = "\n<img src='" + Uri.fromFile(file).getPath() + "'/>\n";
        SpannableString spannableString = new SpannableString(temUri);
        spannableString.setSpan(imageSpan, 1, temUri.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int selectionStart = editText.getSelectionStart();
        Editable edit_text = editText.getEditableText();

        if (selectionStart < 0 || selectionStart >= edit_text.length()) {
            edit_text.append(spannableString);
        } else {
            edit_text.insert(selectionStart, spannableString);
        }

    }


    /**
     * 加载提示窗口
     *
     * @param context
     * @param str
     * @param time
     */
    public static void showProgressDialog(Context context, String str, final long time) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(str);
        progressDialog.show();
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
            }
        }.start();
    }


    /**
     * 创建媒体文件夹
     *
     * @return
     */
    public static File getMediaDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), "NotesMedia");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

}

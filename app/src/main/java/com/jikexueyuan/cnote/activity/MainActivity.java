package com.jikexueyuan.cnote.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.adapter.SectionsPagerAdapter;
import com.jikexueyuan.cnote.data.MyUser;
import com.jikexueyuan.cnote.manager.DataManager;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.SaveListener;


//主界面
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout mainLayout, logLayout;
    private MyUser currentUser;
    private EditText et_name, et_password;
    private Button btn_login, btn_reg;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;
    protected int mFinishCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // 初始化 Bmob SDK
        Bmob.initialize(this, "8a7608fd5d4eb04b7008d86e457a5ebc");

        //同时使用两个布局
        //查看有道云，发现必须登录后才能进入APP使用界面。使用两个布局交互隐藏、显示来达到效果
        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);//主布局 显示日志ListView 和 Fragment
        logLayout = (LinearLayout) findViewById(R.id.logLayout);//登陆布局

        et_name = (EditText) findViewById(R.id.et_name);
        et_password = (EditText) findViewById(R.id.et_password);
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_reg = (Button) findViewById(R.id.btn_reg);


        //Fragment适配器
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(sectionsPagerAdapter);

        //Tab标签
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);


        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(sectionsPagerAdapter.getTabView(i));
            }
        }
        tabLayout.getTabAt(0).getCustomView().setSelected(true);//设置选中状态

        btn_login.setOnClickListener(this);
        btn_reg.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //再次进入程序时根据缓存的CurrentUser对象来显示布局
        currentUser = BmobUser.getCurrentUser(getApplicationContext(), MyUser.class);
        if (currentUser != null) {
            mainLayout.setVisibility(View.VISIBLE);
            logLayout.setVisibility(View.GONE);
        } else {
            mainLayout.setVisibility(View.GONE);
            logLayout.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //登陆成功后 隐藏登录界面 显示主界面
            case R.id.btn_login:
                MyUser user = new MyUser();
                user.setUsername(et_name.getText().toString().trim());
                user.setPassword(et_password.getText().toString().trim());
                user.login(getApplicationContext(), new SaveListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "登陆成功", Toast.LENGTH_LONG).show();
                        //登陆成功 有网络 有用户
//                        //下载一次数据
                        if (BmobUser.getCurrentUser(getApplicationContext(), MyUser.class) != null) {
                            if (DataManager.isNetworkAvailable(getApplicationContext())) {   //判断网络连接
                                //进入APP时同步一次数据
                                DataManager.syncDBData(getApplicationContext(), BmobUser.getCurrentUser(getApplicationContext(), MyUser.class));
                                //经过多次测试，在第一次进入下载数据时，可能数据还没有下载完成就到了主界面
                                //此时SQL数据库可能还没有写入数据  ListView无法加载数据  通过下面的延时可使数据下载完成
                                /**
                                 * 实际上来说，如果数据很大，可能会要更多的时间，应该在onSuccess
                                 * 之后再调用UI控件
                                 */
                                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                progressDialog.setMessage("正在加载数据...");
                                progressDialog.show();
                                final Handler handler = new Handler() {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        super.handleMessage(msg);
                                        if (msg.what == 0) {
                                            mainLayout.setVisibility(View.VISIBLE);
                                            logLayout.setVisibility(View.GONE);
                                        }
                                    }
                                };
                                new Thread() {
                                    @Override
                                    public void run() {
                                        super.run();
                                        try {
                                            sleep(3000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        progressDialog.dismiss();
                                        Message msg = new Message();
                                        msg.what = 0;
                                        handler.sendMessage(msg);
                                    }
                                }.start();

                            }
                        }

                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                    }
                });
                break;

            //用户注册
            case R.id.btn_reg:
                startActivity(new Intent(getApplicationContext(), RegActivity.class));
                break;
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mFinishCount = 0;
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void finish() {

        if (currentUser != null) {
            mFinishCount++;
            if (mFinishCount == 1) {
                Toast.makeText(this, "再按一次退出！", Toast.LENGTH_LONG).show();
            } else if (mFinishCount == 2) {
                super.finish();
            }
        } else {
            super.finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (currentUser == null) {
            this.finish();
        }
    }


}

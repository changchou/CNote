package com.jikexueyuan.cnote.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.data.MyUser;

import cn.bmob.v3.BmobUser;

//用户界面
public class AccountActivity extends AppCompatActivity {

    private TextView tvAccountName, tvAccountNickName;
    private Button btnLogOut;
    private ImageButton btnAccountBack;
    private MyUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        tvAccountName = (TextView) findViewById(R.id.tvAccountName);

        tvAccountNickName = (TextView) findViewById(R.id.tvAccountNickName);

        btnAccountBack = (ImageButton) findViewById(R.id.btnAccountBack);

        btnLogOut = (Button) findViewById(R.id.btnLogOut);

        user = BmobUser.getCurrentUser(getApplicationContext(), MyUser.class);

        if (user != null) {
            tvAccountName.setText(user.getUsername());
            tvAccountNickName.setText(user.getUsername());
        }

        btnAccountBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //退出当前用户
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BmobUser.logOut(getApplicationContext());   //清除缓存用户对象
                finish();
            }
        });
    }
}

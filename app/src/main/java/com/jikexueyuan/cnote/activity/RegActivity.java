package com.jikexueyuan.cnote.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jikexueyuan.cnote.R;
import com.jikexueyuan.cnote.data.MyUser;

import cn.bmob.v3.listener.SaveListener;

public class RegActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_name, et_password;
    private Button btn_reg, btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        et_name = (EditText) findViewById(R.id.et_name);
        et_password = (EditText) findViewById(R.id.et_password);
        btn_reg = (Button) findViewById(R.id.btn_reg);
        btn_back = (Button) findViewById(R.id.btn_back);
        btn_reg.setOnClickListener(this);
        btn_back.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_reg:
                MyUser user = new MyUser();
                user.setUsername(et_name.getText().toString().trim());
                user.setPassword(et_password.getText().toString().trim());
                user.signUp(this, new SaveListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Toast.makeText(getApplicationContext(), "注册失败", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case R.id.btn_back:
                finish();
                break;
        }
    }
}

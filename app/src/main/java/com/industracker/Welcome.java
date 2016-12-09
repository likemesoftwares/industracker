package com.industracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Welcome extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        View registerView = findViewById(R.id.bt_register);
        View signInView = findViewById(R.id.bt_sigin);
        signInView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent= new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        registerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent= new Intent(getApplicationContext(),RegisterActivity.class);
                startActivity(registerIntent);
            }
        });
    }
}

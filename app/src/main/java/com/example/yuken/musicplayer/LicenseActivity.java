package com.example.yuken.musicplayer;

import android.app.Activity;
import android.os.Bundle;

import team.birdhead.widget.LicenseView;

public class LicenseActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        setContentView(new LicenseView(LicenseActivity.this));
    }
}
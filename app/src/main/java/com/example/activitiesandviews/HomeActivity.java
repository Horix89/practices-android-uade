package com.example.activitiesandviews;

import android.os.Bundle;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        String nombre = getIntent().getStringExtra("NOMBRE_USUARIO");

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Bienvenido, " + nombre + "!");
    }
}

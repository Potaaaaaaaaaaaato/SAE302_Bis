package com.tristan.sae302;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonClient = findViewById(R.id.buttonClient);
        Button buttonServer = findViewById(R.id.buttonServer);

        buttonClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lancer activité mode client
                // Reste à : implémenter la logique du mode client
            }
        });

        buttonServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lancer activité mode serveur
                // Reste à : implémenter la logique du mode serveur
            }
        });
    }
}






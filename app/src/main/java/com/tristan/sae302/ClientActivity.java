package com.tristan.sae302;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientActivity extends AppCompatActivity {

    private EditText ipEditText, portEditText, messageEditText;
    private Button connectButton, sendButton, disconnectButton, clearLogsButton;
    private TextView clientLogTextView;
    private ScrollView logScrollView;

    private Socket clientSocket;
    private PrintWriter output;
    private BufferedReader input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);
        messageEditText = findViewById(R.id.messageEditText);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        clearLogsButton = findViewById(R.id.clearLogsButton);
        clientLogTextView = findViewById(R.id.clientLogTextView);
        logScrollView = findViewById(R.id.logScrollView);

        sendButton.setEnabled(false);
        disconnectButton.setEnabled(false);

        connectButton.setOnClickListener(v -> connectToServer());
        sendButton.setOnClickListener(v -> sendMessageToServer());
        disconnectButton.setOnClickListener(v -> disconnectFromServer());
        clearLogsButton.setOnClickListener(v -> clientLogTextView.setText(""));
    }

    private void connectToServer() {
        String serverIP = ipEditText.getText().toString().trim();
        int serverPort;

        try {
            serverPort = Integer.parseInt(portEditText.getText().toString().trim());
            if (serverPort <= 0 || serverPort > 65535) {
                appendLog("Erreur : Port invalide.", Color.RED);
                return;
            }
        } catch (NumberFormatException e) {
            appendLog("Erreur : Le port doit être un nombre valide.", Color.RED);
            return;
        }

        new Thread(() -> {
            try {
                // Connexion au serveur
                clientSocket = new Socket(serverIP, serverPort);
                output = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                runOnUiThread(() -> {
                    appendLog("Connecté au serveur.", Color.GREEN);
                    sendButton.setEnabled(true);
                    disconnectButton.setEnabled(true);
                    connectButton.setEnabled(false);
                    ipEditText.setEnabled(false);
                    portEditText.setEnabled(false);
                });

                // Écouter les messages du serveur
                String response;
                while ((response = input.readLine()) != null) {
                    String finalResponse = response;
                    runOnUiThread(() -> appendLog("Serveur : " + finalResponse, Color.BLUE));
                }

            } catch (Exception e) {
                runOnUiThread(() -> appendLog("Erreur : " + e.getMessage(), Color.RED));
            }
        }).start();
    }

    private void sendMessageToServer() {
        String message = messageEditText.getText().toString().trim();
        if (!message.isEmpty() && output != null) {
            new Thread(() -> {
                output.println(message); // Envoyer le message au serveur
                runOnUiThread(() -> {
                    appendLog("Moi : " + message, Color.BLACK);
                    messageEditText.setText(""); // Effacer le champ de saisie
                });
            }).start();
        }
    }

    private void disconnectFromServer() {
        new Thread(() -> {
            try {
                if (clientSocket != null) clientSocket.close();
                runOnUiThread(() -> {
                    appendLog("Déconnecté du serveur.", Color.MAGENTA);
                    connectButton.setEnabled(true);
                    sendButton.setEnabled(false);
                    disconnectButton.setEnabled(false);
                    ipEditText.setEnabled(true);
                    portEditText.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> appendLog("Erreur lors de la déconnexion : " + e.getMessage(), Color.RED));
            }
        }).start();
    }

    private void appendLog(String message, int color) {
        String timestamp = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
        SpannableString spannableMessage = new SpannableString(timestamp + message + "\n");
        spannableMessage.setSpan(new ForegroundColorSpan(color), 0, spannableMessage.length(), 0);
        clientLogTextView.append(spannableMessage);
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN)); // Scroll vers le bas
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(() -> {
            try {
                if (clientSocket != null) clientSocket.close();
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
package com.tristan.sae302;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientActivity extends AppCompatActivity {

    private EditText ipEditText, portEditText, messageEditText, usernameEditText, passwordEditText;
    private Button connectButton, sendButton, disconnectButton, clearLogsButton, udpButton, sendFileButton;
    private TextView clientLogTextView;
    private ScrollView logScrollView;

    private Socket tcpSocket;
    private PrintWriter tcpOutput;
    private BufferedReader tcpInput;

    private DatagramSocket udpSocket;
    private boolean isUDPMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        ipEditText = findViewById(R.id.ipEditText);
        portEditText = findViewById(R.id.portEditText);
        messageEditText = findViewById(R.id.messageEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        clearLogsButton = findViewById(R.id.clearLogsButton);
        udpButton = findViewById(R.id.udpButton);
        sendFileButton = findViewById(R.id.sendFileButton);
        clientLogTextView = findViewById(R.id.clientLogTextView);
        logScrollView = findViewById(R.id.logScrollView);

        sendButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        sendFileButton.setEnabled(false);

        connectButton.setOnClickListener(v -> connectToServer());
        sendButton.setOnClickListener(v -> sendMessageToServer());
        disconnectButton.setOnClickListener(v -> disconnectFromServer());
        clearLogsButton.setOnClickListener(v -> clientLogTextView.setText(""));
        udpButton.setOnClickListener(v -> toggleUDPMode());
        sendFileButton.setOnClickListener(v -> selectFile());
    }

    private void connectToServer() {
        String serverIP = ipEditText.getText().toString().trim();
        int serverPort;

        try {
            serverPort = Integer.parseInt(portEditText.getText().toString().trim());
            if (serverPort <= 0 || serverPort > 65535) {
                appendLog("Erreur : port invalide.", Color.RED);
                return;
            }
        } catch (NumberFormatException e) {
            appendLog("Erreur : le port doit être un nombre valide.", Color.RED);
            return;
        }

        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (isUDPMode) {
            try {
                udpSocket = new DatagramSocket();
                appendLog("Mode UDP activé. Prêt à envoyer des messages.", Color.GREEN);
                sendButton.setEnabled(true);
                sendFileButton.setEnabled(true);
                disconnectButton.setEnabled(true);
                connectButton.setEnabled(false);
                ipEditText.setEnabled(false);
                portEditText.setEnabled(false);
            } catch (SocketException e) {
                appendLog("Erreur : impossible d'initialiser le mode UDP.", Color.RED);
            }
        } else {
            new Thread(() -> {
                try {
                    tcpSocket = new Socket(serverIP, serverPort);
                    tcpOutput = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream()), true);
                    tcpInput = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

                    // Envoyer les informations d'authentification
                    tcpOutput.println(username + "," + password);

                    // Attendre la réponse d'authentification
                    String authResponse = tcpInput.readLine();
                    if ("Success".equals(authResponse)) {
                        runOnUiThread(() -> {
                            appendLog("Authentification réussie. Connecté au serveur (TCP).", Color.GREEN);
                            sendButton.setEnabled(true);
                            sendFileButton.setEnabled(true);
                            disconnectButton.setEnabled(true);
                            connectButton.setEnabled(false);
                            ipEditText.setEnabled(false);
                            portEditText.setEnabled(false);
                            usernameEditText.setEnabled(false);
                            passwordEditText.setEnabled(false);
                        });

                        // Écouter les messages du serveur TCP
                        String response;
                        while ((response = tcpInput.readLine()) != null) {
                            String finalResponse = response;
                            runOnUiThread(() -> appendLog("Serveur : " + finalResponse, Color.BLUE));
                        }
                    } else {
                        runOnUiThread(() -> {
                            appendLog("Authentification échouée.", Color.RED);
                            disconnectFromServer();
                        });
                    }

                } catch (Exception e) {
                    runOnUiThread(() -> appendLog("Erreur TCP : " + e.getMessage(), Color.RED));
                }
            }).start();
        }
    }

    private void sendMessageToServer() {
        String message = messageEditText.getText().toString().trim();
        if (message.isEmpty()) {
            appendLog("Erreur : message vide.", Color.RED);
            return;
        }

        String serviceType = "echo"; // Par défaut, ou sélectionné par l'interface utilisateur
        String fullMessage = serviceType + ":" + message;

        if (isUDPMode && udpSocket != null) {
            new Thread(() -> {
                try {
                    String serverIP = ipEditText.getText().toString().trim();
                    int serverPort = Integer.parseInt(portEditText.getText().toString().trim());
                    byte[] data = fullMessage.getBytes();

                    // Envoyer le message au serveur
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIP), serverPort);
                    udpSocket.send(packet);
                    appendLog("Moi (UDP) : " + fullMessage, Color.BLACK);

                    // Recevoir la réponse
                    byte[] buffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(responsePacket);
                    String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    appendLog("Serveur (UDP) : " + responseMessage, Color.BLUE);

                } catch (Exception e) {
                    appendLog("Erreur UDP : " + e.getMessage(), Color.RED);
                }
            }).start();
        } else if (tcpOutput != null) {
            new Thread(() -> {
                tcpOutput.println(fullMessage);
                runOnUiThread(() -> {
                    appendLog("Moi (TCP) : " + fullMessage, Color.BLACK);
                    messageEditText.setText("");
                });
            }).start();
        }
    }

    private void disconnectFromServer() {
        new Thread(() -> {
            try {
                if (isUDPMode && udpSocket != null) {
                    udpSocket.close();
                    runOnUiThread(() -> {
                        appendLog("Mode UDP déconnecté.", Color.MAGENTA);
                        sendFileButton.setEnabled(false);
                    });
                } else if (tcpSocket != null) {
                    tcpSocket.close();
                    runOnUiThread(() -> {
                        appendLog("Déconnecté du serveur (TCP).", Color.MAGENTA);
                        sendFileButton.setEnabled(false);
                    });
                }

                runOnUiThread(() -> {
                    connectButton.setEnabled(true);
                    sendButton.setEnabled(false);
                    disconnectButton.setEnabled(false);
                    sendFileButton.setEnabled(false);
                    ipEditText.setEnabled(true);
                    portEditText.setEnabled(true);
                    usernameEditText.setEnabled(true);
                    passwordEditText.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> appendLog("Erreur lors de la déconnexion : " + e.getMessage(), Color.RED));
            }
        }).start();
    }

    private void toggleUDPMode() {
        isUDPMode = !isUDPMode;
        String mode = isUDPMode ? "UDP" : "TCP";
        appendLog("Mode de communication changé en : " + mode, Color.MAGENTA);
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Tous types de fichiers
        startActivityForResult(intent, 1); // Demande de résultat avec le code 1
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                sendFile(uri);
            }
        }
    }

    private void sendFile(Uri fileUri) {
        if (isUDPMode && udpSocket != null) {
            sendFileUDP(fileUri);
        } else if (tcpSocket != null) {
            sendFileTCP(fileUri);
        } else {
            appendLog("Vous devez d'abord vous connecter.", Color.RED);
        }
    }

    private void sendFileTCP(Uri fileUri) {
        new Thread(() -> {
            try {
                ContentResolver resolver = getContentResolver();
                InputStream inputStream = resolver.openInputStream(fileUri);
                if (inputStream == null) throw new IOException("Impossible d'ouvrir le fichier");

                OutputStream outputStream = tcpSocket.getOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                appendLog("Fichier envoyé via TCP.", Color.GREEN);

            } catch (IOException e) {
                appendLog("Erreur lors de l'envoi du fichier via TCP : " + e.getMessage(), Color.RED);
            }
        }).start();
    }

    private void sendFileUDP(Uri fileUri) {
        new Thread(() -> {
            try {
                ContentResolver resolver = getContentResolver();
                InputStream inputStream = resolver.openInputStream(fileUri);
                if (inputStream == null) throw new IOException("Impossible d'ouvrir le fichier");

                String serverIP = ipEditText.getText().toString().trim();
                int serverPort = Integer.parseInt(portEditText.getText().toString().trim());

                byte[] buffer = new byte[1024]; // Taille du paquet UDP
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(serverIP), serverPort);
                    udpSocket.send(packet);
                }
                inputStream.close();
                appendLog("Fichier envoyé via UDP.", Color.GREEN);

            } catch (IOException e) {
                appendLog("Erreur lors de l'envoi du fichier via UDP : " + e.getMessage(), Color.RED);
            }
        }).start();
    }

    private void appendLog(String message, int color) {
        String timestamp = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
        SpannableString spannableMessage = new SpannableString(timestamp + message + "\n");
        spannableMessage.setSpan(new ForegroundColorSpan(color), 0, spannableMessage.length(), 0);
        clientLogTextView.append(spannableMessage);
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(() -> {
            try {
                if (tcpSocket != null) tcpSocket.close();
                if (tcpOutput != null) tcpOutput.close();
                if (tcpInput != null) tcpInput.close();
                if (udpSocket != null) udpSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
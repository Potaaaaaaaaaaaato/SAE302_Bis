package com.tristan.sae302;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends AppCompatActivity {

    private TextView statusTextView;
    private ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        statusTextView = findViewById(R.id.statusTextView);

        // Lancer le serveur TCP dans un thread
        serverThread = new ServerThread(12345); // Port 12345
        serverThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverThread != null) {
            serverThread.stopServer();
        }
    }

    // Thread Serveur
    class ServerThread extends Thread {
        private int port;
        private boolean running = false;
        private ServerSocket serverSocket;

        public ServerThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                updateStatus("Serveur démarré sur le port : " + port);

                while (running) {
                    // Accepter une connexion client
                    Socket clientSocket = serverSocket.accept();
                    updateStatus("Client connecté : " + clientSocket.getInetAddress());

                    // Gérer le client dans un thread séparé
                    new ClientHandler(clientSocket).start();
                }
            } catch (IOException e) {
                updateStatus("Erreur serveur : " + e.getMessage());
            }
        }

        public void stopServer() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateStatus(String message) {
            runOnUiThread(() -> statusTextView.append("\n" + message));
        }
    }

    // Thread pour gérer chaque client
    class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                // Lire les données envoyées par le client
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

                String clientMessage;
                while ((clientMessage = input.readLine()) != null) {
                    updateStatus("Message reçu : " + clientMessage);

                    // Envoyer une réponse au client
                    output.println("Message reçu : " + clientMessage);
                }

                clientSocket.close();
                updateStatus("Client déconnecté");
            } catch (IOException e) {
                updateStatus("Erreur client : " + e.getMessage());
            }
        }

        private void updateStatus(String message) {
            runOnUiThread(() -> statusTextView.append("\n" + message));
        }
    }
}
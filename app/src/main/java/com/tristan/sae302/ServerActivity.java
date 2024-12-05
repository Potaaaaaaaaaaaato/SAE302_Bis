package com.tristan.sae302;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends AppCompatActivity {

    private TextView statusTextView;
    private ServerThread serverThread;
    private UDPServerThread udpServerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        statusTextView = findViewById(R.id.statusTextView);

        // Lancer les serveurs TCP et UDP dans des threads séparés
        serverThread = new ServerThread(12345); // Serveur TCP sur le port 12345
        serverThread.start();

        udpServerThread = new UDPServerThread(12346); // Serveur UDP sur le port 12346
        udpServerThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverThread != null) {
            serverThread.stopServer();
        }
        if (udpServerThread != null) {
            udpServerThread.stopServer();
        }
    }

    // Thread Serveur TCP
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
                updateStatus("Serveur TCP démarré sur le port : " + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    updateStatus("Client connecté : " + clientSocket.getInetAddress());
                    new ClientHandler(clientSocket).start();
                }
            } catch (IOException e) {
                updateStatus("Erreur serveur TCP : " + e.getMessage());
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

    // Thread Serveur UDP
    class UDPServerThread extends Thread {
        private int port;
        private boolean running = false;
        private DatagramSocket udpSocket;

        public UDPServerThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                udpSocket = new DatagramSocket(port);
                running = true;
                updateStatus("Serveur UDP démarré sur le port : " + port);

                byte[] buffer = new byte[1024];

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    updateStatus("Message UDP reçu : " + receivedMessage);

                    // Répondre au client
                    String responseMessage = "Message reçu : " + receivedMessage;
                    byte[] responseData = responseMessage.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length, packet.getAddress(), packet.getPort()
                    );
                    udpSocket.send(responsePacket);
                }
            } catch (IOException e) {
                updateStatus("Erreur serveur UDP : " + e.getMessage());
            }
        }

        public void stopServer() {
            running = false;
            if (udpSocket != null) {
                udpSocket.close();
            }
        }

        private void updateStatus(String message) {
            runOnUiThread(() -> statusTextView.append("\n" + message));
        }
    }

    // Thread pour gérer chaque client TCP
    class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

                String clientMessage;
                while ((clientMessage = input.readLine()) != null) {
                    updateStatus("Message TCP reçu : " + clientMessage);
                    output.println("Message reçu : " + clientMessage);
                }

                clientSocket.close();
                updateStatus("Client déconnecté");
            } catch (IOException e) {
                updateStatus("Erreur client TCP : " + e.getMessage());
            }
        }

        private void updateStatus(String message) {
            runOnUiThread(() -> statusTextView.append("\n" + message));
        }
    }
}
package com.tristan.sae302;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ServerActivity extends AppCompatActivity {

    private TextView statusTextView;
    private TextView ipValue;
    private TextView portValue;
    private ServerThread serverThread;
    private UDPServerThread udpServerThread;

    // Utilisateurs simulés pour l'authentification
    private static final Map<String, String> userDatabase = new HashMap<String, String>() {{
        put("user1", "password1");
        put("user2", "password2");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        statusTextView = findViewById(R.id.statusTextView);
        ipValue = findViewById(R.id.ipValue);
        portValue = findViewById(R.id.portValue);

        // Obtenir l'IP locale
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ipValue.setText(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("ServerActivity", "Erreur lors de la récupération de l'adresse IP locale", e);
            ipValue.setText("Erreur : IP non disponible");
        }

        // Afficher les ports sur lesquels les serveurs écoutent
        portValue.setText(String.valueOf(12345)); // Port TCP
        updateStatus("Serveur TCP démarré sur le port : 12345");

        serverThread = new ServerThread(12345);
        serverThread.start();

        udpServerThread = new UDPServerThread(12346);
        udpServerThread.start();
        updateStatus("Serveur UDP démarré sur le port : 12346");
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

                    // Vérification d'authentification
                    String authInfo = input.readLine();
                    if (authInfo != null) {
                        String[] credentials = authInfo.split(",");
                        if (credentials.length == 2 && authenticateUser(credentials[0], credentials[1])) {
                            output.println("Success");
                            updateStatus("Authentification réussie pour " + credentials[0]);

                            // Gestion des messages après authentification
                            String clientMessage;
                            while ((clientMessage = input.readLine()) != null) {
                                // Analyse du message pour déterminer le service demandé
                                String[] messageParts = clientMessage.split(":");
                                if (messageParts.length == 2) {
                                    String serviceType = messageParts[0];
                                    String messageContent = messageParts[1];
                                    handleService(serviceType, messageContent, output);
                                } else {
                                    updateStatus("Message mal formé : " + clientMessage);
                                    output.println("Message mal formé. Utilisez le format 'ServiceType:Message'");
                                }
                            }
                        } else {
                            output.println("Failure");
                            updateStatus("Authentification échouée pour " + (credentials.length > 0 ? credentials[0] : "unknown"));
                            clientSocket.close();
                            return;
                        }
                    }

                    clientSocket.close();
                    updateStatus("Client déconnecté");
                } catch (IOException e) {
                    updateStatus("Erreur client TCP : " + e.getMessage());
                }
            }

            private void handleService(String serviceType, String messageContent, PrintWriter output) {
                switch (serviceType.toLowerCase()) {
                    case "echo":
                        output.println("Echo: " + messageContent);
                        updateStatus("Service Echo utilisé : " + messageContent);
                        break;
                    case "reverse":
                        StringBuilder reversed = new StringBuilder(messageContent).reverse();
                        output.println("Reverse: " + reversed.toString());
                        updateStatus("Service Reverse utilisé : " + messageContent);
                        break;
                    case "uppercase":
                        output.println("Uppercase: " + messageContent.toUpperCase());
                        updateStatus("Service Uppercase utilisé : " + messageContent);
                        break;
                    default:
                        output.println("Service non reconnu : " + serviceType);
                        updateStatus("Service non reconnu demandé : " + serviceType);
                }
            }

            private boolean authenticateUser(String username, String password) {
                return userDatabase.containsKey(username) && userDatabase.get(username).equals(password);
            }
        }
    }

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

    private void updateStatus(String message) {
        runOnUiThread(() -> statusTextView.append("\n" + message));
    }
}
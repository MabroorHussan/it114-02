package Homework.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Server {
    int port = 3001;
    private List<ServerThread> clients = new ArrayList<>();

    private boolean gameActive = false;
    private int hiddenNumber;
    
    private void start(int port) {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                System.out.println("Waiting for next client");
                Socket incoming_client = serverSocket.accept();
                System.out.println("Client connected");
                ServerThread sClient = new ServerThread(incoming_client, this);
                clients.add(sClient);
                sClient.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("Closing server socket");
        }
    }

    protected synchronized void disconnect(ServerThread client) {
        long id = client.getId();
        client.disconnect();
        broadcast("Disconnected", id);
    }

    protected synchronized void broadcast(String message, long id) {
        if (processCommand(message, id)) {
            return;
        }
        message = String.format("User[%d]: %s", id, message);
        Iterator<ServerThread> it = clients.iterator();
        while (it.hasNext()) {
            ServerThread client = it.next();
            boolean wasSuccessful = client.send(message);
            if (!wasSuccessful) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getId()));
                it.remove();
                broadcast("Disconnected", id);
            }
        }
    }

    private boolean processCommand(String message, long clientId) {
        System.out.println("Checking command: " + message);
        if (message.equalsIgnoreCase("/disconnect")) {
            Iterator<ServerThread> it = clients.iterator();
            while (it.hasNext()) {
                ServerThread client = it.next();
                if (client.getId() == clientId) {
                    it.remove();
                    disconnect(client);
                    break;
                }
            }
            return true;
        } else if (message.equalsIgnoreCase("/start")) {
            if (!gameActive) {
                gameActive = true;
                // Generate a random number between 1 and 100 as the hidden number
                hiddenNumber = new Random().nextInt(100) + 1;
                broadcast("Number guesser game started! Guess a number between 1 and 100.", clientId);
            }
            return true;
        } else if (message.equalsIgnoreCase("/stop")) {
            if (gameActive) {
                gameActive = false;
                broadcast("Number guesser game stopped. Guesses will be treated as regular messages.", clientId);
            }
            return true;
        } else if (message.toLowerCase().startsWith("/guess")) {
            if (gameActive) {
                try {
                    int guess = Integer.parseInt(message.substring(7).trim());
                    String guessResult = (guess == hiddenNumber) ? "correct!" : "incorrect.";
                    broadcast("User[" + clientId + "] guessed " + guess + ", which was " + guessResult, clientId);
                } catch (NumberFormatException e) {
                    broadcast("Invalid guess format. Please provide a number.", clientId);
                }
            } else {
                broadcast("Game is not active. Please start the game first.", clientId);
            }
            return true;
        } else if (message.equalsIgnoreCase("/flip") || message.equalsIgnoreCase("/toss") || message.equalsIgnoreCase("/coin")) {
            flipCoin(clientId);
            return true;
        }
        return false;
    }

    private void flipCoin(long clientId) {
        // Perform a coin toss
        String result = (Math.random() < 0.5) ? "Heads" : "Tails";
        broadcast("User[" + clientId + "] flipped a coin and got " + result, clientId);
    }

    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // Ignore - will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
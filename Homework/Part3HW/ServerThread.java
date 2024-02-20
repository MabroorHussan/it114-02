package Homework.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client;
    private boolean isRunning = false;
    private ObjectOutputStream out; // Initialize this object
    private Server server; // ref to our server so we can call methods on it more easily

    public ServerThread(Socket myClient, Server server) {
        // get communication channels to single client
        this.client = myClient;
        this.server = server;
        try {
            // Initialize the ObjectOutputStream
            this.out = new ObjectOutputStream(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        isRunning = false;
        cleanup();
    }

    public boolean send(String message) {
        // added a boolean so we can see if the send was successful
        try {
            out.writeObject(message);
            return true;
        } catch (IOException e) {
            // comment this out to inspect the stack trace
            // e.printStackTrace();
            cleanup();
            return false;
        }
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            isRunning = true;
            String fromClient;
            while (isRunning && (fromClient = (String) in.readObject()) != null) {
                server.broadcast(fromClient, this.getId());
            }
        } catch (Exception e) {
            // happens when client disconnects
            e.printStackTrace();
        } finally {
            isRunning = false;
            cleanup();
        }
    }

    private void cleanup() {
        try {
            client.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}

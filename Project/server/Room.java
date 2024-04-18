package Project.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import Project.common.Constants;


public class Room implements AutoCloseable {
    protected static Server server;// used to refer to accessible server functions
    private String name;
    private List<ServerThread> clients = new ArrayList<ServerThread>();
    private boolean isRunning = false;
    // Commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String JOIN_ROOM = "joinroom";
    private final static String DISCONNECT = "disconnect";
    private final static String LOGOUT = "logout";
    private final static String LOGOFF = "logoff";
    private static Logger logger = Logger.getLogger(Room.class.getName());




    // Regular expressions for text formatting
    private static final String BOLD_REGEX = "\\*([^*]+)\\*";
    private static final String ITALICS_REGEX = "-([^\\-]+)-";
    private static final String UNDERLINE_REGEX = "_([^_]+)_";
    private static final String COLOR_REGEX = "\\[([rgbsilver]*)\\s([a-zA-Z]+)\\s([rgbsilver]*)\\]";
   
    public Room(String name) {
        this.name = name;
        isRunning = true;
    }


    public String getName() {
        return name;
    }


    public boolean isRunning() {
        return isRunning;
    }


   


    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        client.setCurrentRoom(this);
        if (clients.indexOf(client) > -1) {
            logger.warning("Attempting to add client that already exists in room");
        } else {
            clients.add(client);
            client.sendResetUserList();
            syncCurrentUsers(client);
            sendConnectionStatus(client, true);
        }
    }


    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        // attempt to remove client from room
        try {
            clients.remove(client);
        } catch (Exception e) {
            logger.severe(String.format("Error removing client from room %s", e.getMessage()));
            e.printStackTrace();
        }
        // if there are still clients tell them this person left
        if (clients.size() > 0) {
            sendConnectionStatus(client, false);
        }
        checkClients();
    }


    private void syncCurrentUsers(ServerThread client) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread existingClient = iter.next();
            if (existingClient.getClientId() == client.getClientId()) {
                continue;// don't sync ourselves
            }
            boolean messageSent = client.sendExistingClient(existingClient.getClientId(),
                    existingClient.getClientName());
            if (!messageSent) {
                handleDisconnect(iter, existingClient);
                break;// since it's only 1 client receiving all the data, break if any 1 send fails
            }
        }
    }


    /***
     * Checks the number of clients.
     * If zero, begins the cleanup process to dispose of the room
     */
    private void checkClients() {
        // Cleanup if room is empty and not lobby
        if (!name.equalsIgnoreCase(Constants.LOBBY) && (clients == null || clients.size() == 0)) {
            close();
        }
    }


    /***
     * Helper function to process messages to trigger different functionality.
     *
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    @Deprecated // not used in my project as of this lesson, keeping it here in case things
                // change
    private boolean processCommands(String message, ServerThread client) { // UCID: mth39, Date: 04/17/24, Milestone 3
        boolean wasCommand = false;
   
        try {
            if (message.startsWith(COMMAND_TRIGGER)) {
                String[] comm = message.split(COMMAND_TRIGGER);
                String part1 = comm[1];
                String[] comm2 = part1.split(" ");
                String command = comm2[0];
                String roomName;
                wasCommand = true;
   
                switch (command) {
                    case CREATE_ROOM:
                        roomName = comm2[1];
                        Room.createRoom(roomName, client);
                        break;
                    case JOIN_ROOM:
                        roomName = comm2[1];
                        Room.joinRoom(roomName, client);
                        break;
                    case DISCONNECT:
                    case LOGOUT:
                    case LOGOFF:
                        Room.disconnectClient(client, this);
                        break;
                    case "flip":
                        processFlipCommand(client);
                        break;
                    case "roll":
                        String rollCommand = comm2[1];
                        processRollCommand(client, rollCommand);
                        break;
                    case "mute":
                        String targetUsername = comm2[1];
                        processMuteCommand(client, targetUsername);
                        break;
                    case "unmute":
                        targetUsername = comm2[1];
                        processUnmuteCommand(client, targetUsername);
                        break;
                    default:
                        wasCommand = false;
                        break;
                }
            } else if (message.startsWith("@")) { // UCID: mth39, Date: 04/17/24, Milestone 3
                // Whisper command
                String[] parts = message.split(" ", 2);
                if (parts.length == 2) {
                    String targetUsername = parts[0].substring(1); // Remove the "@" symbol
                    String whisperMessage = parts[1];
                    sendWhisperMessage(client, targetUsername, whisperMessage);
                    wasCommand = true;
                } else {
                    // Invalid whisper command
                    client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid whisper command format.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
   
        return wasCommand;
    }// UCID: mth39, Date: 04/17/24, Milestone 3


    private void processMuteCommand(ServerThread sender, String targetUsername) {
        // Check if the sender has the authority to mute
        // For example, you may want to add a check like if (sender.isAdmin()) { ... }
        if (sender != null && targetUsername != null && !targetUsername.isEmpty()) {
            // Find the target ServerThread
            ServerThread targetClient = findClientByUsername(targetUsername);
   
            if (targetClient != null) {
                // Mute the target user
                targetClient.mute(targetUsername);
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "User '" + targetUsername + "' has been muted.");
               
            // mute message occurs here, as the muted user is informed about the mute.
            targetClient.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have been muted by " + sender.getClientName()); // UCID: mth39, Date: 04/17/24, Milestone 3


            } else {
                // Target user not found
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "User '" + targetUsername + "' not found.");
            }
        }
    }
   
    private void processUnmuteCommand(ServerThread sender, String targetUsername) {
        // Check if the sender has the authority to unmute
        // For example, you may want to add a check like if (sender.isAdmin()) { ... }
        if (sender != null && targetUsername != null && !targetUsername.isEmpty()) {
            // Find the target ServerThread
            ServerThread targetClient = findClientByUsername(targetUsername);
   
            if (targetClient != null) {
                // Unmute the target user
                targetClient.unmute(targetUsername);
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "User '" + targetUsername + "' has been unmuted.");
           
            // unmute message occurs here, as the unmuted user is informed about the unmute.
            targetClient.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have been unmuted by " + sender.getClientName()); // UCID: mth39, Date: 04/17/24, Milestone 3


            } else {
                // Target user not found
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "User '" + targetUsername + "' not found.");
            }
        }
    }
   
    private void sendWhisperMessage(ServerThread sender, String targetUsername, String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        // Find the target ServerThread
        ServerThread targetClient = findClientByUsername(targetUsername);
   
        if (targetClient != null) {
            // Send the whisper message to both sender and target
            sender.sendMessage(sender.getClientId(), "[Whisper to " + targetUsername + "]: " + message);
            targetClient.sendMessage(sender.getClientId(), "[Whisper from " + sender.getClientName() + "]: " + message);
        } else {
            // Target user not found
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "User '" + targetUsername + "' not found.");
        }
    } // UCID: mth39, Date: 04/17/24, Milestone 3
   
    private ServerThread findClientByUsername(String username) { // UCID: mth39, Date: 04/17/24, Milestone 3
        for (ServerThread client : clients) {
            if (client.getClientName().equalsIgnoreCase(username)) {
                return client;
            }
        }
        return null; // Target user not found
    } // UCID: mth39, Date: 04/17/24, Milestone 3


    private void processRollCommand(ServerThread sender, String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        // Remove the command prefix
        String rollCommand = message;


        // Check if it's in Format 1: /roll # or Format 2: /roll #d#
        Pattern format1Pattern = Pattern.compile("(\\d+)");
        Pattern format2Pattern = Pattern.compile("(\\d+)d(\\d+)");
   
        Matcher format2Matcher = format2Pattern.matcher(rollCommand);
        Matcher format1Matcher = format1Pattern.matcher(rollCommand);
   
        if (format2Matcher.matches()) {
            int numberOfDice = Integer.parseInt(format2Matcher.group(1));
            int sides = Integer.parseInt(format2Matcher.group(2));
            int total = 0;
            StringBuilder result = new StringBuilder("Rolled: [");
   
            for (int i = 0; i < numberOfDice; i++) {
                int roll = (int) (Math.random() * sides) + 1;
                result.append(roll);
   
                if (i < numberOfDice - 1) {
                    result.append(", ");
                }
   
                total += roll;
            }
   
            result.append("] Total: <b>").append(total).append("</b>");
            sendMessage(sender, ("<i>" + result.toString() + "<i>"));
        } else if (format1Matcher.matches()) {
            int upperBound = Integer.parseInt(format1Matcher.group(1));
            int result = (int) (Math.random() * upperBound) + 1;
            sendMessage(sender, "Rolled: <b>" + result + "</b>");
        } else {
            sendMessage(sender, "<i>Invalid roll command. Please use /roll # or /roll #d#.</i>");
        }
    }
    private void processFlipCommand(ServerThread sender) { // UCID: mth39, Date: 04/17/24, Milestone 3
        // Simulate a coin flip
        String result = (Math.random() < 0.5) ? "Heads" : "Tails";
        sendMessage(sender, "Flipped: <b>" + result + "</b>");
    } // UCID: mth39, Date: 04/17/24, Milestone 3


    // Command helper methods
    protected static void getRooms(String query, ServerThread client) {
        String[] rooms = Server.INSTANCE.getRooms(query).toArray(new String[0]);
        client.sendRoomsList(rooms,
                (rooms != null && rooms.length == 0) ? "No rooms found containing your query string" : null);
    }


    protected static void createRoom(String roomName, ServerThread client) {
        if (server.createNewRoom(roomName)) {
            Room.joinRoom(roomName, client);
        } else {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s already exists", roomName));
        }
    }


    /**
     * Will cause the client to leave the current room and be moved to the new room
     * if applicable
     *
     * @param roomName
     * @param client
     */
    protected static void joinRoom(String roomName, ServerThread client) {
        if (!server.joinRoom(roomName, client)) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s doesn't exist", roomName));
        }
    }


    protected static void disconnectClient(ServerThread client, Room room) {
        client.disconnect();
        room.removeClient(client);
    }
    // end command helper methods


    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     *
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */


     
     private String processBold(String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        System.out.println("Source Message (Bold): " + message);
        // Print the original message for debugging


        // Define the regular expression pattern for detecting bold formatting
        Pattern pattern = Pattern.compile(BOLD_REGEX);
        // Create a matcher object to find matches in the message
        Matcher matcher = pattern.matcher(message);


        // Iterate through all matches in the message
        while (matcher.find()) {
            // Create bold-formatted text using the matched content
            String boldText = "<b>" + matcher.group(1) + "</b>";
            // Replace the original matched content with the formatted text
            message = message.replace(matcher.group(0), boldText);
        }


        // Print the formatted message for debugging
        System.out.println("Formatted Message (Bold): " + message);
        return message;
    } // UCID: mth39, Date: 04/17/24, Milestone 3


     private String processItalics(String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        // Print the original message for debugging
        System.out.println("Source Message (Italics): " + message);
        // Define the regular expression pattern for detecting italics formatting
        Pattern pattern = Pattern.compile(ITALICS_REGEX);
        // Create a matcher object to find matches in the message
        Matcher matcher = pattern.matcher(message);


        // Iterate through all matches in the message
        while (matcher.find()) {
            // Create italics-formatted text using the matched content
            String italicText = "<i>" + matcher.group(1) + "</i>";
            // Replace the original matched content with the formatted text
            message = message.replace(matcher.group(0), italicText);
        }
        // Print the formatted message for debugging
        System.out.println("Formatted Message (Italics): " + message);
        return message;
    } // UCID: mth39, Date: 04/17/24, Milestone 3


    private String processUnderline(String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        System.out.println("Source Message (Underline): " + message);
        Pattern pattern = Pattern.compile(UNDERLINE_REGEX);
        Matcher matcher = pattern.matcher(message);


        while (matcher.find()) {
            String underlineText = "<u>" + matcher.group(1) + "</u>";
            message = message.replace(matcher.group(0), underlineText);
        }


        System.out.println("Formatted Message (Underline): " + message);
        return message;
    } // UCID: mth39, Date: 04/17/24, Milestone 3


        private String processColor(String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        System.out.println("Source Message (Color): " + message);


        message = message.replace("[r", "<font color=red>").replace("r]","</font>");
        message = message.replace("[g", "<font color=green>").replace("g]","</font>");
        message = message.replace("[b", "<font color=blue>").replace("b]","</font>");        
           
        System.out.println("Formatted Message (Color): " + message);
        return message;
    } // UCID: mth39, Date: 04/17/24, Milestone 3


    private String formatMessage(String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        System.out.println("Source Message: " + message);
        message = processBold(message);
        message = processItalics(message);
        message = processUnderline(message);
        message = processColor(message);


        System.out.println("Formatted Message: " + message);
        return message;
    } // UCID: mth39, Date: 04/17/24, Milestone 3


    public void processTextFormatting(ServerThread sender, String message) { // UCID: mth39, Date: 04/17/24, Milestone 3
        String formattedMessage = formatMessage(message);
        sendMessage(sender, formattedMessage);
    } // UCID: mth39, Date: 04/17/24, Milestone 3




    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
   
        if (sender != null && processCommands(message, sender)) {
            // It was a command, don't broadcast
            return;
        }
   
        message = formatMessage(message); // Format the message for text display
        long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
   
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
   
            // Check if the sender is muted by the client in the current iteration
            if (client.isMuted(sender.getClientName())) {
                // Skip broadcasting to muted clients
                continue;
            }
   
            boolean messageSent = client.sendMessage(from, message);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
   
        // Broadcast the message to the muted sender as well
        if (sender != null && sender.isMuted(sender.getClientName())) {
            sender.sendMessage(from, message);
        }
    }
   
    protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread receivingClient = iter.next();
            boolean messageSent = receivingClient.sendConnectionStatus(
                    sender.getClientId(),
                    sender.getClientName(),
                    isConnected);
            if (!messageSent) {
                handleDisconnect(iter, receivingClient);
            }
        }
    }


    private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
        iter.remove();
        logger.info(String.format("Removed client %s", client.getClientName()));
        sendMessage(null, client.getClientName() + " disconnected");
        checkClients();
    }


    public void close() {
        server.removeRoom(this);
        isRunning = false;
        clients.clear();
    }
   
}

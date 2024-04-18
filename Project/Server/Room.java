package Project.Server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import Project.Common.Constants;

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
	private final static String ROLL = "roll";
	private final static String FLIP = "flip";
	private final static String BOLD = "bold";
	private final static String ITALIC = "italic";
	private final static String COLOR = "color";
	private final static String UNDERLINE = "underline";
	private final static String MUTE = "mute";
	private final static String UNMUTE = "unmute";
	private static Logger logger = Logger.getLogger(Room.class.getName());

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
		logger.info("Room addClient called");
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
		if (!name.equalsIgnoreCase("lobby") && clients.size() == 0) {
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
	private boolean processCommands(String message, ServerThread client) {
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

					case ROLL: // mth39 04/03/24
						int max = Integer.parseInt(comm2[1]);
						Random rand = new Random();
						int result = rand.nextInt(max + 1);
						sendMessage(client, "/roll: " + result);
						break;
					case FLIP:
						Random rando = new Random();
						int results = rando.nextInt(2);
						if (results == 0) {
							sendMessage(client, "/flip: heads");
						} else {
							sendMessage(client, "/flip: tails");
						}
						break;

					case BOLD: // mth39 04/03/24
						String boldMessage = message.replace("*/", "<b>");
						boldMessage = boldMessage.replace("/*", "</b>");
						sendMessage(client, boldMessage);
						break;

					case ITALIC:
						String italicMessage = message.replace("&_", "<i>");
						italicMessage = italicMessage.replace("_&", "</i>");
						sendMessage(client, italicMessage);
						break;

					case COLOR:
						String color = comm2[1];
						String colorMessage = message.replace(color, "<font color='" + color + "'>");
						colorMessage = colorMessage.replace("#", "</font>");
						sendMessage(client, colorMessage);
						break;

					case UNDERLINE:
						String underlineMessage = message.replace("_/", "<u>");
						underlineMessage = underlineMessage.replace("/_", "</u>");
						sendMessage(client, underlineMessage);

					case MUTE:
					    break;

					case UNMUTE:
					    break;

					default:
						sendMessage(client, "Unrecognized command: " + command);
						break;
				}

			}

			else if (message.startsWith("@")) {  //mth39 04/03/24
				String[] splitMessage = message.split(" ");
				String username = splitMessage[0].substring(1);
				String privateMessage = "";
				
				for (int i = 1; i < splitMessage.length; i++) {
					privateMessage += splitMessage[i] + " ";
				}
				privateMessage = privateMessage.trim();
				for (ServerThread st : clients) {
					if (st.getClientName().equals(username)) {
						st.sendMessage(privateMessage, client.getClientName());
						wasCommand = true;
						break;
					}
				}
			if (!wasCommand) {
				client.sendError("No user found with the specified username.");
			}
		}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return wasCommand;
	}

	protected static void getRooms(String query, ServerThread client) {
        String[] rooms = Server.INSTANCE.getRooms(query).toArray(new String[0]);
        client.sendRoomsList(rooms,
                (rooms != null && rooms.length == 0) ? "No rooms found containing your query string" : null);
    }

	// Command helper methods
	protected static void createRoom(String roomName, ServerThread client) {
        if (Server.INSTANCE.createNewRoom(roomName)) {
            Room.joinRoom(roomName, client);
        } else {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s already exists", roomName));
        }
    }

	protected static void joinRoom(String roomName, ServerThread client) {
        if (!Server.INSTANCE.joinRoom(roomName, client)) {
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
	protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
        logger.info(String.format("Sending message to %s clients", clients.size()));
        if (sender != null && processCommands(message, sender)) {
            // it was a command, don't broadcast
            return;
        }
        //is private message
        //filter message
        long from = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendMessage(from, message);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
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

	protected void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
        if (iter != null) {
            iter.remove();
        } else {
            Iterator<ServerThread> iter2 = clients.iterator();
            while (iter2.hasNext()) {
                ServerThread th = iter2.next();
                if (th.getClientId() == client.getClientId()) {
                    iter2.remove();
                    break;
                }
            }
        }
        logger.info(String.format("Removed client %s", client.getClientName()));
        sendMessage(null, client.getClientName() + " disconnected");
        checkClients();
    }


	public void close() {
        Server.INSTANCE.removeRoom(this);
        isRunning = false;
        clients.clear();
    }
}
if (client.getId() == receiverId || client.getId() == senderId)

else if (message.startsWith(prefix:"@")){
    String target = message.replace(target:"@", replacement:"").trim();
    long receiver = Long.parseLong(target);
    return sendPM(message, clientID, receiver);
}
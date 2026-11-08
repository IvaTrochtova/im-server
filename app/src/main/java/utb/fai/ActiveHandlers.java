package utb.fai;

import java.util.*;

public class ActiveHandlers {
    private final Set<SocketHandler> clients = new HashSet<>();
    private final Map<String, SocketHandler> names = new HashMap<>();
    
    synchronized void add(SocketHandler client) {
        clients.add(client);
        client.groups.add("public");
    }

    synchronized void remove(SocketHandler client) {
        clients.remove(client);
        if (client.name != null && names.get(client.name) == client) {
            names.remove(client.name);
        }
    }
    synchronized boolean setName(SocketHandler client, String newName) {
        if (newName == null || newName.isEmpty() || newName.matches(".*\\s+.*")) return false;
        SocketHandler owner = names.get(newName);
        if (owner != null && owner != client) return false;
        if (client.name != null) names.remove(client.name);
        client.name = newName;
        names.put(newName, client);
        return true;
    }

    /**
     * sendMessageToAll - Pole zprávu vem aktivním klientùm kromì sebe sama
     * 
     * @param sender  - reference odesílatele
     * @param message - øetìzec se zprávou
     */
    synchronized void sendMessageToAll(SocketHandler sender, String message) {
        String formatted = "[" + sender.name + "] >> " + message;
        for (SocketHandler recipient : clients) { // pro vechny aktivní handlery
            if (recipient == sender) continue;
            if (!Collections.disjoint(sender.groups, recipient.groups)){ // zkus pøidat zprávu do fronty jeho zpráv
                recipient.messages.offer(formatted);
            }
        }
    }
    synchronized void sendPrivate(SocketHandler sender, String recipientName, String message) {
        SocketHandler recipient = names.get(recipientName);
        if (recipient != null && recipient != sender) {
            recipient.messages.offer("[" + sender.name + "] >> " + message);
        }
    }

        synchronized void join(SocketHandler client, String group) {
        if (group != null && !group.isEmpty()) client.groups.add(group);
    }

    /**
     * remove odebere z mnoiny aktivních handlerù nový handler.
     * Metoda je sychronizovaná, protoe HashSet neumí multithreading.
     * 
     * @param handler - reference na handler, který se má odstranit
     * @return true if the set did not already contain the specified element.
     */
    synchronized boolean remove(SocketHandler client, String group) {
        return client.groups.remove(group);
    }

    synchronized String groups(SocketHandler client) {
    ArrayList<String> ordered = new ArrayList<>(client.groups);
    Collections.sort(ordered);
    return String.join(", ", ordered);
    }

    synchronized void leave(SocketHandler client, String group) {
    client.groups.remove(group);
    }
}

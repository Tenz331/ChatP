package api;

import Controller.DBConnector;
import Server.ClientHandler;
import domain.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ChatP {
    private final DBConnector db;
    private final List<ClientHandler> handlers;
    private final HashMap<Integer, List<String>> activeUsersByChannel = new HashMap<>();

    public ChatP() {
        this(DBConnector.getInstance(), new ArrayList<>());
    }

    public ChatP(DBConnector connector, List<ClientHandler> handlers) {
        this.db = connector;
        this.handlers = handlers;
    }

    public void addClient(ClientHandler clientThread) {
        handlers.add(clientThread);
    }

    public Iterable<ClientHandler> getClientHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    public Iterable<String> getActiveUsers(int channel) {
        return Collections.unmodifiableList(activeUsersByChannel.get(channel));
    }

    public void setActiveChannel(String user, int channel) {
        for (List<String> users : activeUsersByChannel.values()) {
            users.remove(user);
        }
        var list = activeUsersByChannel.get(channel);
        if (list == null) {
            list = new ArrayList<>();
            activeUsersByChannel.put(channel, list);
        }
        list.add(user);
    }

    public void sendMessage(Integer user_id, String userInput, int channel_id) {
        db.addMessage(user_id, userInput, channel_id);
    }

    public boolean checkIfUserIsLoggedIn(String username) {
        for (var list : activeUsersByChannel.values()) {
            if (list.contains(username)) return true;
        }
        return false;
    }

    public boolean checkAvailableUsername(String username) {
        return findUser(username) != null;
    }

    public User findUser(String username) {
        try (var conn = db.getConnection()) {
            PreparedStatement loginStmt = conn.prepareStatement(
                    "SELECT id, username, password FROM users WHERE username=?");
            loginStmt.setString(1, username);
            ResultSet rs = loginStmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username")
                        , rs.getString("password"));
            } else {
                return null;
            }
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    public void login(User user) {
        setActiveChannel(user.getName(), 1);
    }

    public void logoff(String user) {
        for (List<String> users : activeUsersByChannel.values()) {
            users.remove(user);
        }
    }

    public int getActiveChannelOf(String clientUserName) {
        for (var entry : activeUsersByChannel.entrySet()) {
            if (entry.getValue().contains(clientUserName)) {
                return entry.getKey();
            }
        }
        throw new NoSuchElementException(clientUserName);
    }
}
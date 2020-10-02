package Server;


import Controller.DBConnector;
import Util.Encryptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientHandler implements Runnable {
    public HashMap<Integer, List<String>> channelOccupancy;
    public static ArrayList<String> users; //temp player info
    private String clientUserName; //clients - players username
    private final BufferedReader in;
    private final PrintWriter out;
    private final ArrayList<ClientHandler> clients;
    LocalDateTime time;
    Integer user_id;
    boolean running = false;
    private int channel_id;
    private static final Connection connection = DBConnector.getInstance().getConnection();

    public ClientHandler(Socket ClientSocket, ArrayList<ClientHandler> clients) throws IOException { //constructor
        users = new ArrayList<>();
        channelOccupancy = new HashMap<>();
        this.clients = clients;
        in = new BufferedReader((new InputStreamReader(ClientSocket.getInputStream())));
        out = new PrintWriter(ClientSocket.getOutputStream(), true);
    }

    @Override
    public void run() { //thread start for client
        try {
            login();
            joinChannel("main");
            while (running) {
                String userInput = in.readLine();
                if (!userInput.startsWith("!")) { //player broadcast msg
                    //Insert message into DB
                    String insertMsgQuery = "INSERT INTO ChadChat.log(user_ID, msg, channel_id) VALUES (?, ?, ?)";
                    PreparedStatement stmt = connection.prepareStatement(insertMsgQuery);
                    stmt.setInt(1, user_id);
                    stmt.setString(2, userInput);
                    stmt.setInt(3, channel_id);
                    stmt.execute();

                    //Timestamp for chat window
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
                    time = LocalDateTime.now();
                    String timestamp = "[" + format.format(time) + "]";
                    userBroadCast(timestamp, userInput, channel_id);

                } else if (userInput.startsWith("!")) {
                    String[] userCommand = userInput.split(" ");
                    switch (userCommand[0].toLowerCase().substring(1)) {
                        case "users":
                            out.println("Current users: " + getUsersInLobby().toString());
                            System.out.println(channelOccupancy.get(channel_id).toString());
                            break;
                        case "quit":
                            removeUser(clientUserName);
                            run();
                            break;
                        case "join":
                            serverBroadCast(clientUserName + " has left the channel");
                            joinChannel(userCommand[1].toLowerCase());
                            break;
                        case "channels":
                            out.println("Available channels: " + getChannelNames().toString());
                            break;
                        default:
                            out.println("Invalid command");
                            break;
                    }
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (removeUser(this.clientUserName)) {
                    serverBroadCast(this.clientUserName + " has left");
                    this.clientUserName = null;
                    running = false;
                }
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> getChannelNames() {
        ArrayList<String> channelNames = new ArrayList<>();
        String getChannelNameQuery = "SELECT name FROM channel";
        try {
            Statement channelNameStmt = connection.createStatement();
            ResultSet channelNameRS = channelNameStmt.executeQuery(getChannelNameQuery);
                while (channelNameRS.next()) {
                    String channelName = channelNameRS.getString(1);
                    channelNames.add(channelName.substring(0,1).toUpperCase() + channelName.substring(1));
                }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return channelNames;
    }

    private void serverBroadCast(String substring) { //server broadcast
        for (ClientHandler clientHandler : clients) {
            if (Objects.equals(clientHandler.channel_id, this.channel_id)) {
                if (!Objects.equals(clientHandler.clientUserName, this.clientUserName)) {
                    clientHandler.out.println("[SERVER] " + substring);
                }
            }
        }
    }

    private void userBroadCast(String timestamp, String substring, int channelID) { //player - client broadcast
        for (ClientHandler clientHandler : clients) {
            if (Objects.equals(clientHandler.channel_id, channelID)) {
                if (!Objects.equals(clientHandler.clientUserName, this.clientUserName)) {
                    clientHandler.out.println(timestamp + clientUserName + ": " + substring);
                } else {
                    clientHandler.out.println(timestamp + "You: " + substring);
                }
            }
        }
    }

    public static ArrayList<String> getUsersInLobby() { //gets users on server;
        return users;
    }

    public boolean removeUser(String user) { //remove user from server
        if (users.contains(user)){
            users.remove(user);
            System.out.println("[SERVER] removed: "+ user);
            return true;
        } else {
            return false;
        }
    }

    public boolean validatePassword(String password1, String password2) {
        return password1.equals(password2);
    }

    private void login() throws IOException, SQLException {
        out.println("What is your username?:");
        String username = in.readLine();
        if (users.stream().noneMatch(username::equalsIgnoreCase)) { //check if username is allowed / enabled < - >
            PreparedStatement loginStmt = connection.prepareStatement("SELECT id, username FROM users WHERE username=?");
            loginStmt.setString(1, username);
            ResultSet rs = loginStmt.executeQuery();
            if (rs.next()) {
                user_id = rs.getInt(1);
                if (rs.getString("username").toLowerCase().equals(username.toLowerCase())) {
                    out.println("User already exists!");
                    out.println("Enter password to continue");
                    String password = Encryptor.SHA256(in.readLine());
                    PreparedStatement passwordStmt = connection.prepareStatement("SELECT password FROM users WHERE username=?");
                    passwordStmt.setString(1, username);
                    ResultSet passRs = passwordStmt.executeQuery();
                    if (passRs.next()) {
                        if (validatePassword(password, passRs.getString(1))) {
                            out.println("Login successful");
                            users.add(rs.getString("username"));
                            this.clientUserName = rs.getString("username");
                            user_id = rs.getInt(1);
                            channel_id = 1;
                            addToList(channel_id, this.clientUserName);
                        } else {
                            out.println("Wrong password!");
                            out.println();
                            login();
                        }
                    }
                }
            } else {
                createNewUser(username);
            }
        }
        else {
            out.println("User is already logged in!");
            login();
        }
        running = true;
    }
    private void createNewUser(String username) throws IOException, SQLException {
        out.println("Creating new user!");
        out.println("Enter desired password");
        String password1 = Encryptor.SHA256(in.readLine());
        out.println("Enter password again");
        String password2 = Encryptor.SHA256(in.readLine());
        if (validatePassword(password1, password2)) {
            PreparedStatement newUser = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            newUser.setString(1, username);
            newUser.setString(2, password1);
            newUser.executeUpdate();
            try (ResultSet generatedKeys = newUser.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user_id = generatedKeys.getInt(1);
                }
            }
            System.out.println(user_id);
            out.println("Created new user: " + username);
            users.add(username);
            this.clientUserName = username;
            channel_id = 1;
            addToList(channel_id, username);
        }
        else {
            out.println("Passwords did not match! Try again.");
            createNewUser(username);
        }
    }

    public void getLastLogEntries(int channelID) {
        //Gets last 10 entries in the channel
        String joinChannelQuery = "SELECT * FROM (SELECT log.entry_id, users.username, log.timestamp, log.msg, log.channel_id " +
                "FROM log INNER JOIN users ON log.user_ID = users.ID " +
                "WHERE channel_id = " + channelID + " ORDER BY entry_ID DESC LIMIT 10)" +
                "sub1 ORDER BY entry_ID ASC;";
        try {
            Statement joinChannelStmt = connection.createStatement();
            ResultSet logRs = joinChannelStmt.executeQuery(joinChannelQuery);
            while (logRs.next()) {
                String username = logRs.getString(2);
                String timestamp = logRs.getString(3).substring(0, 16);
                String message = logRs.getNString(4);
                if (username.equals(this.clientUserName)) {
                    username = "You";
                }
                out.println("[" + timestamp + "] " + username + ": " + message);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void joinChannel(String channelName) {
        try {
            PreparedStatement channelIDStmt = connection.prepareStatement("SELECT * FROM channel WHERE name = '" + channelName + "'", Statement.RETURN_GENERATED_KEYS);
            channelOccupancy.get(channel_id).remove(this.clientUserName);
            ResultSet channelIDRs = channelIDStmt.executeQuery();
                if (channelIDRs.next()) {
                    channel_id = channelIDRs.getInt(1);
                }
                else {
                    PreparedStatement createNewChannelStmt = connection.prepareStatement("INSERT INTO channel (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                    createNewChannelStmt.setString(1, channelName);
                    createNewChannelStmt.executeUpdate();
                    try (ResultSet newChannelRs = createNewChannelStmt.getGeneratedKeys()) {
                        if (newChannelRs.next()) {
                            channel_id = newChannelRs.getInt(1);
                        }
                    }
                }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        addToList(channel_id, this.clientUserName);
        out.println("[SERVER] Joined channel: " + channelName.substring(0, 1).toUpperCase() + channelName.substring(1));
        serverBroadCast(clientUserName + " joined the channel");
        getLastLogEntries(channel_id);
        System.out.println(this.clientUserName + " joined: " + channel_id);
    }

    private synchronized void addToList(int channelID, String userName) {
        if (channelOccupancy.containsKey(channelID)) {
            List<String> usernames = channelOccupancy.get(channelID);
            if (usernames == null) {
                usernames = new ArrayList<>();
                usernames.add(userName);
                channelOccupancy.put(channelID, usernames);
            }
            else {
                if (!usernames.contains(this.clientUserName)) usernames.add(userName);
            }
        }
        else {
            List<String> usernames = new ArrayList<>();
            usernames.add(userName);
            channelOccupancy.put(channelID, usernames);
        }
    }
}
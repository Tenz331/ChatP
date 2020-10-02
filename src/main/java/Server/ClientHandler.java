package Server;


import Controller.DBConnector;
import Util.Encryptor;
import api.ChatP;
import domain.User;

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
    public final ChatP chatP;
    private String clientUserName; //clients - players username
    private final BufferedReader in;
    private final PrintWriter out;
    LocalDateTime time;
    Integer user_id;
    boolean running = false;
    private int channel_id;
    private final Connection connection = DBConnector.getInstance().getConnection();

    public ClientHandler(ChatP chatP, Socket ClientSocket) throws IOException {
        this.chatP = chatP; //constructor
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
                    chatP.sendMessage(user_id, userInput, channel_id);

                    //Timestamp for chat window
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
                    time = LocalDateTime.now();
                    String timestamp = "[" + format.format(time) + "]";
                    userBroadCast(timestamp, userInput, channel_id);

                } else if (userInput.startsWith("!")) {
                    String[] userCommand = userInput.split(" ");
                    switch (userCommand[0].toLowerCase().substring(1)) {
                        case "users":
                            int channel_id = chatP.getActiveChannelOf(clientUserName);
                            out.println("Current users: " +
                                    chatP.getActiveUsers(channel_id).toString());
                            System.out.println(chatP.getActiveUsers(channel_id).toString());
                            break;
                        case "quit":
                            chatP.logoff(clientUserName);
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
                chatP.logoff(this.clientUserName);
                serverBroadCast(this.clientUserName + " has left");
                this.clientUserName = null;
                running = false;
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
        for (ClientHandler clientHandler : chatP.getClientHandlers()) {
            if (Objects.equals(clientHandler.channel_id, this.channel_id)) {
                if (!Objects.equals(clientHandler.clientUserName, this.clientUserName)) {
                    clientHandler.out.println("[SERVER] " + substring);
                }
            }
        }
    }

    private void userBroadCast(String timestamp, String substring, int channelID) { //player - client broadcast
        for (ClientHandler clientHandler : chatP.getClientHandlers()) {
            if (Objects.equals(clientHandler.channel_id, channelID)) {
                if (!Objects.equals(clientHandler.clientUserName, this.clientUserName)) {
                    clientHandler.out.println(timestamp + clientUserName + ": " + substring);
                } else {
                    clientHandler.out.println(timestamp + "You: " + substring);
                }
            }
        }
    }

    public boolean validatePassword(String password1, String password2) {
        return password1.equals(password2);
    }

    private void login() throws IOException, SQLException {
        out.println("What is your username?:");
        String username = in.readLine();
        if (chatP.checkIfUserIsLoggedIn(username)) {
            out.println("User all ready logged in!");
            out.println();
            login();
        } else {
            User user = chatP.findUser(username);
            if (user != null) {
                out.println("User already exists!");
                out.println("Enter password to continue");
                String password = Encryptor.SHA256(in.readLine());
                if (validatePassword(user.getPassword(), password)) {
                    chatP.login(user);
                } else {
                    out.println("Wrong password!");
                    out.println();
                    login();
                }
            } else {
                createNewUser(username);
            }
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
            chatP.login(new User(user_id, username, password1));
            this.clientUserName = username;
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
            ResultSet channelIDRs = channelIDStmt.executeQuery();
                if (channelIDRs.next()) {
                    channel_id = channelIDRs.getInt(1);
                    chatP.setActiveChannel(clientUserName, channel_id);
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
        chatP.setActiveChannel(this.clientUserName, channel_id);
        out.println("[SERVER] Joined channel: " + channelName.substring(0, 1).toUpperCase() + channelName.substring(1));
        serverBroadCast(clientUserName + " joined the channel");
        getLastLogEntries(channel_id);
        System.out.println(this.clientUserName + " joined: " + channel_id);
    }

}
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
import java.util.ArrayList;
import java.util.Objects;

public class ClientHandler implements Runnable {
    public static ArrayList<String> users; //temp player info
    private String clientUserName; //clients - players username
    private final BufferedReader in;
    private final PrintWriter out;
    private final ArrayList<ClientHandler> clients;
    LocalDateTime time;
    Integer user_id;
    boolean running = true;
    private static final Connection connection = DBConnector.getInstance().getConnection();

    public ClientHandler(Socket ClientSocket, ArrayList<ClientHandler> clients) throws IOException { //constructor
        users = new ArrayList<>();
        this.clients = clients;
        in = new BufferedReader((new InputStreamReader(ClientSocket.getInputStream())));
        out = new PrintWriter(ClientSocket.getOutputStream(), true);
    }

    @Override
    public void run() { //thread start for client
        try {
            login();
            while (running) {
                String userInput = in.readLine();
                if (!userInput.startsWith("!")) { //player broadcast msg
                    //Insert message into DB
                    String query = "INSERT INTO ChadChat.log(user_ID, msg) VALUES (?, ?)";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, user_id);
                    stmt.setString(2, userInput);
                    stmt.execute();

                    //Timestamp for chat window
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
                    time = LocalDateTime.now();
                    String timestamp = "[" + format.format(time) + "]";
                    playerBroadCast(timestamp, userInput);

                } else if (userInput.startsWith("!")) {
                    switch (userInput.substring(1).toLowerCase()) {
                        case "users":
                            out.println("Current users: " + getUsersInLobby().toString());
                            break;
                        case "quit":
                            removeUser(clientUserName);
                            run();
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

    private void serverBroadCast(String substring) { //server broadcast
        for (ClientHandler clientHandler : clients) {
            clientHandler.out.println("[SERVER] " + substring);
        }
    }

    private void playerBroadCast(String timestamp, String substring) { //player - client broadcast
        for (ClientHandler clientHandler : clients) {
            if (clientUserName != null) {
                if (!Objects.equals(clientHandler.clientUserName,this.clientUserName)) {
                    clientHandler.out.println(timestamp + clientUserName + ": " + substring);
                }
                else {
                    clientHandler.out.println(timestamp + "You: " + substring);
                }
            }
        }
    }

    public static ArrayList<String> getUsersInLobby() { //gets players on server;
        return users;
    }

    public boolean removeUser(String user) { //remove players
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
        String username = in.readLine(); //check if username is allowed / enabled < - >
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
                    }
                    else {
                        out.println("Wrong password!");
                        out.println();
                        login();
                    }
                }
            }
        }
        else {
            createNewUser(username);
        }
        serverBroadCast(clientUserName + " has joined");
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
        }
        else {
            out.println("Passwords did not match! Try again.");
            createNewUser(username);
        }
    }
}
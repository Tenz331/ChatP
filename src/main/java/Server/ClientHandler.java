package Server;


import Controller.DBConnector;
import Core.User;
import Util.Encryptor;
import com.mysql.cj.protocol.Resultset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Set<String> players; //temp player info
    private User clientUserName; //clients - players username
    private final BufferedReader in;
    private final PrintWriter out;
    private final ArrayList<ClientHandler> clients; //todo
    LocalDateTime time;
    Integer user_id;
    private static final Connection connection = DBConnector.getInstance().getConnection();

    public ClientHandler(Socket ClientSocket, ArrayList<ClientHandler> clients, Set<String> users) throws IOException { //constructor
        this.players = users;
        this.clients = clients;
        in = new BufferedReader((new InputStreamReader(ClientSocket.getInputStream())));
        out = new PrintWriter(ClientSocket.getOutputStream(), true);
    }

    @Override
    public void run() { //thread start for client
        try {
            out.println("What is your username?:");
            String username = in.readLine(); //check if username is allowed / enabled < - >
            PreparedStatement loginStmt = connection.prepareStatement("SELECT id, username FROM users WHERE username=?");
            loginStmt.setString(1, username);
            ResultSet rs = loginStmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("username").toLowerCase().equals(username.toLowerCase())) {
                    out.println("User already exists!");
                    out.println("Enter password to continue");
                    String password = Encryptor.SHA256(in.readLine());
                    PreparedStatement passwordStmt = connection.prepareStatement("SELECT password FROM users WHERE username=?");
                    passwordStmt.setString(1, username);
                    ResultSet passRs = passwordStmt.executeQuery();
                    if (passRs.next()) {
                        String DBPassword = passRs.getString(1);
                        System.out.println(DBPassword);
                        System.out.println(password);
                        if (password.equals(passRs.getString(1))) {
                            out.println("Login successful");
                            players.add(username);
                            clientUserName = new User(username);
                            user_id = rs.getInt(1);
                        }
                        else {
                            out.println("Wrong password!");
                            run();
                        }
                    }
                }
            }
            else {
                out.println("Creating new user!");
                out.println("Enter desired password");
                String password1 = Encryptor.SHA256(in.readLine());
                out.println("Enter password again");
                String password2 = Encryptor.SHA256(in.readLine());
                if (validatePassword(password1, password2)) {
                    PreparedStatement newUser = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                    newUser.setString(1, username);
                    newUser.setString(2, password1);
                    ResultSet newUserRs = newUser.executeQuery();
                    System.out.println(newUserRs);
                    out.println("Created new user: " + username);

                }
            }


            /*if (User.getUsers().contains(username)) {
                out.println(username + "Username is already taken");
                run();
            } else {
                serverBroadCast(username + " joined the server");

            }*/

            while (true) {
                String userInput = in.readLine();
                if (!userInput.startsWith("!")) { //player broadcast msg
                    //Insert message into DB
                    String query = "INSERT INTO ChadChat.log(user_ID, msg) VALUES (?, ?)";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, 1);
                    stmt.setString(2, userInput);
                    stmt.execute();

                    //Timestamp for chatwindow
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
                    time = LocalDateTime.now();
                    String timestamp = "[" + format.format(time) + "]";
                    playerBroadCast(timestamp, userInput);

                } else if (userInput.startsWith("!")) {
                    switch (userInput.substring(1).toLowerCase()) {
                        case "users":
                            out.println("Current users: " + getPlayersInLobby().toString());
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
                if (User.removeUser(clientUserName.toString())) {
                    removePlayer(clientUserName.toString());
                    serverBroadCast(clientUserName + " has left");
                    getPlayersInLobby();
                    this.clientUserName = null;
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

    private void playerBroadCast(String timestamp, String substring) { //player - client boradcast
        for (ClientHandler clientHandler : clients) {
            if (clientUserName != null) {
                if (clientHandler.clientUserName != this.clientUserName) {
                    clientHandler.out.println(timestamp + clientUserName + ": " + substring);
                }
                else {
                    clientHandler.out.println(timestamp + "You: " + substring);
                }
            }
        }
    }

    public Set<String> getPlayersInLobby() { //gets players on server
        players = User.getUsers();
        return players;
    }

    public void removePlayer(String player) { //remove players
        players.remove(player);
        User.removeUser(player);
    }

    public boolean validatePassword(String password1, String password2) {
        return password1.equals(password2);
    }
}
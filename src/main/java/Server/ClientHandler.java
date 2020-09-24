package Server;


import Core.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;

public class ClientHandler implements Runnable {
    private User clientUserName; //clients - players username
    private final BufferedReader in;
    private final PrintWriter out;
    private final ArrayList<ClientHandler> clients; //todo
    LocalDateTime time;


    public ClientHandler(Socket ClientSocket, ArrayList<ClientHandler> clients) throws IOException { //constructor
        this.clients = clients;
        in = new BufferedReader((new InputStreamReader(ClientSocket.getInputStream())));
        out = new PrintWriter(ClientSocket.getOutputStream(), true);
    }

    @Override
    public void run() { //thread start for client
        try {
            out.println("What is your username?:");
            String username = in.readLine(); //check if username is allowed / enabled < - >
            if (User.getUsers().contains(username)) {
                out.println(username + "Username is already taken");
                run();
            } else {
                serverBroadCast(username + " joined the server");
                User.addUser(username);
                clientUserName = new User(username);
            }

            while (true) {
                String userInput = in.readLine();
                if (!userInput.startsWith("!")) { //player broadcast msg
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
                    time = LocalDateTime.now();
                    String timestamp = "[" + format.format(time) + "]";
                    playerBroadCast(timestamp, userInput);

                } else if (userInput.startsWith("!")) {
                    switch (userInput.substring(1).toLowerCase()) {
                        case "users":
                            out.println("Current users: " + User.getUsers());
                            break;
                        default:
                            out.println("Invalid command");
                            break;
                    }
                }
                //asdadasd
            }
        } catch (IOException e) {
            e.getMessage();
        } finally {
            try {
                removePlayer(clientUserName.toString());
                serverBroadCast(clientUserName + " has left");
                User.getUsers();
                this.clientUserName = null;
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


    public void removePlayer(String player) { //remove players
        User.removeUser(player);
    }
}
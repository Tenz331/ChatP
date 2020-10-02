package Server;


import api.ChatP;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerHandler {
    private static final int PORT = 2222;

    public static void main(String[] args) throws IOException{
        ChatP chatP = new ChatP();
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println("[SERVER] listening on port: " + PORT); // starting server
        while (true) {
            Socket client = listener.accept(); //accept client - todo if max treads started add to que
            ClientHandler clientThread = new ClientHandler(chatP, client);
            chatP.addClient(clientThread); //threading add client to thread
            Thread t = new Thread(clientThread);
            t.start();
        }
    }
}

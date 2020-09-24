package Server;

import Core.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerHandler implements Runnable {
    private final String serverHandle = ">";
    private final Socket server;
    private final BufferedReader in;
    private static final int PORT = 2222;
    private static ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); //client threads


    public ServerHandler(Socket socket) throws IOException {
        server = socket;
        in = new BufferedReader(new InputStreamReader(server.getInputStream()));
    }

    @Override
    public void run() { //thread
        String serverResponds = null;
        try {
            while (true) {
                serverResponds = in.readLine();
                if (serverResponds == null) break;
                System.out.println(serverHandle + " " + serverResponds); //message bounce back
            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println("[SERVER] listeing on port: " + PORT); // starting server
        while (true) {
            Socket client = listener.accept(); //accept client - todo if max treads started add to que
            InetAddress info;
            info = client.getInetAddress(); //delete for later just to get some useless ifno

            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println("Welcome to Team Blue´s SERVER");
            out.println(info.getCanonicalHostName() + " // " + info.getHostName() + " // " + info.getHostAddress() + " // " + PORT + "]"); //useless info delte
            ClientHandler clientThread = new ClientHandler(client, clientHandlers, User.getUsers());
            clientHandlers.add(clientThread); //threading add client to thread
            Thread t = new Thread(clientThread);
            t.start();
        }
    }
}

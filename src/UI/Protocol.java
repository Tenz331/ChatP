package UI;


import Core.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Protocol {
    private static final int counter = 0;

    private final BufferedReader in;
    private final PrintWriter out;
    private final Player player;

    public Protocol(BufferedReader in, PrintWriter out, Player player) {
        this.in = in;
        this.out = out;
        this.player = player;
    }

    private String fetchCommand() throws IOException {
        out.print("> ");
        String word = in.readLine(); // answer a100 -> answer
        return word;
    }

    public void run() throws IOException {
        try {
            out.println("Welcome to ");
            out.flush();
            String cmd = fetchCommand();
            while (!cmd.equals("quit")) {
                switch (cmd) {
                    case "h":
                    case "help":
                        out.println("aITS FUCKING JEOPARDY, IF YOU DONT KNOW HOW TO PLAY GOOGLE IT!\nbut 4real, 'd' to draw the board, 'a' to answer. answers are entered as: Catagory Letter, Question, like so: 'B400'");
                        break;
                    default:
                        out.println("Unknown command! '" + cmd + "' type help too see all commands");
                }
                out.flush();
                cmd = fetchCommand();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    }


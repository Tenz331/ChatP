package Core;

import java.util.HashSet;
import java.util.Set;

public class User {
    public final String id;
    private static Set<String> users = new HashSet<>(); //players / clients on server

    public User(String id) {
        this.id = id;
    }

    public static boolean addUser(String user) {
        if (users.contains(user))
        {
            System.out.println("[SERVER] User already in party");
            return true;
        }
        else {
            users.add(user);
            System.out.println("[SERVER] User ADDED to party");
            return false;
        }
    }
    public static boolean removeUser(String user){
        if (users.contains(user)){
            users.remove(user);
            System.out.println("  [SERVER] removed: "+ user);
            return true;
        } else {
            return false;
        }
    }
    public static Set<String> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        return this.id;
    }
}

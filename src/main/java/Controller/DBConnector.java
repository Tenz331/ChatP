package Controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnector {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CONN_STRING = "jdbc:mysql://165.232.69.243:3306/ChadChat";
    private static final int PORT = 22;

    //private static final String CONN_STRING = "jdbc:mysql://localhost:3306/ChadChat";
    private static final String timezone = "?serverTimezone=UTC";
    private Statement Statement = null;
    private Connection connection;
    private static DBConnector instance;

    public DBConnector() {
        try {
            connection = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static DBConnector getInstance() {
        if (instance == null) {
            instance = new DBConnector();
        }
        return instance;
    }
    public Connection getConnection() {
        return this.connection;
    }
}
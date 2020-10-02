package Controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBConnector {
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Passw0rd";
    //private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String CONN_STRING = "jdbc:mysql://localhost:3306/ChadChat";
    private static final String timezone = "?serverTimezone=UTC";
    private Connection connection;
    private static DBConnector instance;

    public DBConnector() {
        try {
            connection = DriverManager.getConnection(CONN_STRING + timezone, USERNAME, PASSWORD);
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

    public void addMessage(int user_id, String userInput, int channel_id) {
        try (var conn = getConnection()) {
            String insertMsgQuery = "INSERT INTO ChadChat.log(user_ID, msg, channel_id) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(insertMsgQuery);
            stmt.setInt(1, user_id);
            stmt.setString(2, userInput);
            stmt.setInt(3, channel_id);
            stmt.execute();
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

}
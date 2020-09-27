package Controller;

import java.sql.Connection;
import java.sql.DriverManager;

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
}
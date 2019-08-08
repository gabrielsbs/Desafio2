package ag.strider.server;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerApplication {
	public static Connection connection;

	public static void main(String[] args) {
		createConnection();
		createTable();
		SpringApplication.run(ServerApplication.class, args);
	}

	private static void createTable() {
		String query = "CREATE TABLE IF NOT EXISTS task(" + "id INT AUTO_INCREMENT, "
				+ "name VARCHAR(50) NOT NULL, " + "phase VARCHAR(50) NOT NULL, "
				+ "image_location VARCHAR(100) NOT NULL, " + " latitude decimal(11,6)," + " longitude decimal(11,6)," 
				+ " PRIMARY KEY (id)" +  ");";
		try {
			connection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/taskdb?user=root&password=root&useSSL=true&useTimezone=true&serverTimezone=America/Sao_Paulo");
			Statement st = connection.createStatement();
			st.execute(query);
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void createConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/?user=root&password=root&useTimezone=true&serverTimezone=UTC");
			Statement st = connection.createStatement();
			st.executeUpdate("CREATE DATABASE IF NOT EXISTS taskdb");
			st.close();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

}

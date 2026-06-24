import java.sql.*;
public class TestDB {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/./data/innovationlab", "sa", "");
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM simulations");
        while(rs.next()) {
            System.out.println(rs.getString("id") + " " + rs.getString("sim_status"));
        }
    }
}

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbChecker {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:h2:file:./data/innovationlab;AUTO_SERVER=TRUE", "sa", "");
            Statement stmt = conn.createStatement();
            
            String[] tables = {"USERS", "RULES", "PHYSIOLOGICAL_SYSTEMS", "SIMULATIONS", "CLINICAL_SCENARIOS", "PHYSIOLOGICAL_READING", "ALERT"};
            
            for (String table : tables) {
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + table);
                if (rs.next()) {
                    System.out.println("Total in " + table + ": " + rs.getInt(1));
                }
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

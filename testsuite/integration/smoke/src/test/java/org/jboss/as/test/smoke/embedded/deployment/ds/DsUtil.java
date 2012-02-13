package org.jboss.as.test.smoke.embedded.deployment.ds;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static junit.framework.Assert.*;

public class DsUtil {
    public static void testConnection(Connection con, String query,    String resultContains) {
        String result = null;
        Statement st = null;
        ResultSet rs = null;

        try {
            st = con.createStatement();
            rs = st.executeQuery(query);
            if (rs != null && rs.next())
                result = rs.getString(1);
        } 
        catch (Throwable t) {
            fail("it's impossible to execute query:" + query);
            t.printStackTrace();
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                    // Ignore
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignore) {
                    // Ignore
                }
            }

        }

        assertTrue("result='" + result + "', but must contain '"
                + resultContains + "' substring",
                result.indexOf(resultContains) >= 0);

    }
}

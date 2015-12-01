package org.jboss.as.test.manualmode.jca.connectionlistener;



import java.sql.Connection;
import java.sql.SQLException;

import org.jboss.jca.adapters.jdbc.spi.listener.ConnectionListener;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:hsvabek@redhat.com">Hynek Svabek</a>
 */
public class TestConnectionListener implements ConnectionListener
{
   private static final Logger log = Logger.getLogger(TestConnectionListener.class);
	
   public TestConnectionListener(){
   }

   public void initialize(ClassLoader cl) throws SQLException {
   }

   public void activated(Connection c) throws SQLException {
	  c.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS test_table("
			  +"description character varying(255),"
			  +"type bigint"
			+")");
	  c.createStatement().executeUpdate("INSERT INTO test_table(description, type) VALUES ('activated', '1')");
	  log.info("Activated record has been created, " + c);
   }

   public void passivated(Connection c) throws SQLException {
	   c.createStatement().executeUpdate("INSERT INTO test_table(description, type) VALUES ('passivated', '0')");
	   log.info("Passivated record has been created, " + c);
   }
}


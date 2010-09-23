package org.jboss.as.messaging.hornetq;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.concurrent.Executor;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class HornetQService implements Service<HornetQServer> {
   private Configuration configuration;

   HornetQServer server;

   /**
    * {@inheritDoc}
    */
   public synchronized void start(final StartContext context) throws StartException {
      try {
         server = new HornetQServerImpl(configuration);
      } catch (Exception e) {
         throw new StartException("Failed to start service", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void stop(final StopContext context) {
      try {
         server.stop();
      }
      catch (Exception e) {
         throw new RuntimeException("Failed to shutdown HornetQ server", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public synchronized HornetQServer getValue() throws IllegalStateException {
      if (server == null) throw new IllegalStateException();
      return server;
   }

   public Configuration getConfiguration() {
      return configuration;
   }

   public void setConfiguration(Configuration hqConfig) {
      this.configuration = hqConfig;
   }
}

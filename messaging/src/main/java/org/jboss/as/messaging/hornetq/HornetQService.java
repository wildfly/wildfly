package org.jboss.as.messaging.hornetq;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.jboss.as.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class HornetQService implements Service<HornetQServer> {
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   /** */
   private static final String HOST = "host";
   /** */
   private static final String PORT = "port";

   private Configuration configuration;

   private HornetQServer server;
   private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();

   /**
    * An injector that manages a name/SocketBinding pair mapping in #socketBindings
    */
   class SocketBindingInjector implements Injector<SocketBinding>, Value<SocketBinding> {
      private String name;
      private SocketBinding value;
      SocketBindingInjector(String name) {
         this.name = name;
      }
      @Override
      public void inject(SocketBinding value) throws InjectionException {
         this.value = value;
         socketBindings.put(name, value);
      }

      @Override
      public void uninject() {
         socketBindings.remove(name);
      }

      @Override
      public SocketBinding getValue() throws IllegalStateException {
         return value;
      }
   }

   public Injector<SocketBinding> getSocketBindingInjector(String name) {
      SocketBindingInjector injector = new SocketBindingInjector(name);
      return injector;
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void start(final StartContext context) throws StartException {
      try {
         // Update the acceptor/connector port/host values from the
         // Map the socket bindings onto the connectors/acceptors
         Collection<TransportConfiguration> acceptors = configuration.getAcceptorConfigurations();
         Collection<TransportConfiguration> connectors = configuration.getConnectorConfigurations().values();
         if (connectors != null) {
            for (TransportConfiguration tc : connectors) {
               SocketBinding binding = socketBindings.get(tc.getName());
               if (binding == null) {
                  throw new StartException("Failed to find SocketBinding for connector: " + tc.getName());
               }
               log.debug("Applying socket binding: "+binding);
               tc.getParams().put(HOST, binding.getSocketAddress().getHostName());
               tc.getParams().put(PORT, "" + binding.getSocketAddress().getPort());
            }
         }
         if (acceptors != null) {
            for (TransportConfiguration tc : acceptors) {
               SocketBinding binding = socketBindings.get(tc.getName());
               if (binding == null) {
                  throw new StartException("Failed to find SocketBinding for acceptor: " + tc.getName());
               }
               log.debug("Applying socket binding: "+binding);
               tc.getParams().put(HOST, binding.getSocketAddress().getHostName());
               tc.getParams().put(PORT, "" + binding.getSocketAddress().getPort());
            }
         }

         // Now start the server
         server = new HornetQServerImpl(configuration);
         log.info("Starting the HornetQServer...");
         server.start();
      } catch (Exception e) {
         throw new StartException("Failed to start service", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public synchronized void stop(final StopContext context) {
      try {
         if (server != null) {
            server.stop();
         }
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

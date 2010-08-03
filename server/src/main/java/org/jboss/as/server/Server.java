/**
 * 
 */
package org.jboss.as.server;

import org.jboss.as.model.Standalone;
import org.jboss.as.process.ProcessManagerSlave;


/**
 * An actual JBoss Application Server instance.
 * 
 * @author Brian Stansberry
 */
public class Server {

    private final ServerEnvironment environment;
    private final MessageHandler messageHandler;
    private ProcessManagerSlave processManagerSlave;
    private Standalone config;
    
    public Server(ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        this.messageHandler = new MessageHandler(this);
        launchProcessManagerSlave();
    }
    
    public void start(Standalone config) {
        this.config = config;
        // FIXME implement start
        throw new UnsupportedOperationException("implement me");
    }
    
    public void stop() {
        // FIXME implement start
    }

    private void launchProcessManagerSlave() {
        this.processManagerSlave = ProcessManagerSlaveFactory.getInstance().getProcessManagerSlave(environment, config, messageHandler);
        Thread t = new Thread(this.processManagerSlave.getController(), "Server Manager Process");
        t.setDaemon(true);
        t.start();
    }
}

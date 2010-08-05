/**
 * 
 */
package org.jboss.as.server.manager;

import org.jboss.as.model.Host;
import org.jboss.as.process.ProcessManagerSlave;

/**
 * A ProcessManagerSlaveFactory.
 * 
 * @author Brian Stansberry
 */
public final class ProcessManagerSlaveFactory {

    private static final ProcessManagerSlaveFactory INSTANCE = new ProcessManagerSlaveFactory();
    
    public static ProcessManagerSlaveFactory getInstance() {
        return INSTANCE;
    }
    
    public ProcessManagerSlave getProcessManagerSlave(ServerManagerEnvironment environment, Host host, MessageHandler handler) {
        
        // TODO JBAS-8259 -- possible socket-based communication
        // use bootstrapConfig to detect if PM wants that; use Host to
        // determine what socket to use
        
        // For now, keep it simple
        return new ProcessManagerSlave(environment.getStdin(), environment.getStdout(), handler);
    }
    
    private ProcessManagerSlaveFactory() {}
}

/**
 * 
 */
package org.jboss.as.server;

import org.jboss.as.model.Standalone;
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
    
    public ProcessManagerSlave getProcessManagerSlave(ServerEnvironment environment, Standalone config, MessageHandler handler) {
        
        // TODO JBAS-8259 -- possible socket-based communication
        // use environment to detect if PM wants that; use Standalone to
        // determine what socket to use
        
        // Problem: during primordial bootstrap we have no Standalone
        // Have ServerManager pass our address/port via command line?
        
        // For now, keep it simple
        return new ProcessManagerSlave(environment.getStdin(), environment.getStdout(), handler);
    }
    
    private ProcessManagerSlaveFactory() {}
}

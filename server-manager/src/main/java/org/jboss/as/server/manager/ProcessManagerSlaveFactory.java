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
        return new ProcessManagerSlave(environment.getProcessName(), environment.getProcessManagerAddress(), environment.getProcessManagerPort(), handler);
    }
    
    private ProcessManagerSlaveFactory() {}
}

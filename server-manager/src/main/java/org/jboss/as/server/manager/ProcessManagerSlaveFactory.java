/**
 *
 */
package org.jboss.as.server.manager;

import org.jboss.as.model.HostModel;
import org.jboss.as.process.ProcessManagerSlave;
import org.jboss.as.process.ProcessManagerSlave.Handler;

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

    public ProcessManagerSlave getProcessManagerSlave(ServerManagerEnvironment environment, HostModel host, Handler handler) {
        return new ProcessManagerSlave(environment.getProcessName(), environment.getProcessManagerAddress(), environment.getProcessManagerPort(), handler);
    }

    private ProcessManagerSlaveFactory() {}
}

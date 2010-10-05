/**
 *
 */
package org.jboss.as.server.manager;

import org.jboss.as.model.HostModel;
import org.jboss.as.process.ProcessManagerProtocol.OutgoingPmCommandHandler;

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

    public ProcessManagerSlave getProcessManagerSlave(ServerManagerEnvironment environment, HostModel host, OutgoingPmCommandHandler handler) {
        return new ProcessManagerSlave(environment.getProcessName(), environment.getProcessManagerAddress(), environment.getProcessManagerPort(), handler);
    }

    private ProcessManagerSlaveFactory() {}
}

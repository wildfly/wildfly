/**
 *
 */
package org.jboss.as.server.manager;

import java.io.IOException;
import java.util.List;

import org.jboss.as.process.ProcessManagerSlave;

/**
 * A {@link ServerCommunicationHandler} that routes messages to the server
 * via the {@link ProcessManagerSlave process manager}.
 *
 * @author Brian Stansberry
 */
public class ProcessManagerServerCommunicationHandler implements ServerCommunicationHandler {

    private final String serverName;
    private final ProcessManagerSlave processManagerSlave;

    public ProcessManagerServerCommunicationHandler(String serverName, ProcessManagerSlave processManagerSlave) {
        if (serverName == null) {
            throw new IllegalArgumentException("serverName is null");
        }
        this.serverName = serverName;

        if (processManagerSlave == null) {
            throw new IllegalArgumentException("processManagerSlave is null");
        }
        this.processManagerSlave = processManagerSlave;

    }

    /* (non-Javadoc)
     * @see org.jboss.as.server.manager.ServerCommunicationHandler#sendMessage(java.util.List)
     */
    @Override
    public void sendMessage(List<String> message) throws IOException {
        processManagerSlave.sendMessage(serverName, message);
    }

    @Override
    public void sendMessage(byte[] message) throws IOException {
        processManagerSlave.sendMessage(serverName, message);
    }

}

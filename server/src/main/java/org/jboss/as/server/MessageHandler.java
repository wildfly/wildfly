/**
 * 
 */
package org.jboss.as.server;

import java.util.List;

import org.jboss.as.process.ProcessManagerSlave.Handler;

/**
 * A MessageHandler.
 * 
 * @author Brian Stansberry
 */
class MessageHandler implements Handler {

    private final Server server;
    
    MessageHandler(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server is null");
        }
        this.server = server;
    }

    @Override
    public void handleMessage(String sourceProcessName, byte[] message) {
        // FIXME implement handleMessage
        throw new UnsupportedOperationException("implement me");
    }
    
    /* (non-Javadoc)
     * @see org.jboss.as.process.ProcessManagerSlave.Handler#handleMessage(java.lang.String, java.util.List)
     */
    @Override
    public void handleMessage(String sourceProcessName, List<String> message) {
        // FIXME implement handleMessage
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    public void shutdown() {
        server.stop();        
    }

}

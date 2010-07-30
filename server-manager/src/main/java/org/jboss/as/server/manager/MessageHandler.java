/**
 * 
 */
package org.jboss.as.server.manager;

import java.util.List;

import org.jboss.as.process.ProcessManagerSlave.Handler;

/**
 * A MessageHandler.
 * 
 * @author Brian Stansberry
 */
class MessageHandler implements Handler {

    private final ServerManager serverManager;
//    private final Map<String, Server> servers = new ConcurrentHashMap<String, Server>();
    
    
    MessageHandler(ServerManager serverManager) {
        if (serverManager == null) {
            throw new IllegalArgumentException("serverManager is null");
        }
        this.serverManager = serverManager;
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
        serverManager.stop();        
    }
    
//    public void registerServer(String serverName, Server server) {
//        if (serverName == null) {
//            throw new IllegalArgumentException("serverName is null");
//        }
//        if (server == null) {
//            throw new IllegalArgumentException("server is null");
//        }
//        servers.put(serverName, server);
//    }
//    
//    public void unregisterServer(String serverName) {
//        if (serverName == null) {
//            throw new IllegalArgumentException("serverName is null");
//        }
//        servers.remove(serverName);
//    }

}

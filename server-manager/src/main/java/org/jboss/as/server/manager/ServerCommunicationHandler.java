/**
 * 
 */
package org.jboss.as.server.manager;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction for objects that handle communication between a ServerManager
 * and a Server.
 * 
 * @author Brian Stansberry
 */
public interface ServerCommunicationHandler {

    void sendMessage(List<String> message) throws IOException;
    
    void sendMessage(byte[] msg, long checksum) throws IOException;
}

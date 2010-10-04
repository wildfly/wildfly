/**
 *
 */
package org.jboss.as.server.manager;

import java.io.IOException;

/**
 * Abstraction for objects that handle communication between a ServerManager
 * and a Server.
 *
 * @author Brian Stansberry
 */
public interface ServerCommunicationHandler {

    void sendMessage(byte[] msg) throws IOException;
}

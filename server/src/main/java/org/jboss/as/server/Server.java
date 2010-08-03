/**
 * 
 */
package org.jboss.as.server;


/**
 * An actual JBoss Application Server instance.
 * 
 * @author Brian Stansberry
 */
public class Server {

    private final ServerEnvironment environment;
    
    public Server(ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
    }
}

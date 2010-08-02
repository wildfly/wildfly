/**
 * 
 */
package org.jboss.as.server.manager;

import java.io.Serializable;

/**
 * A simple data type representing a command sent from a ServerManager to a 
 * managed server.
 * 
 * TODO Investigate using an enum instead of a String command.
 * TODO smarter marshalling of empty args/types if that proves common
 * 
 * @author Brian Stansberry
 */
public class ServerCommand implements Serializable {

    private static final long serialVersionUID = -6895384924351955222L;
    
    private static final Object[] NULL_ARGS = new Object[0];
    private static final Class<?>[] NULL_TYPES = new Class<?>[0];
    
    private final String command;
    private final Object[] args;
    private final Class<?>[] types;
    
    public ServerCommand(String command) {
        this(command, NULL_ARGS, NULL_TYPES);
    }
    
    public ServerCommand(String command, Object[] args, Class<?>[] types) {
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        this.command = command;
        if (args == null) {
            throw new IllegalArgumentException("args is null");
        }
        this.args = args;
        if (types == null) {
            throw new IllegalArgumentException("types is null");
        }
        if (args.length != types.length) {
            throw new IllegalStateException("Invalid number of types; expected " + 
                    args.length + " but got " + types.length);
        }
        this.types = types;        
    }

    public String getCommand() {
        return command;
    }

    public Object[] getArgs() {
        return args;
    }

    public Class<?>[] getTypes() {
        return types;
    }
}

/**
 * 
 */
package org.jboss.as.server.manager;

/**
 * A simple data type representing a command sent from a ServerManager to a 
 * managed server.
 * 
 * TODO Investigate using an enum instead of a String command.
 * TODO smarter marshalling of empty args/types if that proves common
 * 
 * @author Brian Stansberry
 */
public class ServerCommand extends ServerMessage {
    private static final long serialVersionUID = -2694481303807272572L;

    public enum Command {
        START, STOP, SHUTDOWN;
    }

    public ServerCommand(final Command command) {
        super(command.toString());
    }

    public ServerCommand(final Command command, final Object[] args, final Class<?>[] types) {
        super(command.toString(), args, types);
    }

    public Command getCommand() {
        return Command.valueOf(getMessage());
    }
}

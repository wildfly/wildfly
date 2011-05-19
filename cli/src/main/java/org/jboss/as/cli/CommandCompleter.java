/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli;


import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.operation.OperationRequestCompleter;

import jline.Completor;

/**
 * Tab-completer for commands starting with '/'.
 *
 * @author Alexey Loubyansky
 */
public class CommandCompleter implements Completor, CommandLineCompleter {

    private final CommandContext ctx;
    private final CommandRegistry cmdRegistry;

    public CommandCompleter(CommandRegistry cmdRegistry, CommandContext ctx) {
        if(cmdRegistry == null)
            throw new IllegalArgumentException("Command registry can't be null.");
        this.cmdRegistry = cmdRegistry;
        this.ctx = ctx;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int complete(String buffer, int cursor, List candidates) {
        return complete(ctx, buffer, cursor, candidates);
    }

    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

        int cmdFirstIndex = 0;
        while(cmdFirstIndex < buffer.length()) {
            if(!Character.isWhitespace(buffer.charAt(cmdFirstIndex))) {
                break;
            }
            ++cmdFirstIndex;
        }

        if(cmdFirstIndex == buffer.length()) {
            for(String cmd : cmdRegistry.getTabCompletionCommands()) {
                CommandHandler handler = cmdRegistry.getCommandHandler(cmd);
                if(handler.isAvailable(ctx)) {
                    candidates.add(cmd);
                }
            }
            return cmdFirstIndex;
        }

        char firstChar = buffer.charAt(cmdFirstIndex);
        if(firstChar == '.' || firstChar == ':' || firstChar == '/') {
            return OperationRequestCompleter.INSTANCE.complete(ctx, buffer, cursor, candidates);
        }

        int cmdLastIndex = cmdFirstIndex + 1;
        while(cmdLastIndex < buffer.length() && !Character.isWhitespace(buffer.charAt(cmdLastIndex))) {
            ++cmdLastIndex;
        }

        String cmd = buffer.substring(cmdFirstIndex, cmdLastIndex);
        if(cmdLastIndex < buffer.length()) {
            CommandHandler handler = cmdRegistry.getCommandHandler(cmd);
            if (handler != null) {
                CommandLineCompleter argsCompleter = handler.getArgumentCompleter();
                if (argsCompleter != null) {

                    int nextCharIndex = cmdLastIndex + 1;
                    while (nextCharIndex < buffer.length()) {
                        if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                            break;
                        }
                        ++nextCharIndex;
                    }

                    String cmdBuffer = buffer.substring(nextCharIndex);
                    int result = argsCompleter.complete(ctx, cmdBuffer, cursor - nextCharIndex, candidates);
                    if (result >= 0) {
                        return nextCharIndex + result;
                    } else {
                        return result;
                    }
                }
            }
        }

        if(cmdLastIndex < buffer.length()) {
            cmd = buffer.substring(cmdFirstIndex);
        }
        for(String command : cmdRegistry.getTabCompletionCommands()) {
            if (!command.startsWith(cmd)) {
                continue;
            }

            CommandHandler handler = cmdRegistry.getCommandHandler(command);
            if(handler.isAvailable(ctx)) {
                candidates.add(command);
            }
        }
        Collections.sort(candidates);
        return buffer.length() - cmd.length();
    }
}

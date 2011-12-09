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

import org.jboss.as.cli.impl.CommandCandidatesProvider;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;


/**
 * Tab-completer for commands starting with '/'.
 *
 * @author Alexey Loubyansky
 */
public class CommandCompleter implements CommandLineCompleter {

    private final CommandRegistry cmdRegistry;
    private final CommandCandidatesProvider cmdProvider;

    public CommandCompleter(CommandRegistry cmdRegistry) {
        if(cmdRegistry == null)
            throw new IllegalArgumentException("Command registry can't be null.");
        this.cmdRegistry = cmdRegistry;
        this.cmdProvider = new CommandCandidatesProvider(cmdRegistry);
    }

    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

/*        if(cursor != buffer.length()) {
            return -1;
        }
*/
        if(buffer.isEmpty()) {
            for(String cmd : cmdRegistry.getTabCompletionCommands()) {
                CommandHandler handler = cmdRegistry.getCommandHandler(cmd);
                if(handler.isAvailable(ctx)) {
                    candidates.add(cmd);
                }
            }
            Collections.sort(candidates);
            return 0;
        }

        final DefaultCallbackHandler parsedCmd = (DefaultCallbackHandler) ctx.getParsedCommandLine();
        try {
            parsedCmd.parse(ctx.getPrefix(), buffer, false);
        } catch(CommandFormatException e) {
            if(!parsedCmd.endsOnAddressOperationNameSeparator() || !parsedCmd.endsOnSeparator()) {
                return -1;
            }
        }

        OperationCandidatesProvider candidatesProvider = cmdProvider;
        if(!buffer.isEmpty()) {
            int cmdFirstIndex = 0;
            while(cmdFirstIndex < buffer.length()) {
                final char ch = buffer.charAt(cmdFirstIndex++);
                if(!Character.isWhitespace(ch)) {
                    if(ch == '.' || ch == ':' || ch == '/') {
                        candidatesProvider = ctx.getOperationCandidatesProvider();
                    }
                    break;
                }
            }
        }
        return OperationRequestCompleter.INSTANCE.complete(ctx, parsedCmd, candidatesProvider, buffer, cursor, candidates);
    }
}

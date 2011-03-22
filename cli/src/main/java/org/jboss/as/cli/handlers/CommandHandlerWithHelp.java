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
package org.jboss.as.cli.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.protocol.StreamUtils;

/**
 * Abstract handler that checks whether the argument is '--help', in which case it
 * tries to locate file [cmd].txt and print its content. If the argument
 * is absent or isn't '--help', it'll call handle(ctx, args) method.
 *
 * @author Alexey Loubyansky
 */
public abstract class CommandHandlerWithHelp implements CommandHandler {

    private final String filename;

    public CommandHandlerWithHelp(String command) {
        this.filename = "help/" + command + ".txt";
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandHandler#handle(org.jboss.as.cli.CommandContext)
     */
    @Override
    public void handle(CommandContext ctx) {

        String args = ctx.getCommandArguments();
        if(args != null) {
            args = args.trim();
            if(args.isEmpty()) {
                args = null;
            } else if(args.equals("--help")) {
                printHelp(ctx);
                return;
            }
        }

        handle(ctx, args);
    }

    protected void printHelp(CommandContext ctx) {
        InputStream helpInput = SecurityActions.getClassLoader(CommandHandlerWithHelp.class).getResourceAsStream(filename);
        if(helpInput != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(helpInput));
            try {
                String helpLine = reader.readLine();
                while(helpLine != null) {
                    ctx.printLine(helpLine);
                    helpLine = reader.readLine();
                }
            } catch(java.io.IOException e) {
                ctx.printLine("Failed to read help/help.txt: " + e.getLocalizedMessage());
            } finally {
                StreamUtils.safeClose(reader);
            }
        } else {
            ctx.printLine("Failed to locate command description " + filename);
        }
        return;
    }

    protected abstract void handle(CommandContext ctx, String args);
}

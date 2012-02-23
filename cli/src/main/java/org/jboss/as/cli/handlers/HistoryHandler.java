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

import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 *
 * @author Alexey Loubyansky
 */
public class HistoryHandler extends CommandHandlerWithHelp {

    private final ArgumentWithoutValue clear;
    private final ArgumentWithoutValue disable;
    private final ArgumentWithoutValue enable;

    public HistoryHandler() {
        this("history");
    }

    public HistoryHandler(String command) {
        super(command);

        clear = new ArgumentWithoutValue(this, "--clear");
        clear.setExclusive(true);

        disable = new ArgumentWithoutValue(this, "--disable");
        disable.setExclusive(true);

        enable = new ArgumentWithoutValue(this, "--enable");
        enable.setExclusive(true);
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {

        ParsedCommandLine args = ctx.getParsedCommandLine();
        if(!args.hasProperties()) {
            printHistory(ctx);
            return;
        }

        if(clear.isPresent(args)) {
            ctx.getHistory().clear();
        } else if(disable.isPresent(args)) {
            ctx.getHistory().setUseHistory(false);
        } else if(enable.isPresent(args)) {
            ctx.getHistory().setUseHistory(true);
        } else {
            throw new CommandFormatException("Unexpected argument '" + ctx.getArgumentsString() + '\'');
        }
    }

    private static void printHistory(CommandContext ctx) {

        CommandHistory history = ctx.getHistory();
        List<String> list = history.asList();
        for(String cmd : list) {
            ctx.printLine(cmd);
        }
        ctx.printLine("(The history is currently " + (history.isUseHistory() ? "enabled)" : "disabled)"));
    }
}

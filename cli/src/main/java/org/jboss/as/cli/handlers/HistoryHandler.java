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
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.impl.ArgumentWithoutValue;

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

        SimpleArgumentTabCompleter argsCompleter = (SimpleArgumentTabCompleter) this.getArgumentCompleter();

        clear = new ArgumentWithoutValue("--clear");
        clear.setExclusive(true);
        argsCompleter.addArgument(clear);

        disable = new ArgumentWithoutValue("--disable");
        disable.setExclusive(true);
        argsCompleter.addArgument(disable);

        enable = new ArgumentWithoutValue("--enable");
        enable.setExclusive(true);
        argsCompleter.addArgument(enable);
    }

    @Override
    protected void doHandle(CommandContext ctx) {

        ParsedArguments args = ctx.getParsedArguments();
        if(!args.hasArguments()) {
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
            ctx.printLine("Unexpected argument '" + ctx.getArgumentsString() + '\'');
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

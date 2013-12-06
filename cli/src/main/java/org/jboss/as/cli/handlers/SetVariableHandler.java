/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.ParsedCommandLine;


/**
 * @author Alexey Loubyansky
 *
 */
public class SetVariableHandler extends CommandHandlerWithHelp {

    public SetVariableHandler() {
        super("set");
        new ArgumentWithValue(this, OperationRequestCompleter.ARG_VALUE_COMPLETER, 0, "--variable");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        ParsedCommandLine parsedArgs = ctx.getParsedCommandLine();
        final List<String> vars = parsedArgs.getOtherProperties();
        if(vars.isEmpty()) {
            final Collection<String> defined = ctx.getVariables();
            if(defined.isEmpty()) {
                return;
            }
            final List<String> pairs = new ArrayList<String>(defined.size());
            for(String var : defined) {
                pairs.add(var + '=' + ctx.getVariable(var));
            }
            Collections.sort(pairs);
            for(String pair : pairs) {
                ctx.printLine(pair);
            }
            return;
        }
        for(String arg : vars) {
            if(arg.charAt(0) == '$') {
                arg = arg.substring(1);
                if(arg.isEmpty()) {
                    throw new CommandFormatException("Variable name is missing after '$'");
                }
            }
            final int equals = arg.indexOf('=');
            if(equals < 1) {
                throw new CommandFormatException("'=' is missing for variable '" + arg + "'");
            }
            final String name = arg.substring(0, equals);
            if(name.isEmpty()) {
                throw new CommandFormatException("The name is missing in '" + arg + "'");
            }
            if(equals == arg.length() - 1) {
                ctx.setVariable(name, null);
            } else {
                ctx.setVariable(name, arg.substring(equals + 1));
            }
        }
    }

    @Override
    protected void recognizeArguments(CommandContext ctx) throws CommandFormatException {

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final Set<String> propertyNames = args.getPropertyNames();
        if(!propertyNames.isEmpty()) {
            final Collection<String> names;
            if(helpArg.isPresent(args)) {
                if(propertyNames.size() == 1) {
                    return;
                }
                names = new ArrayList<String>(propertyNames);
                names.remove(helpArg.getFullName());
                names.remove(helpArg.getShortName());
            } else {
                names = propertyNames;
            }
            throw new CommandFormatException("Unrecognized argument names: " + names);
        }
    }
}

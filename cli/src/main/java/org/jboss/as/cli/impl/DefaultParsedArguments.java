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
package org.jboss.as.cli.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.parsing.CommandLineParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultParsedArguments implements ParsedArguments, CommandLineParser.CallbackHandler {

    /** current command's arguments */
    private String argsStr;
    /** named command arguments */
    private Map<String, String> namedArgs = new HashMap<String, String>();
    /** other command arguments */
    private List<String> otherArgs = new ArrayList<String>();

    private CommandHandler handler;

    private boolean parsed;

    public void reset(String args, CommandHandler handler) {
        argsStr = args;
        namedArgs.clear();
        otherArgs.clear();
        this.handler = handler;
        parsed = false;
    }

    public void parse(String args) throws CommandFormatException {
        reset(args, null);
        parseArgs();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgumentsString()
     */
    @Override
    public String getArgumentsString() {
        return argsStr;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#hasArguments()
     */
    @Override
    public boolean hasArguments() throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return !namedArgs.isEmpty() || !otherArgs.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#hasArgument(java.lang.String)
     */
    @Override
    public boolean hasArgument(String argName) throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return namedArgs.containsKey(argName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgument(java.lang.String)
     */
    @Override
    public String getArgument(String argName) throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return namedArgs.get(argName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgumentNames()
     */
    @Override
    public Set<String> getArgumentNames() throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return namedArgs.keySet();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getOtherArguments()
     */
    @Override
    public List<String> getOtherArguments() throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return otherArgs;
    }

    private void parseArgs() throws CommandFormatException {
        if (argsStr != null && !argsStr.isEmpty()) {
            CommandLineParser.parse(argsStr, this);
        }
        parsed = true;
    }

    @Override
    public void argument(String name, int nameStart, String value, int valueStart, int end) throws CommandFormatException {
        if(name != null) {
            if(name.endsWith("-")) {
                // this might be not the best way to check for an empty name, e.g. '--'.
                throw new CommandFormatException("Argument name is not complete: '" + name + "'");
            }
            if(handler != null && !handler.hasArgument(name)) {
                throw new CommandFormatException("Unexpected argument name '" + name + "'.");
            }
            namedArgs.put(name, value);
        } else if(value != null) {
            if(handler != null && !handler.hasArgument(otherArgs.size())) {
                throw new CommandFormatException("Unexpected argument '" + value + "'.");
            }
            otherArgs.add(value);
        }
    }
}

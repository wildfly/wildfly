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
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.handlers.CommandHandlerWithArguments;


/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentWithoutValue implements CommandArgument {

    protected final int index;
    protected final String fullName;
    protected final String shortName;

    protected List<CommandArgument> requiredPreceding;
    protected List<CommandArgument> cantAppearAfter = Collections.emptyList();
    protected boolean exclusive;

    public ArgumentWithoutValue(CommandHandlerWithArguments handler, String fullName) {
        this(handler, -1, fullName);
    }

    public ArgumentWithoutValue(CommandHandlerWithArguments handler, String fullName, String shortName) {
        if(fullName == null || fullName.length() < 1) {
            throw new IllegalArgumentException("Full name is null or an empty string.");
        }
        this.fullName = fullName;
        this.shortName = shortName;
        this.index = -1;

        if(handler == null) {
            throw new IllegalArgumentException("Command handler is null");
        }
        handler.addArgument(this);
    }

    public ArgumentWithoutValue(CommandHandlerWithArguments handler, int index, String fullName) {
        if(fullName == null || fullName.length() < 1) {
            throw new IllegalArgumentException("Full name is null or an empty string.");
        }
        this.fullName = fullName;
        this.shortName = null;
        this.index = index;

        if(handler == null) {
            throw new IllegalArgumentException("Command handler is null");
        }
        handler.addArgument(this);
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public void addRequiredPreceding(CommandArgument arg) {
        if(arg == null) {
            throw new IllegalArgumentException("The argument is null.");
        }
        if(requiredPreceding == null) {
            requiredPreceding = Collections.singletonList(arg);
            return;
        }
        if(requiredPreceding.size() == 1) {
            requiredPreceding = new ArrayList<CommandArgument>(requiredPreceding);
        }
        requiredPreceding.add(arg);
    }

    public void addCantAppearAfter(CommandArgument arg) {
        if(cantAppearAfter.isEmpty()) {
            cantAppearAfter = new ArrayList<CommandArgument>();
        }
        cantAppearAfter.add(arg);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public CommandLineCompleter getValueCompleter() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandArgument#getValue(org.jboss.as.cli.CommandContext)
     */
    @Override
    public String getValue(ParsedArguments args) {
        try {
            return getValue(args, false);
        } catch (CommandFormatException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandArgument#getValue(org.jboss.as.cli.CommandContext)
     */
    @Override
    public String getValue(ParsedArguments args, boolean required) throws CommandFormatException {
        if(!required) {
            return null;
        }
        if(isPresent(args)) {
            return null;
        }
        throw new CommandFormatException("Required argument '" + fullName + "' is missing value.");
    }

    @Override
    public boolean isPresent(ParsedArguments args) throws CommandFormatException {
        if(!args.hasArguments()) {
            return false;
        }

        if (index >= 0 && index < args.getOtherArguments().size()) {
            return true;
        }

        if(args.hasArgument(fullName)) {
            return true;
        }

        if(shortName != null && args.hasArgument(shortName)) {
            return true;
        }
        return false;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {

        ParsedArguments args = ctx.getParsedArguments();
        if (exclusive) {
            return !args.hasArguments();
        }

        if (isPresent(args)) {
            return false;
        }

        for (CommandArgument arg : cantAppearAfter) {
            if (arg.isPresent(args)) {
                return false;
            }
        }

        if (requiredPreceding != null) {
            for (CommandArgument arg : requiredPreceding) {
                if (arg.isPresent(args)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean isValueRequired() {
        return false;
    }

    @Override
    public String getShortName() {
        return shortName;
    }
}

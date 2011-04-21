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
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentWithoutValue implements CommandArgument {

    protected final int index;
    protected final String defaultName;
    protected final String[] names;

    protected CommandArgument requiredPreceding;
    protected List<CommandArgument> cantAppearAfter = Collections.emptyList();
    protected boolean exclusive;

    public ArgumentWithoutValue(String... names) {
        this(-1, names);
    }

    public ArgumentWithoutValue(int index, String... names) {
        if(names == null || names.length < 1) {
            throw new IllegalArgumentException("There must be at least one non-null default name.");
        }
        this.defaultName = names[0];
        if(defaultName == null) {
            throw new IllegalArgumentException("There must be at least one non-null default name.");
        }
        this.names = names;
        this.index = index;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = true;
    }

    public void addRequiredPreceding(CommandArgument arg) {
        if(arg == null) {
            throw new IllegalArgumentException("The argument is null.");
        }
        if(requiredPreceding != null) {
            throw new IllegalStateException("Currently supports only one required preceding arg.");
        }
        requiredPreceding = arg;
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
        return null;
    }

    @Override
    public boolean isPresent(ParsedArguments args) {
        if(!args.hasArguments()) {
            return false;
        }

        if (index >= 0 && index < args.getOtherArguments().size()) {
            return true;
        }

        if(names != null && names.length > 0) {
            if(names.length == 1) {
                return args.hasArgument(names[0]);
            }
            for(String name : names) {
                if(args.hasArgument(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getDefaultName() {
        return defaultName;
    }

    @Override
    public boolean canAppearNext(ParsedArguments args) {
        if(exclusive) {
            return !args.hasArguments();
        }

        if(isPresent(args)) {
            return false;
        }

        for(CommandArgument arg : cantAppearAfter) {
            if(arg.isPresent(args)) {
                return false;
            }
        }

        if(requiredPreceding != null) {
            return requiredPreceding.isPresent(args);
        }

        return true;
    }

    @Override
    public boolean isValueRequired() {
        return false;
    }
}

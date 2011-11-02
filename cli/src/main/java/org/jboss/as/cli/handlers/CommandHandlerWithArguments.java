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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;


/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CommandHandlerWithArguments implements CommandHandler {

    //private Set<String> argumentNames = Collections.emptySet();
    //private int maxArgumentIndex = -1;
    private List<CommandArgument> args = Collections.emptyList();

    public void addArgument(CommandArgument arg) {
/*        if(arg.getIndex() > -1) {
            maxArgumentIndex = arg.getIndex() > maxArgumentIndex ? arg.getIndex() : maxArgumentIndex;
        }
*/
        if(arg.getFullName() == null) {
            throw new IllegalArgumentException("Full name can't be null");
        }
/*        if(argumentNames.isEmpty()) {
            argumentNames = new HashSet<String>();
        }
        argumentNames.add(arg.getFullName());
        if(arg.getShortName() != null) {
            argumentNames.add(arg.getShortName());
        }
*/
        if(args.isEmpty()) {
            args = new ArrayList<CommandArgument>();
        }
        args.add(arg);
        //argCompleter.addArgument(arg);
    }

    @Override
    public boolean hasArgument(String name) {
        //return argumentNames.contains(name);
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public boolean hasArgument(int index) {
        //return index <= maxArgumentIndex;
        throw new UnsupportedOperationException("not used yet");
    }

    @Override
    public Collection<CommandArgument> getArguments(CommandContext ctx) {
        return this.args;
    }
}

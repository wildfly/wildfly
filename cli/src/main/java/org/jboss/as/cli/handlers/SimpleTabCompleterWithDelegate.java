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
import org.jboss.as.cli.CommandLineCompleter;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleTabCompleterWithDelegate extends SimpleTabCompleter {

    private final CommandLineCompleter delegate;

    public SimpleTabCompleterWithDelegate(String[] candidates, CommandLineCompleter delegate) {
        super(candidates);
        if(delegate == null) {
            throw new IllegalStateException("delegate can't be null");
        }
        this.delegate = delegate;
    }

    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
        int result = super.complete(ctx, buffer, cursor, candidates);
        if(/*candidates.isEmpty() && */delegate != null) {
            int delegateResult = delegate.complete(ctx, buffer, result, candidates);
            if(delegateResult < 0) {
                return result;
            } else {
                return delegateResult;
            }
        }
        return result;
    }
}

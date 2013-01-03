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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CommaSeparatedCompleter implements CommandLineCompleter {

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext, java.lang.String, int, java.util.List)
     */
    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

        final Collection<String> all = getAllCandidates(ctx);
        if (all.isEmpty()) {
            return -1;
        }

        candidates.addAll(all);
        if(buffer.isEmpty()) {
            return 0;
        }
        final String[] specified = buffer.split(",+");
        candidates.removeAll(Arrays.asList(specified));
        if(buffer.charAt(buffer.length() - 1) == ',') {
            return buffer.length();
        }
        final String chunk = specified[specified.length - 1];
        final Iterator<String> iterator = candidates.iterator();
        while(iterator.hasNext()) {
            if(!iterator.next().startsWith(chunk)) {
                iterator.remove();
            }
        }
        return buffer.length() - chunk.length();
    }

    protected abstract Collection<String> getAllCandidates(CommandContext ctx);
}

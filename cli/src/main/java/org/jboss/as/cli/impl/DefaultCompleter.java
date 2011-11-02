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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultCompleter implements CommandLineCompleter {

    public interface CandidatesProvider {
        Collection<String> getAllCandidates(CommandContext ctx);
    }

    private final CandidatesProvider candidatesProvider;

    public DefaultCompleter(CandidatesProvider candidatesProvider) {
        if(candidatesProvider == null) {
            throw new IllegalArgumentException("candidatesProvider can't be null.");
        }
        this.candidatesProvider = candidatesProvider;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext, java.lang.String, int, java.util.List)
     */
    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

        int nextCharIndex = 0;
        while (nextCharIndex < buffer.length()) {
            if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                break;
            }
            ++nextCharIndex;
        }

        final Collection<String> all = candidatesProvider.getAllCandidates(ctx);
        if (all.isEmpty()) {
            return -1;
        }

        String opBuffer = buffer.substring(nextCharIndex).trim();
        if (opBuffer.isEmpty()) {
            candidates.addAll(all);
        } else {
            for (String name : all) {
                if (name.startsWith(opBuffer)) {
                    candidates.add(name);
                }
            }
            Collections.sort(candidates);
        }
        return nextCharIndex;
    }
}

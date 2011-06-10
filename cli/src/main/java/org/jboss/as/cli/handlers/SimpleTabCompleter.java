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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleTabCompleter implements CommandLineCompleter {

    public static final SimpleTabCompleter BOOLEAN = new SimpleTabCompleter(new String[]{"false", "true"});

    private final List<String> all;

    public SimpleTabCompleter(String[] candidates) {
        if(candidates == null) {
            throw new IllegalArgumentException("Candidates can't be null");
        }
        all = Arrays.asList(candidates);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext, java.lang.String, int, java.util.List)
     */
    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

        int nextCharIndex = 0;
        while(nextCharIndex < buffer.length()) {
            if(!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                break;
            }
            ++nextCharIndex;
        }

        if(nextCharIndex == buffer.length()) {
            candidates.addAll(all);
            return nextCharIndex;
        }

        String[] split = buffer.split("\\s+");
        int result;
        final String chunk;
        if(Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
            chunk = null;
            result = buffer.length();
        } else {
            chunk = split[split.length - 1];
            result = buffer.length() - 1;
            while(result >= 0 && !Character.isWhitespace(buffer.charAt(result))) {
                --result;
            }
            ++result;
        }

        final List<String> remainingArgs = new ArrayList<String>(all);
        int maxI = chunk == null ? split.length : split.length - 1;
        for(int i = 0; i < maxI; ++i) {
            String arg = split[i];
            int equalsIndex = arg.indexOf('=');
            if(equalsIndex >= 0) {
                arg = arg.substring(0, equalsIndex + 1);
            }
            remainingArgs.remove(arg);
        }

        if (chunk == null) {
            candidates.addAll(remainingArgs);
        } else {
            for(String name : remainingArgs) {
                if(name.startsWith(chunk)) {
                    candidates.add(name);
                }
            }
            Collections.sort(candidates);
        }
        return result;
    }
}

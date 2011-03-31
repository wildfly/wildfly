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
package org.jboss.as.cli;


import java.util.List;
import java.util.Set;

import org.jboss.as.cli.operation.OperationRequestCompleter;

import jline.Completor;
import jline.FileNameCompletor;

/**
 * Tab-completer for commands starting with '/'.
 *
 * @author Alexey Loubyansky
 */
public class CommandCompleter implements Completor {

    private final FileNameCompletor fnCompleter = new FileNameCompletor();
    private final OperationRequestCompleter opCompleter;
    private final Set<String> commands;

    public CommandCompleter(Set<String> commands, OperationRequestCompleter opCompleter) {
        if(commands == null)
            throw new IllegalArgumentException("Set of commands can't be null.");
        this.commands = commands;
        this.opCompleter = opCompleter;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int complete(String buffer, int cursor, List candidates) {

        int firstCharIndex = 0;
        while(firstCharIndex < buffer.length()) {
            if(!Character.isWhitespace(buffer.charAt(firstCharIndex))) {
                break;
            }
            ++firstCharIndex;
        }

        if(firstCharIndex == buffer.length()) {
            candidates.addAll(commands);
            return firstCharIndex;
        }

        char firstChar = buffer.charAt(firstCharIndex);
        if(firstChar == '.' || firstChar == ':' || firstChar == '/') {
            return -1;
        }

        // TODO a hack to enable tab-completion for cd/cn
        if(buffer.startsWith("cd ", firstCharIndex) || buffer.startsWith("cn ", firstCharIndex)
                || buffer.startsWith("ls ")) {
            String opBuffer = buffer.substring(firstCharIndex + 3);
            int result = opCompleter.doComplete(opBuffer, candidates, false);
            if(result >= 0) {
                return firstCharIndex + 3 + result;
            } else {
                return result;
            }
        } else if(buffer.startsWith("deploy ", firstCharIndex)) {

            int nextCharIndex = firstCharIndex + 7;
            while(nextCharIndex < buffer.length()) {
                if(!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                    break;
                }
                ++nextCharIndex;
            }

            if(nextCharIndex < buffer.length() && buffer.charAt(nextCharIndex) == '-') {
                // that's a switch, skip it
                nextCharIndex = buffer.indexOf(' ', nextCharIndex);
                if(nextCharIndex < 0) {
                    return -1;
                }

                while(nextCharIndex < buffer.length()) {
                    if(!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                        break;
                    }
                    ++nextCharIndex;
                }
            }

            String opBuffer = buffer.substring(nextCharIndex);
            int result = fnCompleter.complete(opBuffer, cursor, candidates);
            if(result >= 0) {
                return nextCharIndex + result;
            } else {
                return result;
            }
        }

        String cmdChunk = buffer.substring(firstCharIndex);
        for (String command : commands) {
            if (command.startsWith(cmdChunk))
                candidates.add(command);
        }

        return buffer.length() - cmdChunk.length();
    }

}

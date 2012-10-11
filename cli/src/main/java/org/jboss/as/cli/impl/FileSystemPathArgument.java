/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.cli.handlers.CommandHandlerWithArguments;
import org.jboss.as.cli.handlers.FilenameTabCompleter;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
*
* @author Alexey Loubyansky
*/
public class FileSystemPathArgument extends ArgumentWithValue {

    private final FilenameTabCompleter completer;

    public FileSystemPathArgument(CommandHandlerWithArguments handler, FilenameTabCompleter completer, int index, String name) {
        super(handler, completer, index, name);
        this.completer = completer;
    }

    public FileSystemPathArgument(CommandHandlerWithArguments handler, FilenameTabCompleter completer, String name) {
        super(handler, completer, name);
        this.completer = completer;
    }

    @Override
    public String getValue(ParsedCommandLine args) {
        String value = super.getValue(args);
        if(value != null) {
            if(value.length() >= 0 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                value = value.substring(1, value.length() - 1);
            }
            if(completer != null) {
                value = completer.translatePath(value);
            }
        }
        return value;
    }

}

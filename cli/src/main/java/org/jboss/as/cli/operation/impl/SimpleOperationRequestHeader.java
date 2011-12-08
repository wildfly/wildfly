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
package org.jboss.as.cli.operation.impl;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationRequestHeader;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleOperationRequestHeader implements OperationRequestHeader {

    private final String name;
    private final String value;

    public SimpleOperationRequestHeader(String name, String value) throws CommandFormatException {
        if(name == null) {
            throw new CommandFormatException("Header name is null.");
        }
        if(value == null) {
            throw new CommandFormatException("Value for header '" + name + "' is null.");
        }
        this.name = name;
        this.value = value;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationRequestHeader#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationRequestHeader#toModelNode()
     */
    @Override
    public void addTo(CommandContext ctx, ModelNode headers) throws CommandFormatException {
        if(name == null) {
            throw new CommandFormatException("Header name is null.");
        }
        if(value == null) {
            throw new CommandFormatException("Value for header '" + name + "' is null.");
        }
        headers.get(name).set(value);
    }
}

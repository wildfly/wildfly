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
package org.jboss.as.cli.handlers.ifelse;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ModelNodePathOperand implements Operand {

    private final String[] path;

    public ModelNodePathOperand(String pathStr) throws CommandFormatException {
        if(pathStr == null) {
            throw new IllegalArgumentException("path is null.");
        }
        path = pathStr.split("\\.");
        if(path.length == 0) {
            throw new CommandFormatException("The path in the if condition is empty: '" + pathStr + "'");
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.ifelse.Operand#resolveValue(org.jboss.as.cli.CommandContext, org.jboss.dmr.ModelNode)
     */
    @Override
    public Object resolveValue(CommandContext ctx, ModelNode response) throws CommandLineException {
        ModelNode targetValue = response;
        for(String name : path) {
            if(!targetValue.hasDefined(name)) {
                break;
            } else {
                targetValue = targetValue.get(name);
            }
        }
        return targetValue == null ? null : targetValue;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(path[0]);
        for(int i = 1; i < path.length; ++i) {
            buf.append('.').append(path[i]);
        }
        return buf.toString();
    }
}

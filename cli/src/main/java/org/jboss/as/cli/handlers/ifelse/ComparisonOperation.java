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

import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;


/**
*
* @author Alexey Loubyansky
*/
abstract class ComparisonOperation extends BaseOperation {
    ComparisonOperation(String name) {
        super(name, 8);
    }

    @Override
    public Object resolveValue(CommandContext ctx, ModelNode response) throws CommandLineException {
        final List<Operand> operands = getOperands();
        if(operands.isEmpty()) {
            throw new CommandLineException(getName() + " has no operands.");
        }
        if(operands.size() != 2) {
            throw new CommandLineException(getName() + " expects 2 operands but got " + operands.size());
        }
        final Object left = operands.get(0).resolveValue(ctx, response);
        if(left == null) {
            return false;
        }
        final Object right = operands.get(1).resolveValue(ctx, response);
        if(right == null) {
            return false;
        }
        if(!(left instanceof ModelNode) || !(right instanceof ModelNode)) {
            throw new CommandLineException("Operands aren't instances of org.jboss.dmr.ModelNode: " +
                left.getClass().getName() + ", " + right.getClass().getName());
        }
        if(((ModelNode) left).getType() != ((ModelNode)right).getType()) {
            return false;
        }
        return compare(left, right);
    }

    protected abstract boolean compare(Object left, Object right) throws CommandLineException;
}
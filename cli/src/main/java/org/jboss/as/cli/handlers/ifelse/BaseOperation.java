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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.cli.CommandLineException;

/**
*
* @author Alexey Loubyansky
*/
abstract class BaseOperation implements Operation, Comparable<Operation> {
    private final String name;
    private final int priority;
    private final List<Operand> operands;

    BaseOperation(String name, int priority) {
        if(name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        this.name = name;
        this.priority = priority;
        operands = new ArrayList<Operand>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public List<Operand> getOperands() {
        return operands;
    }

    protected void addOperand(Operand operand) throws CommandLineException {
        if(operand == null) {
            throw new IllegalArgumentException("operand can't be null.");
        }
        operands.add(operand);
    }

    @Override
    public int compareTo(Operation o) {
        if(o == null) {
            throw new IllegalArgumentException("can't compare to null.");
        }
        return priority < o.getPriority() ? -1 : (priority > o.getPriority() ? 1 : 0);
    }

    @Override
    public String toString() {
        return '(' + name + ' ' + operands + ')';
    }
}
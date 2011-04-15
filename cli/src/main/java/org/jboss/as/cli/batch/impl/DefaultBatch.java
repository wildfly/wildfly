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
package org.jboss.as.cli.batch.impl;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchedCommand;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultBatch implements Batch {

    private final List<BatchedCommand> commands = new ArrayList<BatchedCommand>();

    /* (non-Javadoc)
     * @see org.jboss.as.cli.batch.Batch#getCommands()
     */
    @Override
    public List<BatchedCommand> getCommands() {
        return commands;
    }

    @Override
    public void add(BatchedCommand cmd) {
        if(cmd == null) {
            throw new IllegalArgumentException("Null argument.");
        }
        commands.add(cmd);
    }

    @Override
    public void clear() {
        commands.clear();
    }

    @Override
    public void remove(int lineNumber) {
        ensureRange(lineNumber);
        commands.remove(lineNumber);
    }

    @Override
    public void set(int index, BatchedCommand cmd) {
        ensureRange(index);
        commands.set(index, cmd);
    }

    protected void ensureRange(int lineNumber) {
        if(lineNumber < 0 || lineNumber > commands.size() - 1) {
            throw new IndexOutOfBoundsException(lineNumber + " isn't in range [0.." + (commands.size() - 1) + "]");
        }
    }

    @Override
    public int size() {
        return commands.size();
    }
}

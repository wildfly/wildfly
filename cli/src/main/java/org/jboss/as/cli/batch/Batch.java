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
package org.jboss.as.cli.batch;

import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public interface Batch {

    /**
     * Adds a command or an operation to the batch.
     * @param cmd  command or operation to add to the batch
     */
    void add(BatchedCommand cmd);

    /**
     * Returns all the commands and operations in the batch as a list.
     * @return  list of commands and operations in the batch
     */
    List<BatchedCommand> getCommands();

    /**
     * Removes all the commands and the operations from the batch.
     */
    void clear();

    /**
     * Removes command or operation corresponding to its index in the list.
     * The indexes start with 0.
     * @param index  the index of the command or operation to be removed from the batch
     */
    void remove(int index);

    /**
     * Move the command or operation corresponding to the currentIndex to the newIndex position,
     * shifting the commands/operations in between the indexes.
     * The indexes start with 0.
     * @param currentIndex  the index of the command or operation to move the new position
     * @param newIndex  the new position for the command/operation
     */
    void move(int currentIndex, int newIndex);

    /**
     * Replaces the command or operation at the specified index with the new one.
     * The indexes start with 0.
     * @param index  the position for the new command or operation.
     * @param cmd  the new command or operation
     */
    void set(int index, BatchedCommand cmd);

    /**
     * Returns the number of the commands and operations in the batch.
     * @return  the number of the commands and operations in the batch
     */
    int size();

    /**
     * Generates a composite operation request from all the commands and operations
     * in the batch.
     * @return  operation request that includes all the commands and operations in the batch
     */
    ModelNode toRequest();
}

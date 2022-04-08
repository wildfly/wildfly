/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.dispatcher;

import org.wildfly.clustering.group.Group;

/**
 * Factory for creating a command dispatcher.
 *
 * @author Paul Ferraro
 */
public interface CommandDispatcherFactory {

    /**
     * Returns the group upon which the this command dispatcher operates.
     *
     * @return a group
     */
    Group getGroup();

    /**
     * Creates a new command dispatcher using the specified identifier and context..
     * The resulting {@link CommandDispatcher} will communicate with those dispatchers within the group sharing the same identifier.
     *
     * @param id      a unique identifier for this dispatcher
     * @param context the context used for executing commands
     * @return a new command dispatcher
     */
    <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context);
}

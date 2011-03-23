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
package org.jboss.as.clustering.jgroups;

import java.util.Set;

/**
 * A registry of channel factories.
 * @author Paul Ferraro
 */
public interface ChannelFactoryRegistry {

    /**
     * Returns the default stack name.
     * @return the default stack name.
     */
    String getDefaultStack();

    /**
     * Returns the registered stack names.
     * @return
     */
    Set<String> getStacks();

    /**
     * Returns the channel factory for the specified stack name.
     * @param stack a registered stack name.
     * @return a JGroups channel factory
     * @throws NullPointerException if the specified stack name is null.
     * @throws IllegalArgumentException if the specified stack name is not registered.
     */
    ChannelFactory getChannelFactory(String stack);

    /**
     * Registers the specified channel factory using the specified stack name.
     * @param stack a stack name
     * @param factory a JGroups channel factory
     * @return true, if the factory was registered successfully, false if a factory is already registered with the specified name.
     */
    boolean addChannelFactory(String stack, ChannelFactory factory);

    /**
     * Unregisters the specified channel factory using the specified stack name.
     * @param stack a stack name
     * @return true, if the factory was unregistered successfully, false if no such factory is registered with the specified name.
     */
    boolean removeChannelFactory(String stack);
}

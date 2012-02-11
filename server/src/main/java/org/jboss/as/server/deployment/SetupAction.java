/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.deployment;

import java.util.Map;
import java.util.Set;

import org.jboss.msc.service.ServiceName;

/**
 * And action that sets up and tears down some form of context (e.g. the TCCL, JNDI context etc).
 * <p>
 * Implementations need to be thread safe, as multiple threads can be setting up and tearing down contexts at any given time
 *
 * @author Stuart Douglas
 *
 */
public interface SetupAction {

    /**
     * Sets up the context. If this method throws an exception then the {@link #teardown(java.util.Map)} method will not be called, so this
     * method should be implmeneted in an atomic manner.
     */
    void setup(Map<String, Object> properties);

    /**
     * Tears down the context that was set up and restores the previous context state.
     */
    void teardown(Map<String, Object> properties);

    /**
     * Higher priority setup actions run first
     */
    int priority();

    /**
     * Any dependencies that this action requires
     */
    Set<ServiceName> dependencies();

}

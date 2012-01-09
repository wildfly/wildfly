/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.configadmin.service;

import java.util.Dictionary;
import java.util.Set;

/**
 * A configuration listener for the {@link ConfigAdminService}.
 *
 * When a configuration listener is first registered with the {@link ConfigAdminService}
 * its configurationModified method is invoked for every PID the listener registers with.
 *
 * A <code>null</code> dictionary indicates that there is currently no configuration for the associated PID.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Dec-2010
 */
public interface ConfigAdminListener {

    /**
     * Called when the {@code ConfigAdminService} receives an update for
     * a PID that the listener has registered with.
     */
    void configurationModified(String pid, Dictionary<String, String> props);

    /**
     * Return the set of PIDs that this listener is interested in.
     * A <code>null</code> return value denotes any PID.
     */
    Set<String> getPIDs();
}

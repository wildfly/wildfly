/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.logging.Logger;

/**
 * Abstract base class for a session snapshot manager.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 */
public abstract class SnapshotManager {
    // The manager the snapshot manager should use
    private SessionManager manager;

    // The context-path
    private String contextPath;

    private Logger log;

    public SnapshotManager(SessionManager manager, String path) {
        this.manager = manager;
        contextPath = path;

        String suffix = path;
        if (suffix == null || "".equals(suffix))
            suffix = "ROOT";
        log = Logger.getLogger(getClass().getName() + "." + suffix);
    }

    /**
     * Tell the snapshot manager which session was modified and must be replicated
     */
    public abstract void snapshot(ClusteredSession<? extends OutgoingDistributableSessionData> session);

    /**
     * Start the snapshot manager
     */
    public abstract void start();

    /**
     * Stop the snapshot manager
     */
    public abstract void stop();

    protected String getContextPath() {
        return contextPath;
    }

    protected Logger getLog() {
        return log;
    }

    protected SessionManager getManager() {
        return manager;
    }

}

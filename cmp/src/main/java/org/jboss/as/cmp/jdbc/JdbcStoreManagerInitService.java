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

package org.jboss.as.cmp.jdbc;

import org.jboss.as.cmp.CmpMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author John Bailey
 */
public class JdbcStoreManagerInitService implements Service<JDBCStoreManager> {

    private final JDBCStoreManager storeManager;

    public JdbcStoreManagerInitService(final JDBCStoreManager storeManager) {
        this.storeManager = storeManager;
    }

    public synchronized void start(final StartContext context) throws StartException {
        try {
            storeManager.initStoreManager();
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.failedToStartJdbcStore(e);
        }
    }

    public synchronized void stop(final StopContext context) {
        storeManager.destroy();
    }

    public synchronized JDBCStoreManager getValue() throws IllegalStateException, IllegalArgumentException {
        return storeManager;
    }
}


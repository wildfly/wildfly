/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow.security.sso;

import org.wildfly.extension.undertow.Host;

/**
 * Factory for creating an in-memory SingleSignOnManager
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 * @author Paul Ferraro
 */
public class InMemorySingleSignOnManagerFactory implements SingleSignOnManagerFactory {

    @Override
    public SingleSignOnManager createSingleSignOnManager(Host host) {
        return new InMemorySingleSignOnManager();
    }

    static class InMemorySingleSignOnManager extends io.undertow.security.impl.InMemorySingleSignOnManager implements SingleSignOnManager {

        private volatile boolean started = false;

        @Override
        public boolean isStarted() {
            return this.started;
        }

        @Override
        public void start() {
            this.started = true;
        }

        @Override
        public void stop() {
            this.started = false;
        }
    }
}

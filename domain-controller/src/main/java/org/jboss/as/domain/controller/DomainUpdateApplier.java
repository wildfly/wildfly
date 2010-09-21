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

package org.jboss.as.domain.controller;

import java.util.Collection;
import java.util.Map;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DomainUpdateApplier<R, P> {

    /**
     * Handle the event of the update failing to apply to the domain.
     *
     * @param reason the reason for the failure
     */
    void handleDomainFailed(UpdateFailedException reason);

    /**
     * Handle the event of the update failing to apply to one or more server managers (hosts).
     *
     * @param hostFailureReasons a map of host name to failure cause
     */
    void handleHostFailed(Map<String, UpdateFailedException> hostFailureReasons);

    /**
     * Handle the event of the update successfully applying to the domain and all applicable server
     * managers (hosts).  The given context should be used to acquire the list of affected servers and
     * to apply the change to each of the servers according to the desired policy.  If the update should be
     * reverted, the {@link Context#cancel()} method should be invoked, which will cause the remaining changes to
     * not be applied.
     *
     * @param context
     * @param param
     */
    void handleReady(Context<R> context, P param);

    interface Context<R> {
        Collection<ServerIdentity> getAffectedServers();

        <P> void apply(ServerIdentity server, UpdateResultHandler<R, P> resultHandler, P param);

        void cancel();
    }

    class ServerIdentity {
        private final String hostName;
        private final String serverName;

        public ServerIdentity(final String hostName, final String serverName) {
            this.hostName = hostName;
            this.serverName = serverName;
        }

        public String getHostName() {
            return hostName;
        }

        public String getServerName() {
            return serverName;
        }
    }

}

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

package org.jboss.as.domain.client.api;

import java.util.Collection;
import java.util.Map;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * @author Brian Stansberry
 */
public interface DomainUpdateListener<R> extends UpdateResultHandler<R, ServerIdentity> {

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
     * Handle the event of the update successfully applying to the domain and to applicable server
     * managers (hosts).
     *
     * @param affectedServers the servers to which the update will be applied (resulting in
     *  subsequent invocations on the methods in the {@link UpdateResultHandler super-interface}
     */
    void handleServersIdentified(Collection<ServerIdentity> affectedServers);

}

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
package org.wildfly.clustering.web.sso;

import org.wildfly.clustering.web.Batcher;

/**
 * The SSO equivalent of a session manager.
 * @author Paul Ferraro
 */
public interface SSOManager<L> {
    /**
     * Creates a new single sign on entry.
     * @param ssoId a unique SSO identifier
     * @return a new SSO.
     */
    SSO<L> createSSO(String ssoId);

    /**
     * Returns the single sign on entry identified by the specified identifier.
     * @param ssoId a unique SSO identifier
     * @return an existing SSO, or null, if no SSO was found
     */
    SSO<L> findSSO(String ssoId);

    /**
     * A mechanism for starting/stopping a batch.
     * @return a batching mechanism.
     */
    Batcher getBatcher();
}

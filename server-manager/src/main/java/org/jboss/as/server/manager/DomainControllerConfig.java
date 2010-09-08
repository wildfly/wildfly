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

package org.jboss.as.server.manager;

import java.io.Serializable;

import org.jboss.as.model.HostModel;
import org.jboss.as.model.LocalDomainControllerElement;

/**
 * Config object passed to a local domain controller with the pre-parsed host and local domain controller element.
 *
 * @author John E. Bailey
 */
public class DomainControllerConfig implements Serializable {
    private static final long serialVersionUID = -6619852348761199970L;
    private LocalDomainControllerElement domainControllerElement;
    private HostModel host;

    /**
     * Default construct.
     */
    public DomainControllerConfig() {
    }

    /**
     * Construct with the local domain controller element and the host config.
     *
     * @param domainControllerElement The local domain controller element
     * @param host The host config
     */
    public DomainControllerConfig(final LocalDomainControllerElement domainControllerElement, final HostModel host) {
        this.domainControllerElement = domainControllerElement;
        this.host = host;
    }

    /**
     * Get the local domain controller element
     *
     * @return The local domain controller element
     */
    public LocalDomainControllerElement getDomainControllerElement() {
        return domainControllerElement;
    }

    /**
     * Get the host config
     *
     * @return The host config
     */
    public HostModel getHost() {
        return host;
    }
}

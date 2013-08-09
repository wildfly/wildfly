/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.audit;

/**
 * For use configuring the host name of the syslog audit log handler
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface EnvironmentNameReader {
    /**
     * Whether this is a server
     *
     *  @return {@code true} if a server, {@code false} if we are a host controller
     */
    boolean isServer();

    /**
     * Get the name of the server if it is a server as given in {@code ServerEnvironment.getServerName()}
     *
     * @return the name of the server
     */
    String getServerName();

    /**
     * Get the name of the host controller in the domain if it is a host controller or a domain mode server as given in @co{@code HostControllerEnvironment.getHostControllerName()}
     * or {@code ServerEnvironment.getHostControllerName()} respectively.
     *
     * @return the name of the server
     */
    String getHostName();

    /**
     * Get the name of the product to be used as the audit logger's app name.
     *
     * @return the product name, or {@code null} if this is not a product.
     */
    String getProductName();
}

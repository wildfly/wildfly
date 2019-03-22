/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.container;

import java.util.function.Supplier;

/**
 * Defines the container configuration for security domain-based single sign-on management.
 * @author Paul Ferraro
 */
public interface SecurityDomainSingleSignOnManagementConfiguration {
    /**
     * Returns the name of the associated security domain
     * @return a security domain name
     */
    String getSecurityDomainName();

    /**
     * Returns the identifier generator to be used for generating single sign-on identifiers.
     * @return an identifier generator
     */
    Supplier<String> getIdentifierGenerator();
}

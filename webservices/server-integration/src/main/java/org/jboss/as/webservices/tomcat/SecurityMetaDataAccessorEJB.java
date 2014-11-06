/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.tomcat;

import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates web app security meta data for EJB deployments.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
interface SecurityMetaDataAccessorEJB {
    /**
     * Obtains security domain from EJB deployment.
     *
     * @param dep webservice deployment
     * @return security domain associated with EJB deployment
     */
    String getSecurityDomain(Deployment dep);

    /**
     * Obtains security roles from EJB deployment.
     *
     * @param dep webservice deployment
     * @return security roles associated with EJB deployment
     */
    SecurityRolesMetaData getSecurityRoles(Deployment dep);

    /**
     * Whether WSDL access have to be secured.
     *
     * @param endpoint webservice EJB endpoint
     * @return authentication method or null if not specified
     */
    boolean isSecureWsdlAccess(Endpoint endpoint);

    /**
     * Gets EJB authentication method.
     *
     * @param endpoint webservice EJB endpoint
     * @return authentication method or null if not specified
     */
    String getAuthMethod(Endpoint endpoint);

    /**
     * Gets EJB transport guarantee.
     *
     * @param endpoint webservice EJB endpoint
     * @return transport guarantee or null if not specified
     */
    String getTransportGuarantee(Endpoint endpoint);
    /**
     * Gets realm name for protect resource
     *
     * @param endpoint webservice EJB endpoint
     * @return realm name or null if not specified
     */
    String getRealmName(Endpoint endpoint);
}

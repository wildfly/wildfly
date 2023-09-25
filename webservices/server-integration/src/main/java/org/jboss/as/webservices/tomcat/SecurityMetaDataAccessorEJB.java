/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.tomcat;

import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates web app security meta data for Jakarta Enterprise Beans deployments.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
interface SecurityMetaDataAccessorEJB {
    /**
     * Obtains security domain from Jakarta Enterprise Beans deployment.
     *
     * @param dep webservice deployment
     * @return security domain associated with Jakarta Enterprise Beans deployment
     */
    String getSecurityDomain(Deployment dep);

    /**
     * Obtains security roles from Jakarta Enterprise Beans deployment.
     *
     * @param dep webservice deployment
     * @return security roles associated with Jakarta Enterprise Beans deployment
     */
    SecurityRolesMetaData getSecurityRoles(Deployment dep);

    /**
     * Whether WSDL access have to be secured.
     *
     * @param endpoint webservice Jakarta Enterprise Beans endpoint
     * @return authentication method or null if not specified
     */
    boolean isSecureWsdlAccess(Endpoint endpoint);

    /**
     * Gets Jakarta Enterprise Beans authentication method.
     *
     * @param endpoint webservice Jakarta Enterprise Beans endpoint
     * @return authentication method or null if not specified
     */
    String getAuthMethod(Endpoint endpoint);

    /**
     * Gets Jakarta Enterprise Beans transport guarantee.
     *
     * @param endpoint webservice Jakarta Enterprise Beans endpoint
     * @return transport guarantee or null if not specified
     */
    String getTransportGuarantee(Endpoint endpoint);
    /**
     * Gets realm name for protect resource
     *
     * @param endpoint webservice Jakarta Enterprise Beans endpoint
     * @return realm name or null if not specified
     */
    String getRealmName(Endpoint endpoint);
}

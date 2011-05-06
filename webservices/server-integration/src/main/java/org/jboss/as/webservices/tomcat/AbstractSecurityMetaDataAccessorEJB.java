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

import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;

/**
 * Creates web app security meta data for EJB deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
abstract class AbstractSecurityMetaDataAccessorEJB implements SecurityMetaDataAccessorEJB {

    /**
     * Constructor.
     */
    protected AbstractSecurityMetaDataAccessorEJB() {
        super();
    }

    /**
     * Appends 'java:jboss/jaas/' prefix to security domain if it's not prefixed with it.
     *
     * @param securityDomain security domain to be prefixed
     * @return security domain prefixed with jaas JNDI prefix
     */
    protected final String appendJaasPrefix(final String securityDomain) {
        if (securityDomain != null) {
            SecurityUtil.unprefixSecurityDomain(securityDomain);
            return SecurityConstants.JAAS_CONTEXT_ROOT + securityDomain;
        }

        return securityDomain;
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.SecurityMetaDataAccessorEJB#getAuthMethod(Endpoint)
     *
     * @param endpoint EJB webservice endpoint
     * @return authentication method or null if not specified
     */
    public String getAuthMethod(final Endpoint endpoint) {
        final EJBSecurityMetaData ejbSecurityMD = this.getEjbSecurityMetaData(endpoint);
        final boolean hasEjbSecurityMD = ejbSecurityMD != null;

        return hasEjbSecurityMD ? ejbSecurityMD.getAuthMethod() : null;
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.SecurityMetaDataAccessorEJB#isSecureWsdlAccess(Endpoint)
     *
     * @param endpoint EJB webservice endpoint
     * @return whether WSDL access have to be secured
     */
    public boolean isSecureWsdlAccess(final Endpoint endpoint) {
        final EJBSecurityMetaData ejbSecurityMD = this.getEjbSecurityMetaData(endpoint);
        final boolean hasEjbSecurityMD = ejbSecurityMD != null;

        return hasEjbSecurityMD ? ejbSecurityMD.getSecureWSDLAccess() : false;
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.SecurityMetaDataAccessorEJB#getTransportGuarantee(Endpoint)
     *
     * @param endpoint EJB webservice endpoint
     * @return transport guarantee or null if not specified
     */
    public String getTransportGuarantee(final Endpoint endpoint) {
        final EJBSecurityMetaData ejbSecurityMD = this.getEjbSecurityMetaData(endpoint);
        final boolean hasEjbSecurityMD = ejbSecurityMD != null;

        return hasEjbSecurityMD ? ejbSecurityMD.getTransportGuarantee() : null;
    }

    /**
     * Gets EJB security meta data if associated with EJB endpoint.
     *
     * @param endpoint EJB webservice endpoint
     * @return EJB security meta data or null
     */
    private EJBSecurityMetaData getEjbSecurityMetaData(final Endpoint endpoint) {
        final String ejbName = endpoint.getShortName();
        final Deployment dep = endpoint.getService().getDeployment();
        final EJBArchiveMetaData ejbArchiveMD = WSHelper.getOptionalAttachment(dep, EJBArchiveMetaData.class);
        final EJBMetaData ejbMD = ejbArchiveMD != null ? ejbArchiveMD.getBeanByEjbName(ejbName) : null;

        return ejbMD != null ? ejbMD.getSecurityMetaData() : null;
    }
}

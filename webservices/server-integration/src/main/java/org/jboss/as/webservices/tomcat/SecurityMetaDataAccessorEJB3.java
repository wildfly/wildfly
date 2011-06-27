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

import java.util.Iterator;

import javax.annotation.security.RolesAllowed;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.ws.api.annotation.WebContext;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.as.webservices.metadata.WebServiceDeclaration;
import org.jboss.as.webservices.metadata.WebServiceDeployment;

/**
 * Creates web app security meta data for EJB 3 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class SecurityMetaDataAccessorEJB3 extends AbstractSecurityMetaDataAccessorEJB {

    private static final DotName ROLES_ALLOWED_DOT_NAME = DotName.createSimple(RolesAllowed.class.getName());
    private static final DotName SECURITY_DOMAIN_DOT_NAME = DotName.createSimple(SecurityDomain.class.getName());

    /**
     * Constructor.
     */
    SecurityMetaDataAccessorEJB3() {
        super();
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.AbstractSecurityMetaDataAccessorEJB#getSecurityDomain(Deployment)
     *
     * @param dep webservice deployment
     * @return security domain associated with EJB 3 deployment
     */
    public String getSecurityDomain(final Deployment dep) {
        final WebServiceDeployment wsDeployment = WSHelper.getRequiredAttachment(dep, WebServiceDeployment.class);
        String securityDomain = null;
        final Iterator<WebServiceDeclaration> ejbContainers = wsDeployment.getServiceEndpoints().iterator();

        while (ejbContainers.hasNext()) {
            final WebServiceDeclaration ejbContainer = ejbContainers.next();
            //final SecurityDomain nextSecurityDomain = ejbContainer.getAnnotation(SecurityDomain.class);
            final AnnotationInstance nextSecurityDomain = ejbContainer.getAnnotation(SECURITY_DOMAIN_DOT_NAME);

            securityDomain = getDomain(securityDomain, nextSecurityDomain);
        }

        // return super.appendJaasPrefix(securityDomain); TODO: properly removed?
        return securityDomain;
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.AbstractSecurityMetaDataAccessorEJB#getSecurityRoles(Deployment)
     *
     * @param dep webservice deployment
     * @return security roles associated with EJB 21 deployment
     */
    public SecurityRolesMetaData getSecurityRoles(final Deployment dep) {
        final WebServiceDeployment wsDeployment = WSHelper.getRequiredAttachment(dep, WebServiceDeployment.class);
        final SecurityRolesMetaData securityRolesMD = new SecurityRolesMetaData();
        final Iterator<WebServiceDeclaration> ejbContainers = wsDeployment.getServiceEndpoints().iterator();

        while (ejbContainers.hasNext()) {
            final WebServiceDeclaration ejbContainer = ejbContainers.next();
            //final RolesAllowed allowedRoles = ejbContainer.getAnnotation(RolesAllowed.class);
            final AnnotationInstance allowedRoles = ejbContainer.getAnnotation(ROLES_ALLOWED_DOT_NAME);
            final boolean hasAllowedRoles = allowedRoles != null;

            if (hasAllowedRoles) {
                for (final String roleName : allowedRoles.value().asStringArray()) {
                    final SecurityRoleMetaData securityRoleMD = new SecurityRoleMetaData();

                    securityRoleMD.setRoleName(roleName);
                    securityRolesMD.add(securityRoleMD);
                }
            }
        }

        return securityRolesMD;
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.SecurityMetaDataAccessorEJB#getAuthMethod(Endpoint)
     *
     * @param endpoint EJB webservice endpoint
     * @return authentication method or null if not specified
     */
    public String getAuthMethod(final Endpoint endpoint) {
        final WebContext webContext = this.getWebContextAnnotation(endpoint);
        final boolean hasAuthMethod = (webContext != null) && (webContext.authMethod().length() > 0);

        return hasAuthMethod ? webContext.authMethod() : super.getAuthMethod(endpoint);
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.SecurityMetaDataAccessorEJB#isSecureWsdlAccess(Endpoint)
     *
     * @param endpoint EJB webservice endpoint
     * @return whether WSDL access have to be secured
     */
    public boolean isSecureWsdlAccess(final Endpoint endpoint) {
        final WebContext webContext = this.getWebContextAnnotation(endpoint);
        final boolean hasSecureWsdlAccess = (webContext != null) && (webContext.secureWSDLAccess());

        return hasSecureWsdlAccess ? true : super.isSecureWsdlAccess(endpoint);
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.SecurityMetaDataAccessorEJB#getTransportGuarantee(Endpoint)
     *
     * @param endpoint EJB webservice endpoint
     * @return transport guarantee or null if not specified
     */
    public String getTransportGuarantee(final Endpoint endpoint) {
        final WebContext webContext = this.getWebContextAnnotation(endpoint);
        final boolean hasTransportGuarantee = (webContext != null) && (webContext.transportGuarantee().length() > 0);

        return hasTransportGuarantee ? webContext.transportGuarantee() : super.getTransportGuarantee(endpoint);
    }

    /**
     * Gets <b>WebContext</b> if associated with EJB endpoint.
     *
     * @param endpoint EJB webservice endpoint
     * @return web context associated with EJB or null
     */
    @SuppressWarnings({ "unchecked" })
    private WebContext getWebContextAnnotation(final Endpoint endpoint) {
        // TODO: rework to use Jandex
        return (WebContext) endpoint.getTargetBeanClass().getAnnotation(WebContext.class);
    }

    /**
     * Returns security domain value. This method checks domain is the same for every EJB 3 endpoint.
     *
     * @param oldSecurityDomain our security domain
     * @param nextSecurityDomain next security domain
     * @return security domain value
     * @throws IllegalStateException if domains have different values
     */
    private String getDomain(final String oldSecurityDomain, final AnnotationInstance nextSecurityDomain) {
        if (nextSecurityDomain == null) {
            return oldSecurityDomain;
        }

        if (oldSecurityDomain == null) {
            return nextSecurityDomain.value().asString();
        }

        ensureSameDomains(oldSecurityDomain, nextSecurityDomain.value().asString());

        return oldSecurityDomain;
    }

    /**
     * This method ensures both passed domains contain the same value.
     *
     * @param oldSecurityDomain our security domain
     * @param newSecurityDomain next security domain
     * @throws IllegalStateException if domains have different values
     */
    private void ensureSameDomains(final String oldSecurityDomain, final String newSecurityDomain) {
        final boolean domainsDiffer = !oldSecurityDomain.equals(newSecurityDomain);

        if (domainsDiffer) {
            final String errorMessage = "Multiple security domains not supported. ";
            final String firstDomain = "First domain: '" + oldSecurityDomain + "' ";
            final String secondDomain = "second domain: '" + newSecurityDomain + "'";

            throw new IllegalStateException(errorMessage + firstDomain + secondDomain);
        }
    }

}

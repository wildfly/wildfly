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

import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WebMetaDataHelper;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;
import org.jboss.wsf.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WebMetaDataCreator {

    /** Our Realm. */
    private static final String EJB_WEBSERVICE_REALM = "EJBWebServiceEndpointServlet Realm";

    /** EJB 21 security meta data builder. */
    private final SecurityMetaDataAccessorEJB ejb21SecurityAccessor = null; // TODO:
                                                                            // implement
                                                                            // =
                                                                            // new
                                                                            // SecurityMetaDataAccessorEJB21();

    /** EJB 3 security meta data builder. */
    private final SecurityMetaDataAccessorEJB ejb3SecurityAccessor = null; // TODO:
                                                                           // implement
                                                                           // =
                                                                           // new
                                                                           // SecurityMetaDataAccessorEJB3();

    /** Logger. */
    private final Logger log = Logger.getLogger(WebMetaDataCreator.class);

    /**
     * Constructor.
     */
    WebMetaDataCreator() {
        super();
    }

    /**
     * Creates web meta data for EJB deployments.
     *
     * @param dep
     *            webservice deployment
     */
    void create(final Deployment dep) {
        final DeploymentUnit unit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
        WarMetaData warMD = ASHelper.getOptionalAttachment(unit, WarMetaData.ATTACHMENT_KEY); // TODO:
                                                                                              // WarMetaData?
        JBossWebMetaData jbossWebMD = warMD != null ? warMD.getJbossWebMetaData() : null;
        if (warMD == null) {
            warMD = new WarMetaData();
            jbossWebMD = new JBossWebMetaData();
            // warMD.setJbossWebMetaData(jbossWebMD); // TODO: ooops!
            warMD.setMergedJBossWebMetaData(jbossWebMD);
            unit.putAttachment(WarMetaData.ATTACHMENT_KEY, warMD);
        }

        this.createWebAppDescriptor(dep, jbossWebMD);
        this.createJBossWebAppDescriptor(dep, jbossWebMD);

        dep.addAttachment(JBossWebMetaData.class, jbossWebMD);
    }

    /**
     * Creates web.xml descriptor meta data.
     *
     * @param dep
     *            webservice deployment
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createWebAppDescriptor(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        this.log.debug("Creating web descriptor");
        this.createServlets(dep, jbossWebMD);
        this.createServletMappings(dep, jbossWebMD);
        this.createSecurityConstraints(dep, jbossWebMD);
        this.createLoginConfig(dep, jbossWebMD);
        this.createSecurityRoles(dep, jbossWebMD);
    }

    /**
     * Creates jboss-web.xml descriptor meta data.
     *
     * <pre>
     * &lt;jboss-web&gt;
     *   &lt;security-domain&gt;java:/jaas/custom-security-domain&lt;/security-domain&gt;
     *   &lt;context-root&gt;/custom-context-root&lt;/context-root&gt;
     *   &lt;virtual-host&gt;host1&lt;/virtual-host&gt;
     *   ...
     *   &lt;virtual-host&gt;hostN&lt;/virtual-host&gt;
     * &lt;/jboss-web&gt;
     * </pre>
     *
     * @param dep
     *            webservice deployment
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createJBossWebAppDescriptor(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        this.log.debug("Creating jboss web descriptor");
        final SecurityMetaDataAccessorEJB ejbMDAccessor = this.getEjbSecurityMetaDataAccessor(dep);

        if (ejbMDAccessor != null) { // TODO: remove this if statement so blok
                                     // code is always executed
            // Set security domain
            final String securityDomain = ejbMDAccessor.getSecurityDomain(dep);
            final boolean hasSecurityDomain = securityDomain != null;

            if (hasSecurityDomain) {
                this.log.debug("Setting security domain: " + securityDomain);
                jbossWebMD.setSecurityDomain(securityDomain);
            }

            // Set virtual hosts
            final List<String> virtualHosts = dep.getService().getVirtualHosts();
            this.log.debug("Setting virtual hosts: " + virtualHosts);
            jbossWebMD.setVirtualHosts(virtualHosts);
        }
    }

    /**
     * Creates servlets part of web.xml descriptor.
     *
     * <pre>
     * &lt;servlet&gt;
     *   &lt;servlet-name&gt;EJBEndpointShortName&lt;/servlet-name&gt;
     *   &lt;servlet-class&gt;EJBEndpointTargetBeanName&lt;/servlet-class&gt;
     * &lt;/servlet&gt;
     * </pre>
     *
     * @param dep
     *            webservice deployment
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createServlets(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        this.log.debug("Creating servlets");
        final JBossServletsMetaData servlets = WebMetaDataHelper.getServlets(jbossWebMD);

        for (final Endpoint endpoint : dep.getService().getEndpoints()) {
            final String endpointName = endpoint.getShortName();
            final String endpointClassName = endpoint.getTargetBeanName();

            this.log.debug("Servlet name: " + endpointName + ", servlet class: " + endpointClassName);
            WebMetaDataHelper.newServlet(endpointName, endpointClassName, servlets);
        }
    }

    /**
     * Creates servlet-mapping part of web.xml descriptor.
     *
     * <pre>
     * &lt;servlet-mapping&gt;
     *   &lt;servlet-name&gt;EJBEndpointShortName&lt;/servlet-name&gt;
     *   &lt;url-pattern&gt;EJBEndpointURLPattern&lt;/url-pattern&gt;
     * &lt;/servlet-mapping&gt;
     * </pre>
     *
     * @param dep
     *            webservice deployment
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createServletMappings(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        this.log.debug("Creating servlet mappings");
        final List<ServletMappingMetaData> servletMappings = WebMetaDataHelper.getServletMappings(jbossWebMD);

        for (final Endpoint ep : dep.getService().getEndpoints()) {
            final String endpointName = ep.getShortName();
            final List<String> urlPatterns = WebMetaDataHelper.getUrlPatterns(ep.getURLPattern());

            this.log.debug("Servlet name: " + endpointName + ", URL patterns: " + urlPatterns);
            WebMetaDataHelper.newServletMapping(endpointName, urlPatterns, servletMappings);
        }
    }

    /**
     * Creates security constraints part of web.xml descriptor.
     *
     * <pre>
     * &lt;security-constraint&gt;
     *   &lt;web-resource-collection&gt;
     *     &lt;web-resource-name&gt;EJBEndpointShortName&lt;/web-resource-name&gt;
     *     &lt;url-pattern&gt;EJBEndpointURLPattern&lt;/url-pattern&gt;
     *     &lt;http-method&gt;GET&lt;/http-method&gt;
     *     &lt;http-method&gt;POST&lt;/http-method&gt;
     *   &lt;/web-resource-collection&gt;
     *   &lt;auth-constraint&gt;
     *     &lt;role-name&gt;*&lt;/role-name&gt;
     *   &lt;/auth-constraint&gt;
     *   &lt;user-data-constraint&gt;
     *     &lt;transport-guarantee&gt;EjbTransportGuarantee&lt;/transport-guarantee&gt;
     *   &lt;/user-data-constraint&gt;
     * &lt;/security-constraint&gt;
     * </pre>
     *
     * @param dep
     *            webservice deployemnt
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createSecurityConstraints(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        this.log.debug("Creating security constraints");
        final SecurityMetaDataAccessorEJB ejbMDAccessor = this.getEjbSecurityMetaDataAccessor(dep);

        if (ejbMDAccessor != null) { // TODO: remove this if statement so the
                                     // following block of code is always
                                     // executed
            for (final Endpoint ejbEndpoint : dep.getService().getEndpoints()) {
                final boolean secureWsdlAccess = ejbMDAccessor.isSecureWsdlAccess(ejbEndpoint);
                final String transportGuarantee = ejbMDAccessor.getTransportGuarantee(ejbEndpoint);
                final boolean hasTransportGuarantee = transportGuarantee != null;
                final String authMethod = ejbMDAccessor.getAuthMethod(ejbEndpoint);
                final boolean hasAuthMethod = authMethod != null;

                if (hasAuthMethod || hasTransportGuarantee) {
                    final List<SecurityConstraintMetaData> securityConstraints = WebMetaDataHelper
                            .getSecurityConstraints(jbossWebMD);

                    // security-constraint
                    final SecurityConstraintMetaData securityConstraint = WebMetaDataHelper
                            .newSecurityConstraint(securityConstraints);

                    // web-resource-collection
                    final WebResourceCollectionsMetaData webResourceCollections = WebMetaDataHelper
                            .getWebResourceCollections(securityConstraint);
                    final String endpointName = ejbEndpoint.getShortName();
                    final String urlPattern = ejbEndpoint.getURLPattern();
                    this.log.debug("Creating web resource collection for endpoint: " + endpointName + ", URL pattern: "
                            + urlPattern);
                    WebMetaDataHelper.newWebResourceCollection(endpointName, urlPattern, secureWsdlAccess,
                            webResourceCollections);

                    // auth-constraint
                    if (hasAuthMethod) {
                        this.log.debug("Creating auth constraint for endpoint: " + endpointName);
                        WebMetaDataHelper.newAuthConstraint(WebMetaDataHelper.getAllRoles(), securityConstraint);
                    }

                    // user-data-constraint
                    if (hasTransportGuarantee) {
                        this.log.debug("Creating new user data constraint for endpoint: " + endpointName
                                + ", transport guarantee: " + transportGuarantee);
                        WebMetaDataHelper.newUserDataConstraint(transportGuarantee, securityConstraint);
                    }
                }
            }
        }
    }

    /**
     * Creates login-config part of web.xml descriptor.
     *
     * <pre>
     * &lt;login-config&gt;
     *   &lt;auth-method&gt;EjbDeploymentAuthMethod&lt;/auth-method&gt;
     *   &lt;realm-name&gt;EJBWebServiceEndpointServlet Realm&lt;/realm-name&gt;
     * &lt;/login-config&gt;
     * </pre>
     *
     * @param dep
     *            webservice deployment
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createLoginConfig(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final String authMethod = this.getAuthMethod(dep);
        final boolean hasAuthMethod = authMethod != null;

        if (hasAuthMethod) {
            this.log.debug("Creating new login config: " + WebMetaDataCreator.EJB_WEBSERVICE_REALM + ", auth method: "
                    + authMethod);
            final LoginConfigMetaData loginConfig = WebMetaDataHelper.getLoginConfig(jbossWebMD);
            loginConfig.setRealmName(WebMetaDataCreator.EJB_WEBSERVICE_REALM);
            loginConfig.setAuthMethod(authMethod);
        }
    }

    /**
     * Creates security roles part of web.xml descriptor.
     *
     * <pre>
     * &lt;security-role&gt;
     *   &lt;role-name&gt;role1&lt;/role-name&gt;
     *   ...
     *   &lt;role-name&gt;roleN&lt;/role-name&gt;
     * &lt;/security-role&gt;
     * </pre>
     *
     * @param dep
     *            webservice deployment
     * @param jbossWebMD
     *            jboss web meta data
     */
    private void createSecurityRoles(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final String authMethod = this.getAuthMethod(dep);
        final boolean hasAuthMethod = authMethod != null;

        if (hasAuthMethod) {
            final SecurityMetaDataAccessorEJB ejbMDAccessor = this.getEjbSecurityMetaDataAccessor(dep);
            if (ejbMDAccessor != null) { // TODO: remove this if statement so
                                         // the following block of code is
                                         // always executed
                final SecurityRolesMetaData securityRolesMD = ejbMDAccessor.getSecurityRoles(dep);
                final boolean hasSecurityRolesMD = securityRolesMD != null;

                if (hasSecurityRolesMD) {
                    this.log.debug("Setting security roles: " + securityRolesMD);
                    jbossWebMD.setSecurityRoles(securityRolesMD);
                }
            }
        }
    }

    /**
     * Returns deployment authentication method.
     *
     * @param dep
     *            webservice deployment
     * @return deployment authentication method
     */
    private String getAuthMethod(final Deployment dep) {
        final SecurityMetaDataAccessorEJB ejbMDAccessor = this.getEjbSecurityMetaDataAccessor(dep);

        if (ejbMDAccessor != null) { // TODO: remove this if statement so the
                                     // following code is always executed
            for (final Endpoint ejbEndpoint : dep.getService().getEndpoints()) {
                final String beanAuthMethod = ejbMDAccessor.getAuthMethod(ejbEndpoint);
                final boolean hasBeanAuthMethod = beanAuthMethod != null;

                if (hasBeanAuthMethod) {
                    // First found auth-method defines war
                    // login-config/auth-method
                    return beanAuthMethod;
                }
            }
        }

        return null;
    }

    /**
     * Returns security builder associated with EJB deployment.
     *
     * @param dep
     *            webservice EJB deployment
     * @return security builder for EJB deployment
     */
    private SecurityMetaDataAccessorEJB getEjbSecurityMetaDataAccessor(final Deployment dep) {
        final boolean isJaxws = WSHelper.isJaxwsDeployment(dep);

        return isJaxws ? this.ejb3SecurityAccessor : this.ejb21SecurityAccessor;
    }
}

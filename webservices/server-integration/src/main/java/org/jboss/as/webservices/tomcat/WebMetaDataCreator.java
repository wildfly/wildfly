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

import java.util.Arrays;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WebMetaDataHelper;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.merge.javaee.spec.SecurityRolesMetaDataMerger;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.HttpEndpoint;

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WebMetaDataCreator {

    private static final String EJB_WEBSERVICE_REALM = "EJBWebServiceEndpointServlet Realm";

    private final SecurityMetaDataAccessorEJB ejb21SecurityAccessor = new SecurityMetaDataAccessorEJB21();

    private final SecurityMetaDataAccessorEJB ejb3SecurityAccessor = new SecurityMetaDataAccessorEJB3();

    WebMetaDataCreator() {
        super();
    }

    /**
     * Creates web meta data for EJB deployments.
     *
     * @param dep webservice deployment
     */
    void create(final Deployment dep) {
        final DeploymentUnit unit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
        WarMetaData warMD = ASHelper.getOptionalAttachment(unit, WarMetaData.ATTACHMENT_KEY);
        JBossWebMetaData jbossWebMD = warMD != null ? warMD.getMergedJBossWebMetaData() : null;

        if (warMD == null) {
            warMD = new WarMetaData();
        }
        if (jbossWebMD == null) {
            jbossWebMD = new JBossWebMetaData();
            warMD.setMergedJBossWebMetaData(jbossWebMD);
            unit.putAttachment(WarMetaData.ATTACHMENT_KEY, warMD);
        }

        createWebAppDescriptor(dep, jbossWebMD);
        createJBossWebAppDescriptor(dep, jbossWebMD);

        dep.addAttachment(JBossWebMetaData.class, jbossWebMD);
    }

    /**
     * Creates web.xml descriptor meta data.
     *
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createWebAppDescriptor(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        ROOT_LOGGER.creatingWebXmlDescriptor();
        createServlets(dep, jbossWebMD);
        createServletMappings(dep, jbossWebMD);
        createSecurityConstraints(dep, jbossWebMD);
        createLoginConfig(dep, jbossWebMD);
        createSecurityRoles(dep, jbossWebMD);
    }

    /**
     * Creates jboss-web.xml descriptor meta data.
     * <p/>
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
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createJBossWebAppDescriptor(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        ROOT_LOGGER.creatingJBossWebXmlDescriptor();
        final SecurityMetaDataAccessorEJB ejbMDAccessor = getEjbSecurityMetaDataAccessor(dep);

        // Set security domain
        final String securityDomain = ejbMDAccessor.getSecurityDomain(dep);
        final boolean hasSecurityDomain = securityDomain != null;

        if (hasSecurityDomain) {
            ROOT_LOGGER.settingSecurityDomain(securityDomain);
            jbossWebMD.setSecurityDomain(securityDomain);
        }

        // Set virtual host
        final String virtualHost = dep.getService().getVirtualHost();
        if (virtualHost != null) {
            ROOT_LOGGER.settingVirtualHost(virtualHost);
            jbossWebMD.setVirtualHosts(Arrays.asList(virtualHost));
        }
    }

    /**
     * Creates servlets part of web.xml descriptor.
     * <p/>
     * <pre>
     * &lt;servlet&gt;
     *   &lt;servlet-name&gt;EJBEndpointShortName&lt;/servlet-name&gt;
     *   &lt;servlet-class&gt;EJBEndpointTargetBeanName&lt;/servlet-class&gt;
     * &lt;/servlet&gt;
     * </pre>
     *
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createServlets(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        ROOT_LOGGER.creatingServlets();
        final JBossServletsMetaData servlets = WebMetaDataHelper.getServlets(jbossWebMD);

        for (final Endpoint endpoint : dep.getService().getEndpoints()) {
            final String endpointName = endpoint.getShortName();
            final String endpointClassName = endpoint.getTargetBeanName();

            ROOT_LOGGER.creatingServlet(endpointName, endpointClassName);
            WebMetaDataHelper.newServlet(endpointName, endpointClassName, servlets);
        }
    }

    /**
     * Creates servlet-mapping part of web.xml descriptor.
     * <p/>
     * <pre>
     * &lt;servlet-mapping&gt;
     *   &lt;servlet-name&gt;EJBEndpointShortName&lt;/servlet-name&gt;
     *   &lt;url-pattern&gt;EJBEndpointURLPattern&lt;/url-pattern&gt;
     * &lt;/servlet-mapping&gt;
     * </pre>
     *
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createServletMappings(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        ROOT_LOGGER.creatingServletMappings();
        final List<ServletMappingMetaData> servletMappings = WebMetaDataHelper.getServletMappings(jbossWebMD);

        for (final Endpoint ep : dep.getService().getEndpoints()) {
            if (ep instanceof HttpEndpoint) {
                final String endpointName = ep.getShortName();
                final List<String> urlPatterns = WebMetaDataHelper.getUrlPatterns(((HttpEndpoint) ep).getURLPattern());

                ROOT_LOGGER.creatingServletMapping(endpointName, urlPatterns);
                WebMetaDataHelper.newServletMapping(endpointName, urlPatterns, servletMappings);
            }
        }
    }

    /**
     * Creates security constraints part of web.xml descriptor.
     * <p/>
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
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createSecurityConstraints(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        ROOT_LOGGER.creatingSecurityConstraints();
        final SecurityMetaDataAccessorEJB ejbMDAccessor = getEjbSecurityMetaDataAccessor(dep);

        for (final Endpoint ejbEndpoint : dep.getService().getEndpoints()) {
            final boolean secureWsdlAccess = ejbMDAccessor.isSecureWsdlAccess(ejbEndpoint);
            final String transportGuarantee = ejbMDAccessor.getTransportGuarantee(ejbEndpoint);
            final boolean hasTransportGuarantee = transportGuarantee != null;
            final String authMethod = ejbMDAccessor.getAuthMethod(ejbEndpoint);
            final boolean hasAuthMethod = authMethod != null;

            if (ejbEndpoint instanceof HttpEndpoint && (hasAuthMethod || hasTransportGuarantee)) {
                final List<SecurityConstraintMetaData> securityConstraints = WebMetaDataHelper
                        .getSecurityConstraints(jbossWebMD);

                // security-constraint
                final SecurityConstraintMetaData securityConstraint = WebMetaDataHelper
                        .newSecurityConstraint(securityConstraints);

                // web-resource-collection
                final WebResourceCollectionsMetaData webResourceCollections = WebMetaDataHelper
                        .getWebResourceCollections(securityConstraint);
                final String endpointName = ejbEndpoint.getShortName();
                final String urlPattern = ((HttpEndpoint) ejbEndpoint).getURLPattern();
                ROOT_LOGGER.creatingWebResourceCollection(endpointName, urlPattern);
                WebMetaDataHelper.newWebResourceCollection(endpointName, urlPattern, secureWsdlAccess,
                        webResourceCollections);

                // auth-constraint
                if (hasAuthMethod) {
                    ROOT_LOGGER.creatingAuthConstraint(endpointName);
                    WebMetaDataHelper.newAuthConstraint(WebMetaDataHelper.getAllRoles(), securityConstraint);
                }

                // user-data-constraint
                if (hasTransportGuarantee) {
                    ROOT_LOGGER.creatingUserDataConstraint(endpointName, transportGuarantee);
                    WebMetaDataHelper.newUserDataConstraint(transportGuarantee, securityConstraint);
                }
            }
        }
    }

    /**
     * Creates login-config part of web.xml descriptor.
     * <p/>
     * <pre>
     * &lt;login-config&gt;
     *   &lt;auth-method&gt;EjbDeploymentAuthMethod&lt;/auth-method&gt;
     *   &lt;realm-name&gt;EJBWebServiceEndpointServlet Realm&lt;/realm-name&gt;
     * &lt;/login-config&gt;
     * </pre>
     *
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createLoginConfig(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final String authMethod = getAuthMethod(dep);
        final boolean hasAuthMethod = authMethod != null;

        if (hasAuthMethod) {
            ROOT_LOGGER.creatingLoginConfig(EJB_WEBSERVICE_REALM, authMethod);
            final LoginConfigMetaData loginConfig = WebMetaDataHelper.getLoginConfig(jbossWebMD);
            loginConfig.setRealmName(WebMetaDataCreator.EJB_WEBSERVICE_REALM);
            loginConfig.setAuthMethod(authMethod);
        }
    }

    /**
     * Creates security roles part of web.xml descriptor.
     * <p/>
     * <pre>
     * &lt;security-role&gt;
     *   &lt;role-name&gt;role1&lt;/role-name&gt;
     *   ...
     *   &lt;role-name&gt;roleN&lt;/role-name&gt;
     * &lt;/security-role&gt;
     * </pre>
     *
     * @param dep        webservice deployment
     * @param jbossWebMD jboss web meta data
     */
    private void createSecurityRoles(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final String authMethod = getAuthMethod(dep);
        final boolean hasAuthMethod = authMethod != null;

        if (hasAuthMethod) {
            final SecurityMetaDataAccessorEJB ejbMDAccessor = getEjbSecurityMetaDataAccessor(dep);
            final SecurityRolesMetaData securityRolesMD = ejbMDAccessor.getSecurityRoles(dep);
            final boolean hasSecurityRolesMD = securityRolesMD != null;

            if (hasSecurityRolesMD) {
                ROOT_LOGGER.creatingSecurityRoles();
                jbossWebMD.setSecurityRoles(securityRolesMD);
            }
        }

        //merge security roles from the ear
        //TODO: is there somewhere better to put this?
        final DeploymentUnit unit = dep.getAttachment(DeploymentUnit.class);
        DeploymentUnit parent = unit.getParent();
        if (parent != null) {
            final EarMetaData earMetaData = parent.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
            if (earMetaData != null) {
                if (jbossWebMD.getSecurityRoles() == null) {
                    jbossWebMD.setSecurityRoles(new SecurityRolesMetaData());
                }

                SecurityRolesMetaData earSecurityRolesMetaData = earMetaData.getSecurityRoles();
                if (earSecurityRolesMetaData != null) {
                    SecurityRolesMetaDataMerger.merge(jbossWebMD.getSecurityRoles(), jbossWebMD.getSecurityRoles(), earSecurityRolesMetaData);
                }
            }
        }
    }

    /**
     * Returns deployment authentication method.
     *
     * @param dep webservice deployment
     * @return deployment authentication method
     */
    private String getAuthMethod(final Deployment dep) {
        final SecurityMetaDataAccessorEJB ejbMDAccessor = getEjbSecurityMetaDataAccessor(dep);

        for (final Endpoint ejbEndpoint : dep.getService().getEndpoints()) {
            final String beanAuthMethod = ejbMDAccessor.getAuthMethod(ejbEndpoint);
            final boolean hasBeanAuthMethod = beanAuthMethod != null;

            if (hasBeanAuthMethod) {
                // First found auth-method defines war
                // login-config/auth-method
                return beanAuthMethod;
            }
        }

        return null;
    }

    /**
     * Returns security builder associated with EJB deployment.
     *
     * @param dep webservice EJB deployment
     * @return security builder for EJB deployment
     */
    private SecurityMetaDataAccessorEJB getEjbSecurityMetaDataAccessor(final Deployment dep) {
        final boolean isJaxws = WSHelper.isJaxwsDeployment(dep);

        return isJaxws ? ejb3SecurityAccessor : ejb21SecurityAccessor;
    }
}

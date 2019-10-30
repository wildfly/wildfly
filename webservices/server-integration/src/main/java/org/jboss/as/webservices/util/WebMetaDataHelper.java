/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AuthConstraintMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.UserDataConstraintMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;

/**
 * Utility class that simplifies work with JBossWebMetaData object structure.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class WebMetaDataHelper {
    /** Star utility string. */
    private static final String STAR_STRING = "*";

    /** GET http method utility string. */
    private static final String GET_STRING = "GET";

    /** POST http method utility string. */
    private static final String POST_STRING = "POST";

    /** GET and POST methods utility list. */
    private static List<String> getAndPostMethods;

    /** POST method utility list. */
    private static List<String> onlyPostMethod;

    /** All roles utility list. */
    private static List<String> allRoles;

    static {
        final List<String> getAndPostList = new LinkedList<String>();
        getAndPostList.add(WebMetaDataHelper.GET_STRING);
        getAndPostList.add(WebMetaDataHelper.POST_STRING);
        WebMetaDataHelper.getAndPostMethods = Collections.unmodifiableList(getAndPostList);

        final List<String> onlyPostList = new LinkedList<String>();
        onlyPostList.add(WebMetaDataHelper.POST_STRING);
        WebMetaDataHelper.onlyPostMethod = Collections.unmodifiableList(onlyPostList);

        final List<String> roleNamesList = new LinkedList<String>();
        roleNamesList.add(WebMetaDataHelper.STAR_STRING);
        WebMetaDataHelper.allRoles = Collections.unmodifiableList(roleNamesList);
    }

    /**
     * Constructor.
     */
    private WebMetaDataHelper() {
        super();
    }

    /**
     * Creates URL pattern list from passed string.
     *
     * @param urlPattern URL pattern
     * @return list wrapping passed parameter
     */
    public static List<String> getUrlPatterns(final String urlPattern) {
        final List<String> linkedList = new LinkedList<String>();

        linkedList.add(urlPattern);

        return linkedList;
    }

    /**
     * If WSDL access is secured, it returns both POST and GET methods, otherwise only POST method.
     *
     * @param secureWsdlAccess whether WSDL is secured
     * @return web access methods
     */
    public static List<String> getHttpMethods(final boolean secureWsdlAccess) {
        return secureWsdlAccess ? WebMetaDataHelper.getAndPostMethods : WebMetaDataHelper.onlyPostMethod;
    }

    /**
     * Returns all role list.
     *
     * @return all role list
     */
    public static List<String> getAllRoles() {
        return WebMetaDataHelper.allRoles;
    }

    /**
     * Gets servlets meta data from jboss web meta data. If not found it creates new servlets meta data and associates them
     * with jboss web meta data.
     *
     * @param jbossWebMD jboss web meta data
     * @return servlets meta data
     */
    public static JBossServletsMetaData getServlets(final JBossWebMetaData jbossWebMD) {
        JBossServletsMetaData servletsMD = jbossWebMD.getServlets();

        if (servletsMD == null) {
            servletsMD = new JBossServletsMetaData();
            jbossWebMD.setServlets(servletsMD);
        }

        return servletsMD;
    }

    /**
     * Gets servlet mappings meta data from jboss web meta data. If not found it creates new servlet mappings meta data and
     * associates them with jboss web meta data.
     *
     * @param jbossWebMD jboss web meta data
     * @return servlet mappings meta data
     */
    public static List<ServletMappingMetaData> getServletMappings(final JBossWebMetaData jbossWebMD) {
        List<ServletMappingMetaData> servletMappingsMD = jbossWebMD.getServletMappings();

        if (servletMappingsMD == null) {
            servletMappingsMD = new LinkedList<ServletMappingMetaData>();
            jbossWebMD.setServletMappings(servletMappingsMD);
        }

        return servletMappingsMD;
    }

    /**
     * Gets security constraints meta data from jboss web meta data. If not found it creates new security constraints meta data
     * and associates them with jboss web meta data.
     *
     * @param jbossWebMD jboss web meta data
     * @return security constraints meta data
     */
    public static List<SecurityConstraintMetaData> getSecurityConstraints(final JBossWebMetaData jbossWebMD) {
        List<SecurityConstraintMetaData> securityConstraintsMD = jbossWebMD.getSecurityConstraints();

        if (securityConstraintsMD == null) {
            securityConstraintsMD = new LinkedList<SecurityConstraintMetaData>();
            jbossWebMD.setSecurityConstraints(securityConstraintsMD);
        }

        return securityConstraintsMD;
    }

    /**
     * Gets login config meta data from jboss web meta data. If not found it creates new login config meta data and associates
     * them with jboss web meta data.
     *
     * @param jbossWebMD jboss web meta data
     * @return login config meta data
     */
    public static LoginConfigMetaData getLoginConfig(final JBossWebMetaData jbossWebMD) {
        LoginConfigMetaData loginConfigMD = jbossWebMD.getLoginConfig();

        if (loginConfigMD == null) {
            loginConfigMD = new LoginConfigMetaData();
            jbossWebMD.setLoginConfig(loginConfigMD);
        }

        return loginConfigMD;
    }

    /**
     * Gets context parameters meta data from jboss web meta data. If not found it creates new context parameters meta data and
     * associates them with jboss web meta data.
     *
     * @param jbossWebMD jboss web meta data
     * @return context parameters meta data
     */
    public static List<ParamValueMetaData> getContextParams(final JBossWebMetaData jbossWebMD) {
        List<ParamValueMetaData> contextParamsMD = jbossWebMD.getContextParams();

        if (contextParamsMD == null) {
            contextParamsMD = new LinkedList<ParamValueMetaData>();
            jbossWebMD.setContextParams(contextParamsMD);
        }

        return contextParamsMD;
    }

    /**
     * Gets web resource collections meta data from security constraint meta data. If not found it creates new web resource
     * collections meta data and associates them with security constraint meta data.
     *
     * @param securityConstraintMD security constraint meta data
     * @return web resource collections meta data
     */
    public static WebResourceCollectionsMetaData getWebResourceCollections(final SecurityConstraintMetaData securityConstraintMD) {
        WebResourceCollectionsMetaData webResourceCollectionsMD = securityConstraintMD.getResourceCollections();

        if (webResourceCollectionsMD == null) {
            webResourceCollectionsMD = new WebResourceCollectionsMetaData();
            securityConstraintMD.setResourceCollections(webResourceCollectionsMD);
        }

        return webResourceCollectionsMD;
    }

    /**
     * Gets init parameters meta data from servlet meta data. If not found it creates new init parameters meta data and
     * associates them with servlet meta data.
     *
     * @param servletMD servlet meta data
     * @return init parameters meta data
     */
    public static List<ParamValueMetaData> getServletInitParams(final ServletMetaData servletMD) {
        List<ParamValueMetaData> initParamsMD = servletMD.getInitParam();

        if (initParamsMD == null) {
            initParamsMD = new LinkedList<ParamValueMetaData>();
            servletMD.setInitParam(initParamsMD);
        }

        return initParamsMD;
    }

    /**
     * Creates new security constraint meta data and associates them with security constraints meta data.
     *
     * @param securityConstraintsMD security constraints meta data
     * @return new security constraing meta data
     */
    public static SecurityConstraintMetaData newSecurityConstraint(final List<SecurityConstraintMetaData> securityConstraintsMD) {
        final SecurityConstraintMetaData securityConstraintMD = new SecurityConstraintMetaData();

        securityConstraintsMD.add(securityConstraintMD);

        return securityConstraintMD;
    }

    /**
     * Creates new web resource collection meta data and associates them with web resource collections meta data.
     *
     * @param servletName servlet name
     * @param urlPattern URL pattern
     * @param securedWsdl whether WSDL access is secured
     * @param webResourceCollectionsMD web resource collections meta data
     * @return new web resource collection meta data
     */
    public static WebResourceCollectionMetaData newWebResourceCollection(final String servletName, final String urlPattern,
            final boolean securedWsdl, final WebResourceCollectionsMetaData webResourceCollectionsMD) {
        final WebResourceCollectionMetaData webResourceCollectionMD = new WebResourceCollectionMetaData();

        webResourceCollectionMD.setWebResourceName(servletName);
        webResourceCollectionMD.setUrlPatterns(WebMetaDataHelper.getUrlPatterns(urlPattern));
        webResourceCollectionMD.setHttpMethods(WebMetaDataHelper.getHttpMethods(securedWsdl));
        webResourceCollectionsMD.add(webResourceCollectionMD);

        return webResourceCollectionMD;
    }

    /**
     * Creates new servlet meta data and associates them with servlets meta data.
     *
     * @param servletName servlet name
     * @param servletClass servlet class name
     * @param servletsMD servlets meta data
     * @return new servlet meta data
     */
    public static JBossServletMetaData newServlet(final String servletName, final String servletClass,
            final JBossServletsMetaData servletsMD) {
        final JBossServletMetaData servletMD = new JBossServletMetaData();

        servletMD.setServletName(servletName);
        servletMD.setServletClass(servletClass);
        servletsMD.add(servletMD);

        return servletMD;
    }

    /**
     * Creates new servlet mapping meta data and associates them with servlet mappings meta data.
     *
     * @param servletName servlet name
     * @param urlPatterns URL patterns
     * @param servletMappingsMD servlet mapping meta data
     * @return new servlet mapping meta data
     */
    public static ServletMappingMetaData newServletMapping(final String servletName, final List<String> urlPatterns,
            final List<ServletMappingMetaData> servletMappingsMD) {
        final ServletMappingMetaData servletMappingMD = new ServletMappingMetaData();

        servletMappingMD.setServletName(servletName);
        servletMappingMD.setUrlPatterns(urlPatterns);
        servletMappingsMD.add(servletMappingMD);

        return servletMappingMD;
    }

    /**
     * Creates new authentication constraint and associates it with security constraint meta data.
     *
     * @param roleNames roles
     * @param securityConstraintMD security constraint meta data
     * @return new authentication constraint meta data
     */
    public static AuthConstraintMetaData newAuthConstraint(final List<String> roleNames,
            final SecurityConstraintMetaData securityConstraintMD) {
        final AuthConstraintMetaData authConstraintMD = new AuthConstraintMetaData();

        authConstraintMD.setRoleNames(roleNames);
        securityConstraintMD.setAuthConstraint(authConstraintMD);

        return authConstraintMD;
    }

    /**
     * Creates new user constraint meta data and associates it with security constraint meta data.
     *
     * @param transportGuarantee transport guarantee value
     * @param securityConstraintMD security constraint meta data
     * @return new user data constraint meta data
     */
    public static UserDataConstraintMetaData newUserDataConstraint(final String transportGuarantee,
            final SecurityConstraintMetaData securityConstraintMD) {
        final UserDataConstraintMetaData userDataConstraintMD = new UserDataConstraintMetaData();
        final TransportGuaranteeType transportGuaranteeValue = TransportGuaranteeType.valueOf(transportGuarantee);

        userDataConstraintMD.setTransportGuarantee(transportGuaranteeValue);
        securityConstraintMD.setUserDataConstraint(userDataConstraintMD);

        return userDataConstraintMD;
    }

    /**
     * Creates new parameter meta data and associates it with parameters meta data.
     *
     * @param key parameter key
     * @param value parameter value
     * @param paramsMD parameters meta data
     * @return new parameter meta data
     */
    public static ParamValueMetaData newParamValue(final String key, final String value, final List<ParamValueMetaData> paramsMD) {
        final ParamValueMetaData paramValueMD = WebMetaDataHelper.newParamValue(key, value);

        paramsMD.add(paramValueMD);

        return paramValueMD;
    }

    /**
     * Creates new parameter with specified key and value.
     *
     * @param key the key
     * @param value the value
     * @return new parameter
     */
    private static ParamValueMetaData newParamValue(final String key, final String value) {
        final ParamValueMetaData paramMD = new ParamValueMetaData();

        paramMD.setParamName(key);
        paramMD.setParamValue(value);

        return paramMD;
    }
}

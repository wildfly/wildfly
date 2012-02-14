/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.Multipart;
import org.apache.catalina.deploy.jsp.FunctionInfo;
import org.apache.catalina.deploy.jsp.TagAttributeInfo;
import org.apache.catalina.deploy.jsp.TagFileInfo;
import org.apache.catalina.deploy.jsp.TagInfo;
import org.apache.catalina.deploy.jsp.TagLibraryInfo;
import org.apache.catalina.deploy.jsp.TagLibraryValidatorInfo;
import org.apache.catalina.deploy.jsp.TagVariableInfo;
import org.apache.catalina.startup.ContextConfig;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.annotation.javaee.Icon;
import org.jboss.as.clustering.ClassLoaderAwareClassResolver;
import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.WebLogger;
import org.jboss.as.web.deployment.helpers.VFSDirContext;
import org.jboss.as.web.session.DistributableSessionManager;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefsMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.merge.web.jboss.JBossWebMetaDataMerger;
import org.jboss.metadata.web.jboss.ContainerListenerMetaData;
import org.jboss.metadata.web.jboss.JBossAnnotationsMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.metadata.web.spec.AnnotationMetaData;
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.AuthConstraintMetaData;
import org.jboss.metadata.web.spec.CookieConfigMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.ErrorPageMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.JspConfigMetaData;
import org.jboss.metadata.web.spec.JspPropertyGroupMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LocaleEncodingMetaData;
import org.jboss.metadata.web.spec.LocaleEncodingsMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletSecurityMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.SessionTrackingModeType;
import org.jboss.metadata.web.spec.TagFileMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TaglibMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.VariableMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;
import org.jboss.metadata.web.spec.WelcomeFileListMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VirtualFile;

/**
 * @author Remy Maucherat
 */
public class JBossContextConfig extends ContextConfig {
    private DeploymentUnit deploymentUnitContext = null;
    private Set<String> overlays = new HashSet<String>();
    private final InjectedValue<DistributedCacheManagerFactory> factory = new InjectedValue<DistributedCacheManagerFactory>();

    /**
     * <p>
     * Creates a new instance of {@code JBossContextConfig}.
     * </p>
     */
    public JBossContextConfig(DeploymentUnit deploymentUnitContext) {
        super();
        this.deploymentUnitContext = deploymentUnitContext;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType().equals(Lifecycle.AFTER_START_EVENT)) {
            // Invoke ServletContainerInitializer
            final ScisMetaData scisMetaData = deploymentUnitContext.getAttachment(ScisMetaData.ATTACHMENT_KEY);
            if (scisMetaData != null) {
                for (ServletContainerInitializer sci : scisMetaData.getScis()) {
                    try {
                        sci.onStartup(scisMetaData.getHandlesTypes().get(sci), context.getServletContext());
                    } catch (Throwable t) {
                        WebLogger.WEB_LOGGER.sciOnStartupError(sci.getClass().getName(), t);
                        ok = false;
                    }
                }
            }
            // Post order
            final WarMetaData warMetaData = deploymentUnitContext.getAttachment(WarMetaData.ATTACHMENT_KEY);
            List<String> order = warMetaData.getOrder();
            if (!warMetaData.isNoOrder()) {
                context.getServletContext().setAttribute(ServletContext.ORDERED_LIBS, order);
            }
        }
        super.lifecycleEvent(event);
    }

    @Override
    protected void applicationWebConfig() {
        final WarMetaData warMetaData = deploymentUnitContext.getAttachment(WarMetaData.ATTACHMENT_KEY);
        processJBossWebMetaData(warMetaData.getMergedJBossWebMetaData());
        processWebMetaData(warMetaData.getMergedJBossWebMetaData());
    }

    @Override
    protected void defaultWebConfig() {
        JBossWebMetaData sharedJBossWebMetaData = new JBossWebMetaData();
        final WarMetaData warMetaData = deploymentUnitContext.getAttachment(WarMetaData.ATTACHMENT_KEY);
        // FIXME: Default jboss-web.xml config
        JBossWebMetaDataMerger.merge(sharedJBossWebMetaData, null, warMetaData.getSharedWebMetaData());
        processJBossWebMetaData(sharedJBossWebMetaData);
        processWebMetaData(sharedJBossWebMetaData);
    }

    protected void processJBossWebMetaData(JBossWebMetaData metaData) {
        // Valves
        List<ValveMetaData> valves = metaData.getValves();
        if (valves != null) {
            for (ValveMetaData valve : valves) {
                Valve valveInstance = (Valve) getInstance(valve.getModule(), valve.getValveClass(), valve.getParams());
                if (ok) {
                    context.getPipeline().addValve(valveInstance);
                }
            }
        }
        // Overlays
        if (metaData.getOverlays() != null) {
            overlays.addAll(metaData.getOverlays());
        }
        // Container listeners
        List<ContainerListenerMetaData> listeners = metaData.getContainerListeners();
        if (listeners != null) {
            for (ContainerListenerMetaData listener : listeners) {
                switch (listener.getListenerType()) {
                    case CONTAINER:
                        ContainerListener containerListener = (ContainerListener) getInstance(listener.getModule(), listener.getListenerClass(), listener.getParams());
                        context.addContainerListener(containerListener);
                        break;
                    case LIFECYCLE:
                        LifecycleListener lifecycleListener = (LifecycleListener) getInstance(listener.getModule(), listener.getListenerClass(), listener.getParams());
                        if (context instanceof Lifecycle) {
                            ((Lifecycle) context).addLifecycleListener(lifecycleListener);
                        }
                        break;
                   case SERVLET_INSTANCE:
                        context.addInstanceListener(listener.getListenerClass());
                        break;
                    case SERVLET_CONTAINER:
                        context.addWrapperListener(listener.getListenerClass());
                        break;
                    case SERVLET_LIFECYCLE:
                        context.addWrapperLifecycle(listener.getListenerClass());
                        break;
                }
            }
        }
    }

    protected Object getInstance(String moduleName, String className, List<ParamValueMetaData> params) {
        try {
            final Module module = deploymentUnitContext.getAttachment(Attachments.MODULE);
            ClassLoader moduleClassLoader = null;
            if (moduleName == null) {
                if (context.getLoader() == null || context.getLoader().getClassLoader() == null) {
                    moduleClassLoader = module.getClassLoader();
                } else {
                    moduleClassLoader = context.getLoader().getClassLoader();
                }
            } else {
                moduleClassLoader = module.getModule(ModuleIdentifier.create(moduleName)).getClassLoader();
            }
            Object instance = moduleClassLoader.loadClass(className).newInstance();
            if (params != null) {
                for (ParamValueMetaData param : params) {
                    IntrospectionUtils.setProperty(instance, param.getParamName(), param.getParamValue());
                }
            }
            return instance;
        } catch (Throwable t) {
            WebLogger.WEB_LOGGER.componentInstanceCreationFailed(className, t);
            ok = false;
        }
        return null;
    }

    protected void processWebMetaData(JBossWebMetaData metaData) {
        if (context instanceof StandardContext) {
            ((StandardContext) context).setReplaceWelcomeFiles(true);
        }

        // Version
        context.setVersion(metaData.getVersion());

        // SetPublicId
        if (metaData.is30())
            context.setPublicId("/javax/servlet/resources/web-app_3_0.dtd");
        else if (metaData.is25())
            context.setPublicId("/javax/servlet/resources/web-app_2_5.dtd");
        else if (metaData.is24())
            context.setPublicId("/javax/servlet/resources/web-app_2_4.dtd");
        else if (metaData.is23())
            context.setPublicId(org.apache.catalina.startup.Constants.WebDtdPublicId_23);
        else
            context.setPublicId(org.apache.catalina.startup.Constants.WebDtdPublicId_22);

        // Display name
        DescriptionGroupMetaData dg = metaData.getDescriptionGroup();
        if (dg != null) {
            String displayName = dg.getDisplayName();
            if (displayName != null) {
                context.setDisplayName(displayName);
            }
        }

        // Distributable
        if (metaData.getDistributable() != null) {
            try {
                Module module = this.deploymentUnitContext.getAttachment(Attachments.MODULE);
                ClassResolver resolver = ModularClassResolver.getInstance(module.getModuleLoader());
                context.setManager(new DistributableSessionManager<OutgoingDistributableSessionData>(this.factory.getValue(), this.context, metaData, new ClassLoaderAwareClassResolver(resolver, module.getClassLoader())));
                context.setDistributable(true);
            } catch (Exception e) {
                WebLogger.WEB_LOGGER.clusteringNotSupported();
            }
        }

        // Context params
        List<ParamValueMetaData> contextParams = metaData.getContextParams();
        if (contextParams != null) {
            for (ParamValueMetaData param : contextParams) {
                context.addParameter(param.getParamName(), param.getParamValue());
            }
        }

        // Error pages
        List<ErrorPageMetaData> errorPages = metaData.getErrorPages();
        if (errorPages != null) {
            for (ErrorPageMetaData value : errorPages) {
                org.apache.catalina.deploy.ErrorPage errorPage = new org.apache.catalina.deploy.ErrorPage();
                errorPage.setErrorCode(value.getErrorCode());
                errorPage.setExceptionType(value.getExceptionType());
                errorPage.setLocation(value.getLocation());
                context.addErrorPage(errorPage);
            }
        }

        // Filter definitions
        FiltersMetaData filters = metaData.getFilters();
        if (filters != null) {
            for (FilterMetaData value : filters) {
                org.apache.catalina.deploy.FilterDef filterDef = new org.apache.catalina.deploy.FilterDef();
                filterDef.setFilterName(value.getName());
                filterDef.setFilterClass(value.getFilterClass());
                if (value.getInitParam() != null)
                    for (ParamValueMetaData param : value.getInitParam()) {
                        filterDef.addInitParameter(param.getParamName(), param.getParamValue());
                    }
                filterDef.setAsyncSupported(value.isAsyncSupported());
                context.addFilterDef(filterDef);
            }
        }

        // Filter mappings
        List<FilterMappingMetaData> filtersMappings = metaData.getFilterMappings();
        if (filtersMappings != null) {
            for (FilterMappingMetaData value : filtersMappings) {
                org.apache.catalina.deploy.FilterMap filterMap = new org.apache.catalina.deploy.FilterMap();
                filterMap.setFilterName(value.getFilterName());
                List<String> servletNames = value.getServletNames();
                if (servletNames != null) {
                    for (String name : servletNames)
                        filterMap.addServletName(name);
                }
                List<String> urlPatterns = value.getUrlPatterns();
                if (urlPatterns != null) {
                    for (String pattern : urlPatterns)
                        filterMap.addURLPattern(pattern);
                }
                List<DispatcherType> dispatchers = value.getDispatchers();
                if (dispatchers != null) {
                    for (DispatcherType type : dispatchers)
                        filterMap.setDispatcher(type.name());
                }
                context.addFilterMap(filterMap);
            }
        }

        // Listeners
        List<ListenerMetaData> listeners = metaData.getListeners();
        if (listeners != null) {
            for (ListenerMetaData value : listeners) {
                context.addApplicationListener(value.getListenerClass());
            }
        }

        // Login configuration
        LoginConfigMetaData loginConfig = metaData.getLoginConfig();
        if (loginConfig != null) {
            org.apache.catalina.deploy.LoginConfig loginConfig2 = new org.apache.catalina.deploy.LoginConfig();
            loginConfig2.setAuthMethod(loginConfig.getAuthMethod());
            loginConfig2.setRealmName(loginConfig.getRealmName());
            if (loginConfig.getFormLoginConfig() != null) {
                loginConfig2.setLoginPage(loginConfig.getFormLoginConfig().getLoginPage());
                loginConfig2.setErrorPage(loginConfig.getFormLoginConfig().getErrorPage());
            }
            context.setLoginConfig(loginConfig2);
        }

        // MIME mappings
        List<MimeMappingMetaData> mimes = metaData.getMimeMappings();
        if (mimes != null) {
            for (MimeMappingMetaData value : mimes) {
                context.addMimeMapping(value.getExtension(), value.getMimeType());
            }
        }

        // Security constraints
        List<SecurityConstraintMetaData> scs = metaData.getSecurityConstraints();
        if (scs != null) {
            for (SecurityConstraintMetaData value : scs) {
                org.apache.catalina.deploy.SecurityConstraint constraint = new org.apache.catalina.deploy.SecurityConstraint();
                TransportGuaranteeType tg = value.getTransportGuarantee();
                constraint.setUserConstraint(tg.name());
                AuthConstraintMetaData acmd = value.getAuthConstraint();
                constraint.setAuthConstraint(acmd != null);
                if (acmd != null) {
                    if (acmd.getRoleNames() != null)
                        for (String role : acmd.getRoleNames()) {
                            constraint.addAuthRole(role);
                        }
                }
                WebResourceCollectionsMetaData wrcs = value.getResourceCollections();
                if (wrcs != null) {
                    for (WebResourceCollectionMetaData wrc : wrcs) {
                        org.apache.catalina.deploy.SecurityCollection collection2 = new org.apache.catalina.deploy.SecurityCollection();
                        collection2.setName(wrc.getName());
                        List<String> methods = wrc.getHttpMethods();
                        if (methods != null) {
                            for (String method : wrc.getHttpMethods()) {
                                collection2.addMethod(method);
                            }
                        }
                        List<String> methodOmissions = wrc.getHttpMethodOmissions();
                        if (methodOmissions != null) {
                            for (String method : wrc.getHttpMethodOmissions()) {
                                collection2.addMethodOmission(method);
                            }
                        }
                        List<String> patterns = wrc.getUrlPatterns();
                        if (patterns != null) {
                            for (String pattern : patterns) {
                                collection2.addPattern(pattern);
                            }
                        }
                        constraint.addCollection(collection2);
                    }
                }
                context.addConstraint(constraint);
            }
        }

        // Security roles
        SecurityRolesMetaData roles = metaData.getSecurityRoles();
        if (roles != null) {
            for (SecurityRoleMetaData value : roles) {
                context.addSecurityRole(value.getRoleName());
            }
        }

        // Servlet
        JBossServletsMetaData servlets = metaData.getServlets();
        if (servlets != null) {
            for (JBossServletMetaData value : servlets) {
                org.apache.catalina.Wrapper wrapper = context.createWrapper();
                wrapper.setName(value.getName());
                wrapper.setServletClass(value.getServletClass());
                if (value.getJspFile() != null) {
                    wrapper.setJspFile(value.getJspFile());
                }
                wrapper.setLoadOnStartup(value.getLoadOnStartupInt());
                if (value.getRunAs() != null) {
                    wrapper.setRunAs(value.getRunAs().getRoleName());
                }
                List<ParamValueMetaData> params = value.getInitParam();
                if (params != null) {
                    for (ParamValueMetaData param : params) {
                        wrapper.addInitParameter(param.getParamName(), param.getParamValue());
                    }
                }
                SecurityRoleRefsMetaData refs = value.getSecurityRoleRefs();
                if (refs != null) {
                    for (SecurityRoleRefMetaData ref : refs) {
                        wrapper.addSecurityReference(ref.getRoleName(), ref.getRoleLink());
                    }
                }
                wrapper.setAsyncSupported(value.isAsyncSupported());
                wrapper.setEnabled(value.isEnabled());
                // Multipart configuration
                if (value.getMultipartConfig() != null) {
                    MultipartConfigMetaData multipartConfigMetaData = value.getMultipartConfig();
                    Multipart multipartConfig = new Multipart();
                    multipartConfig.setLocation(multipartConfigMetaData.getLocation());
                    multipartConfig.setMaxRequestSize(multipartConfigMetaData.getMaxRequestSize());
                    multipartConfig.setMaxFileSize(multipartConfigMetaData.getMaxFileSize());
                    multipartConfig.setFileSizeThreshold(multipartConfigMetaData.getFileSizeThreshold());
                    wrapper.setMultipartConfig(multipartConfig);
                }
                context.addChild(wrapper);
            }
        }

        // Servlet mapping
        List<ServletMappingMetaData> smappings = metaData.getServletMappings();
        if (smappings != null) {
            for (ServletMappingMetaData value : smappings) {
                List<String> urlPatterns = value.getUrlPatterns();
                if (urlPatterns != null) {
                    for (String pattern : urlPatterns)
                        context.addServletMapping(pattern, value.getServletName());
                }
            }
        }

        // JSP Config
        JspConfigMetaData config = metaData.getJspConfig();
        if (config != null) {
            // JSP Property groups
            List<JspPropertyGroupMetaData> groups = config.getPropertyGroups();
            if (groups != null) {
                for (JspPropertyGroupMetaData group : groups) {
                    org.apache.catalina.deploy.JspPropertyGroup jspPropertyGroup = new org.apache.catalina.deploy.JspPropertyGroup();
                    for (String pattern : group.getUrlPatterns()) {
                        jspPropertyGroup.addUrlPattern(pattern);
                    }
                    jspPropertyGroup.setElIgnored(group.getElIgnored());
                    jspPropertyGroup.setPageEncoding(group.getPageEncoding());
                    jspPropertyGroup.setScriptingInvalid(group.getScriptingInvalid());
                    jspPropertyGroup.setIsXml(group.getIsXml());
                    if (group.getIncludePreludes() != null) {
                        for (String includePrelude : group.getIncludePreludes()) {
                            jspPropertyGroup.addIncludePrelude(includePrelude);
                        }
                    }
                    if (group.getIncludeCodas() != null) {
                        for (String includeCoda : group.getIncludeCodas()) {
                            jspPropertyGroup.addIncludeCoda(includeCoda);
                        }
                    }
                    jspPropertyGroup.setDeferredSyntaxAllowedAsLiteral(group.getDeferredSyntaxAllowedAsLiteral());
                    jspPropertyGroup.setTrimDirectiveWhitespaces(group.getTrimDirectiveWhitespaces());
                    jspPropertyGroup.setDefaultContentType(group.getDefaultContentType());
                    jspPropertyGroup.setBuffer(group.getBuffer());
                    jspPropertyGroup.setErrorOnUndeclaredNamespace(group.getErrorOnUndeclaredNamespace());
                    context.addJspPropertyGroup(jspPropertyGroup);
                }
            }
            // Taglib
            List<TaglibMetaData> taglibs = config.getTaglibs();
            if (taglibs != null) {
                for (TaglibMetaData taglib : taglibs) {
                    context.addTaglib(taglib.getTaglibUri(), taglib.getTaglibLocation());
                }
            }
        }

        // Locale encoding mapping
        LocaleEncodingsMetaData locales = metaData.getLocalEncodings();
        if (locales != null) {
            for (LocaleEncodingMetaData value : locales.getMappings()) {
                context.addLocaleEncodingMappingParameter(value.getLocale(), value.getEncoding());
            }
        }

        // Welcome files
        WelcomeFileListMetaData welcomeFiles = metaData.getWelcomeFileList();
        if (welcomeFiles != null) {
            for (String value : welcomeFiles.getWelcomeFiles())
                context.addWelcomeFile(value);
        }

        // Session timeout
        SessionConfigMetaData scmd = metaData.getSessionConfig();
        if (scmd != null) {
            context.setSessionTimeout(scmd.getSessionTimeout());
            if (scmd.getSessionTrackingModes() != null) {
                for (SessionTrackingModeType stmt : scmd.getSessionTrackingModes()) {
                    context.addSessionTrackingMode(stmt.toString());
                }
            }
            if (scmd.getCookieConfig() != null) {
                CookieConfigMetaData value = scmd.getCookieConfig();
                org.apache.catalina.deploy.SessionCookie cookieConfig = new org.apache.catalina.deploy.SessionCookie();
                cookieConfig.setName(value.getName());
                cookieConfig.setDomain(value.getDomain());
                cookieConfig.setPath(value.getPath());
                cookieConfig.setComment(value.getComment());
                cookieConfig.setHttpOnly(value.getHttpOnly());
                cookieConfig.setSecure(value.getSecure());
                cookieConfig.setMaxAge(value.getMaxAge());
                context.setSessionCookie(cookieConfig);
            }
        }
    }

    /**
     * Process a "init" event for this Context.
     */
    @Override
    protected void init() {
        context.setConfigured(false);
        ok = true;
    }

    @Override
    protected void destroy() {
    }

    /**
     * Migrate TLD metadata to Catalina. This is separate, and is not subject to
     * the order defined.
     */
    @Override
    protected void applicationTldConfig() {
        final TldsMetaData tldsMetaData = deploymentUnitContext.getAttachment(TldsMetaData.ATTACHMENT_KEY);
        if (tldsMetaData == null) {
            return;
        }
        Map<String, TldMetaData> localTlds = tldsMetaData.getTlds();
        List<TldMetaData> sharedTlds = tldsMetaData.getSharedTlds(deploymentUnitContext);
        ArrayList<TagLibraryInfo> tagLibraries = new ArrayList<TagLibraryInfo>();

        for (String location : localTlds.keySet()) {
            processTld(tagLibraries, location, localTlds.get(location));
        }
        for (TldMetaData sharedTld : sharedTlds) {
            processTld(tagLibraries, null, sharedTld);
        }

        // Add additional TLDs URIs from explicit web config
        String[] taglibs = context.findTaglibs();
        for (int i = 0; i < taglibs.length; i++) {
            String uri = taglibs[i];
            String path = context.findTaglib(taglibs[i]);
            String location = "";
            if (path.indexOf(':') == -1 && !path.startsWith("/")) {
                path = "/WEB-INF/" + path;
            }
            if (path.endsWith(".jar")) {
                location = path;
                path = "META-INF/taglib.tld";
            }
            for (int j = 0; j < tagLibraries.size(); j++) {
                TagLibraryInfo tagLibraryInfo = tagLibraries.get(j);
                if (tagLibraryInfo.getLocation().equals(location) && tagLibraryInfo.getPath().equals(path)) {
                    context.addJspTagLibrary(uri, tagLibraryInfo);
                }
            }
        }

    }

    protected void processTld(ArrayList<TagLibraryInfo> tagLibraries, String location, TldMetaData tldMetaData) {

        String relativeLocation = location;
        String jarPath = null;
        if (relativeLocation != null && relativeLocation.startsWith("/WEB-INF/lib/")) {
            int pos = relativeLocation.indexOf('/', "/WEB-INF/lib/".length());
            if (pos > 0) {
                jarPath = relativeLocation.substring(pos);
                if (jarPath.startsWith("/")) {
                    jarPath = jarPath.substring(1);
                }
                relativeLocation = relativeLocation.substring(0, pos);
            }
        }

        TagLibraryInfo tagLibraryInfo = new TagLibraryInfo();
        tagLibraryInfo.setTlibversion(tldMetaData.getTlibVersion());
        if (tldMetaData.getJspVersion() == null)
            tagLibraryInfo.setJspversion(tldMetaData.getVersion());
        else
            tagLibraryInfo.setJspversion(tldMetaData.getJspVersion());
        tagLibraryInfo.setShortname(tldMetaData.getShortName());
        tagLibraryInfo.setUri(tldMetaData.getUri());
        if (tldMetaData.getDescriptionGroup() != null) {
            tagLibraryInfo.setInfo(tldMetaData.getDescriptionGroup().getDescription());
        }
        // Listener
        if (tldMetaData.getListeners() != null) {
            for (ListenerMetaData listener : tldMetaData.getListeners()) {
                tagLibraryInfo.addListener(listener.getListenerClass());
            }
        }
        // Validator
        if (tldMetaData.getValidator() != null) {
            TagLibraryValidatorInfo tagLibraryValidatorInfo = new TagLibraryValidatorInfo();
            tagLibraryValidatorInfo.setValidatorClass(tldMetaData.getValidator().getValidatorClass());
            if (tldMetaData.getValidator().getInitParams() != null) {
                for (ParamValueMetaData paramValueMetaData : tldMetaData.getValidator().getInitParams()) {
                    tagLibraryValidatorInfo.addInitParam(paramValueMetaData.getParamName(), paramValueMetaData.getParamValue());
                }
            }
            tagLibraryInfo.setValidator(tagLibraryValidatorInfo);
        }
        // Tag
        if (tldMetaData.getTags() != null) {
            for (TagMetaData tagMetaData : tldMetaData.getTags()) {
                TagInfo tagInfo = new TagInfo();
                tagInfo.setTagName(tagMetaData.getName());
                tagInfo.setTagClassName(tagMetaData.getTagClass());
                tagInfo.setTagExtraInfo(tagMetaData.getTeiClass());
                if (tagMetaData.getBodyContent() != null)
                    tagInfo.setBodyContent(tagMetaData.getBodyContent().toString());
                tagInfo.setDynamicAttributes(tagMetaData.getDynamicAttributes());
                // Description group
                if (tagMetaData.getDescriptionGroup() != null) {
                    DescriptionGroupMetaData descriptionGroup = tagMetaData.getDescriptionGroup();
                    if (descriptionGroup.getIcons() != null && descriptionGroup.getIcons().value() != null
                            && (descriptionGroup.getIcons().value().length > 0)) {
                        Icon icon = descriptionGroup.getIcons().value()[0];
                        tagInfo.setLargeIcon(icon.largeIcon());
                        tagInfo.setSmallIcon(icon.smallIcon());
                    }
                    tagInfo.setInfoString(descriptionGroup.getDescription());
                    tagInfo.setDisplayName(descriptionGroup.getDisplayName());
                }
                // Variable
                if (tagMetaData.getVariables() != null) {
                    for (VariableMetaData variableMetaData : tagMetaData.getVariables()) {
                        TagVariableInfo tagVariableInfo = new TagVariableInfo();
                        tagVariableInfo.setNameGiven(variableMetaData.getNameGiven());
                        tagVariableInfo.setNameFromAttribute(variableMetaData.getNameFromAttribute());
                        tagVariableInfo.setClassName(variableMetaData.getVariableClass());
                        tagVariableInfo.setDeclare(variableMetaData.getDeclare());
                        if (variableMetaData.getScope() != null)
                            tagVariableInfo.setScope(variableMetaData.getScope().toString());
                        tagInfo.addTagVariableInfo(tagVariableInfo);
                    }
                }
                // Attribute
                if (tagMetaData.getAttributes() != null) {
                    for (AttributeMetaData attributeMetaData : tagMetaData.getAttributes()) {
                        TagAttributeInfo tagAttributeInfo = new TagAttributeInfo();
                        tagAttributeInfo.setName(attributeMetaData.getName());
                        tagAttributeInfo.setType(attributeMetaData.getType());
                        tagAttributeInfo.setReqTime(attributeMetaData.getRtexprvalue());
                        tagAttributeInfo.setRequired(attributeMetaData.getRequired());
                        tagAttributeInfo.setFragment(attributeMetaData.getFragment());
                        if (attributeMetaData.getDeferredValue() != null) {
                            tagAttributeInfo.setDeferredValue("true");
                            tagAttributeInfo.setExpectedTypeName(attributeMetaData.getDeferredValue().getType());
                        } else {
                            tagAttributeInfo.setDeferredValue("false");
                        }
                        if (attributeMetaData.getDeferredMethod() != null) {
                            tagAttributeInfo.setDeferredMethod("true");
                            tagAttributeInfo.setMethodSignature(attributeMetaData.getDeferredMethod().getMethodSignature());
                        } else {
                            tagAttributeInfo.setDeferredMethod("false");
                        }
                        tagInfo.addTagAttributeInfo(tagAttributeInfo);
                    }
                }
                tagLibraryInfo.addTagInfo(tagInfo);
            }
        }
        // Tag files
        if (tldMetaData.getTagFiles() != null) {
            for (TagFileMetaData tagFileMetaData : tldMetaData.getTagFiles()) {
                TagFileInfo tagFileInfo = new TagFileInfo();
                tagFileInfo.setName(tagFileMetaData.getName());
                tagFileInfo.setPath(tagFileMetaData.getPath());
                tagLibraryInfo.addTagFileInfo(tagFileInfo);
            }
        }
        // Function
        if (tldMetaData.getFunctions() != null) {
            for (FunctionMetaData functionMetaData : tldMetaData.getFunctions()) {
                FunctionInfo functionInfo = new FunctionInfo();
                functionInfo.setName(functionMetaData.getName());
                functionInfo.setFunctionClass(functionMetaData.getFunctionClass());
                functionInfo.setFunctionSignature(functionMetaData.getFunctionSignature());
                tagLibraryInfo.addFunctionInfo(functionInfo);
            }
        }

        if (jarPath == null && relativeLocation == null) {
            context.addJspTagLibrary(tagLibraryInfo);
        } else if (jarPath == null) {
            tagLibraryInfo.setLocation("");
            tagLibraryInfo.setPath(relativeLocation);
            tagLibraries.add(tagLibraryInfo);
            context.addJspTagLibrary(tagLibraryInfo);
            context.addJspTagLibrary(relativeLocation, tagLibraryInfo);
        } else {
            tagLibraryInfo.setLocation(relativeLocation);
            tagLibraryInfo.setPath(jarPath);
            tagLibraries.add(tagLibraryInfo);
            context.addJspTagLibrary(tagLibraryInfo);
            if (jarPath.equals("META-INF/taglib.tld")) {
                context.addJspTagLibrary(relativeLocation, tagLibraryInfo);
            }
        }
    }

    @Override
    public void applicationServletContainerInitializerConfig() {
        // Do nothing here
    }

    @Override
    protected void createFragmentsOrder() {
        // Do nothing here
    }

    @Override
    protected void applicationExtraDescriptorsConfig() {
        // Do nothing here
    }

    protected void resolveAnnotations(JBossAnnotationsMetaData annotations) {
        if (annotations != null) {
            for (AnnotationMetaData annotation : annotations) {
                String className = annotation.getClassName();
                Container[] wrappers = context.findChildren();
                for (int i = 0; i < wrappers.length; i++) {
                    Wrapper wrapper = (Wrapper) wrappers[i];
                    if (className.equals(wrapper.getServletClass())) {

                        // Merge @RunAs
                        if (annotation.getRunAs() != null && wrapper.getRunAs() == null) {
                            wrapper.setRunAs(annotation.getRunAs().getRoleName());
                        }
                        // Merge @MultipartConfig
                        if (annotation.getMultipartConfig() != null && wrapper.getMultipartConfig() == null) {
                            MultipartConfigMetaData multipartConfigMetaData = annotation.getMultipartConfig();
                            Multipart multipartConfig = new Multipart();
                            multipartConfig.setLocation(multipartConfigMetaData.getLocation());
                            multipartConfig.setMaxRequestSize(multipartConfigMetaData.getMaxRequestSize());
                            multipartConfig.setMaxFileSize(multipartConfigMetaData.getMaxFileSize());
                            multipartConfig.setFileSizeThreshold(multipartConfigMetaData.getFileSizeThreshold());
                            wrapper.setMultipartConfig(multipartConfig);
                        }
                        // Merge @ServletSecurity
                        if (annotation.getServletSecurity() != null && wrapper.getServletSecurity() == null) {
                            ServletSecurityMetaData servletSecurityAnnotation = annotation.getServletSecurity();
                            Collection<HttpMethodConstraintElement> methodConstraints = null;

                            EmptyRoleSemantic emptyRoleSemantic = EmptyRoleSemantic.PERMIT;
                            if (servletSecurityAnnotation.getEmptyRoleSemantic() != null) {
                                emptyRoleSemantic = EmptyRoleSemantic.valueOf(servletSecurityAnnotation.getEmptyRoleSemantic()
                                        .toString());
                            }
                            TransportGuarantee transportGuarantee = TransportGuarantee.NONE;
                            if (servletSecurityAnnotation.getTransportGuarantee() != null) {
                                transportGuarantee = TransportGuarantee.valueOf(servletSecurityAnnotation
                                        .getTransportGuarantee().toString());
                            }
                            String[] roleNames = servletSecurityAnnotation.getRolesAllowed().toArray(new String[0]);
                            HttpConstraintElement constraint = new HttpConstraintElement(emptyRoleSemantic, transportGuarantee,
                                    roleNames);

                            if (servletSecurityAnnotation.getHttpMethodConstraints() != null) {
                                methodConstraints = new HashSet<HttpMethodConstraintElement>();
                                for (HttpMethodConstraintMetaData annotationMethodConstraint : servletSecurityAnnotation
                                        .getHttpMethodConstraints()) {
                                    emptyRoleSemantic = EmptyRoleSemantic.PERMIT;
                                    if (annotationMethodConstraint.getEmptyRoleSemantic() != null) {
                                        emptyRoleSemantic = EmptyRoleSemantic.valueOf(annotationMethodConstraint
                                                .getEmptyRoleSemantic().toString());
                                    }
                                    transportGuarantee = TransportGuarantee.NONE;
                                    if (annotationMethodConstraint.getTransportGuarantee() != null) {
                                        transportGuarantee = TransportGuarantee.valueOf(annotationMethodConstraint
                                                .getTransportGuarantee().toString());
                                    }
                                    roleNames = annotationMethodConstraint.getRolesAllowed().toArray(new String[0]);
                                    HttpConstraintElement constraint2 = new HttpConstraintElement(emptyRoleSemantic,
                                            transportGuarantee, roleNames);
                                    HttpMethodConstraintElement methodConstraint = new HttpMethodConstraintElement(
                                            annotationMethodConstraint.getMethod(), constraint2);
                                    methodConstraints.add(methodConstraint);
                                }
                            }

                            ServletSecurityElement servletSecurity = new ServletSecurityElement(constraint, methodConstraints);
                            wrapper.setServletSecurity(servletSecurity);
                        }

                    }
                }
            }
        }
    }

    @Override
    protected void completeConfig() {
        final WarMetaData warMetaData = deploymentUnitContext.getAttachment(WarMetaData.ATTACHMENT_KEY);
        JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();

        // Process Servlet API related annotations that were dependent on
        // Servlet declarations
        if (ok && (metaData != null)) {
            // Resolve type specific annotations to their corresponding Servlet
            // components
            metaData.resolveAnnotations();
            // Same process for Catalina
            resolveAnnotations(metaData.getAnnotations());
        }

        if (ok) {
            resolveServletSecurity();
        }

        if (ok) {
            validateSecurityRoles();
        }

        if (ok && (metaData != null)) {
            // Resolve run as
            metaData.resolveRunAs();
        }

        // Configure an authenticator if we need one
        if (ok) {
            authenticatorConfig();
        }

        // Find and configure overlays
        if (ok) {
            Set<VirtualFile> overlays = warMetaData.getOverlays();
            if (overlays != null) {
                if (context.getResources() instanceof ProxyDirContext) {
                    ProxyDirContext resources = (ProxyDirContext) context.getResources();
                    for (VirtualFile overlay : overlays) {
                        VFSDirContext dirContext = new VFSDirContext();
                        dirContext.setVirtualFile(overlay);
                        resources.addOverlay(dirContext);
                    }
                } else if (overlays.size() > 0) {
                    // Error, overlays need a ProxyDirContext to compose results
                    WebLogger.WEB_LOGGER.noOverlay(context.getName());
                    ok = false;
                }
            }
        }

        // Add other overlays, if any
        if (ok) {
            for (String overlay : overlays) {
                if (context.getResources() instanceof ProxyDirContext) {
                    ProxyDirContext resources = (ProxyDirContext) context.getResources();
                    FileDirContext dirContext = new FileDirContext();
                    dirContext.setDocBase(overlay);
                    resources.addOverlay(dirContext);
                }
            }
        }

        // Make our application unavailable if problems were encountered
        if (!ok) {
            WebLogger.WEB_LOGGER.unavailable(context.getName());
            context.setConfigured(false);
        }

    }

    Injector<DistributedCacheManagerFactory> getDistributedCacheManagerFactoryInjector() {
        return this.factory;
    }
}

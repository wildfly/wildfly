/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import io.undertow.Handlers;
import io.undertow.jsp.JspFileHandler;
import io.undertow.jsp.JspServletBuilder;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.ClassIntrospecter;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.HttpMethodSecurityInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ServletSecurityInfo;
import io.undertow.servlet.api.ServletSessionConfig;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.api.ThreadSetupAction;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.handlers.ServletPathMatches;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import org.apache.jasper.deploy.FunctionInfo;
import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagAttributeInfo;
import org.apache.jasper.deploy.TagFileInfo;
import org.apache.jasper.deploy.TagInfo;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.jasper.deploy.TagLibraryValidatorInfo;
import org.apache.jasper.deploy.TagVariableInfo;
import org.apache.jasper.servlet.JspServlet;
import org.jboss.annotation.javaee.Icon;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.version.Version;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.CookieConfigMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.EmptyRoleSemanticType;
import org.jboss.metadata.web.spec.ErrorPageMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.HttpMethodConstraintMetaData;
import org.jboss.metadata.web.spec.JspConfigMetaData;
import org.jboss.metadata.web.spec.JspPropertyGroupMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LocaleEncodingMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.SessionTrackingModeType;
import org.jboss.metadata.web.spec.TagFileMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.metadata.web.spec.TransportGuaranteeType;
import org.jboss.metadata.web.spec.VariableMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.auth.login.JASPIAuthenticationInfo;
import org.jboss.security.authorization.config.AuthorizationModuleEntry;
import org.jboss.security.authorization.modules.JACCAuthorizationModule;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.security.config.AuthorizationInfo;
import org.jboss.security.config.SecurityConfiguration;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.JSPConfig;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.SessionCookieConfig;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.security.AuditNotificationReceiver;
import org.wildfly.extension.undertow.security.JAASIdentityManagerImpl;
import org.wildfly.extension.undertow.security.JbossAuthorizationManager;
import org.wildfly.extension.undertow.security.RunAsLifecycleInterceptor;
import org.wildfly.extension.undertow.security.SecurityContextAssociationHandler;
import org.wildfly.extension.undertow.security.SecurityContextThreadSetupAction;
import org.wildfly.extension.undertow.security.jacc.JACCAuthorizationManager;
import org.wildfly.extension.undertow.security.jacc.JACCContextIdHandler;
import org.wildfly.extension.undertow.security.jaspi.JASPIAuthenticationMechanism;
import org.wildfly.extension.undertow.security.jaspi.JASPICSecurityContextFactory;
import org.wildfly.extension.undertow.session.CodecSessionConfigWrapper;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.xnio.IoUtils;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.AUTHENTICATE;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.DENY;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.PERMIT;

/**
 * Service that builds up the undertow metadata.
 *
 * @author Stuart Douglas
 */
public class UndertowDeploymentInfoService implements Service<DeploymentInfo> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("UndertowDeploymentInfoService");

    private static final String TEMP_DIR = "jboss.server.temp.dir";
    public static final String DEFAULT_SERVLET_NAME = "default";
    public static final String OLD_URI_PREFIX = "http://java.sun.com";
    public static final String NEW_URI_PREFIX = "http://xmlns.jcp.org";

    private DeploymentInfo deploymentInfo;

    private final JBossWebMetaData mergedMetaData;
    private final String deploymentName;
    private final TldsMetaData tldsMetaData;
    private final List<TldMetaData> sharedTlds;
    private final Module module;
    private final ScisMetaData scisMetaData;
    private final VirtualFile deploymentRoot;
    private final String jaccContextId;
    private final String securityDomain;
    private final List<ServletContextAttribute> attributes;
    private final String contextPath;
    private final List<SetupAction> setupActions;
    private final Set<VirtualFile> overlays;
    private final List<ExpressionFactoryWrapper> expressionFactoryWrappers;
    private final List<PredicatedHandler> predicatedHandlers;
    private final List<HandlerWrapper> initialHandlerChainWrappers;
    private final List<HandlerWrapper> innerHandlerChainWrappers;
    private final List<HandlerWrapper> outerHandlerChainWrappers;
    private final List<ThreadSetupAction> threadSetupActions;
    private final List<ServletExtension> servletExtensions;
    private final SharedSessionManagerConfig sharedSessionManagerConfig;
    private final boolean explodedDeployment;

    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private final InjectedValue<SessionManagerFactory> sessionManagerFactory = new InjectedValue<>();
    private final InjectedValue<SessionIdentifierCodec> sessionIdentifierCodec = new InjectedValue<>();
    private final InjectedValue<SecurityDomainContext> securityDomainContextValue = new InjectedValue<SecurityDomainContext>();
    private final InjectedValue<ServletContainerService> container = new InjectedValue<>();
    private final InjectedValue<PathManager> pathManagerInjector = new InjectedValue<PathManager>();
    private final InjectedValue<ComponentRegistry> componentRegistryInjectedValue = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final Map<String, InjectedValue<Executor>> executorsByName = new HashMap<String, InjectedValue<Executor>>();

    private UndertowDeploymentInfoService(final JBossWebMetaData mergedMetaData, final String deploymentName, final TldsMetaData tldsMetaData, final List<TldMetaData> sharedTlds, final Module module, final ScisMetaData scisMetaData, final VirtualFile deploymentRoot, final String jaccContextId, final String securityDomain, final List<ServletContextAttribute> attributes, final String contextPath, final List<SetupAction> setupActions, final Set<VirtualFile> overlays, final List<ExpressionFactoryWrapper> expressionFactoryWrappers, List<PredicatedHandler> predicatedHandlers, List<HandlerWrapper> initialHandlerChainWrappers, List<HandlerWrapper> innerHandlerChainWrappers, List<HandlerWrapper> outerHandlerChainWrappers, List<ThreadSetupAction> threadSetupActions, boolean explodedDeployment, List<ServletExtension> servletExtensions, SharedSessionManagerConfig sharedSessionManagerConfig) {
        this.mergedMetaData = mergedMetaData;
        this.deploymentName = deploymentName;
        this.tldsMetaData = tldsMetaData;
        this.sharedTlds = sharedTlds;
        this.module = module;
        this.scisMetaData = scisMetaData;
        this.deploymentRoot = deploymentRoot;
        this.jaccContextId = jaccContextId;
        this.securityDomain = securityDomain;
        this.attributes = attributes;
        this.contextPath = contextPath;
        this.setupActions = setupActions;
        this.overlays = overlays;
        this.expressionFactoryWrappers = expressionFactoryWrappers;
        this.predicatedHandlers = predicatedHandlers;
        this.initialHandlerChainWrappers = initialHandlerChainWrappers;
        this.innerHandlerChainWrappers = innerHandlerChainWrappers;
        this.outerHandlerChainWrappers = outerHandlerChainWrappers;
        this.threadSetupActions = threadSetupActions;
        this.explodedDeployment = explodedDeployment;
        this.servletExtensions = servletExtensions;
        this.sharedSessionManagerConfig = sharedSessionManagerConfig;
    }

    @Override
    public synchronized void start(final StartContext startContext) throws StartException {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(module.getClassLoader());
            DeploymentInfo deploymentInfo = createServletConfig();

            handleDistributable(deploymentInfo);
            handleIdentityManager(deploymentInfo);
            handleJASPIMechanism(deploymentInfo);
            handleJACCAuthorization(deploymentInfo);
            handleAdditionalAuthenticationMechanisms(deploymentInfo);

            if(mergedMetaData.isUseJBossAuthorization()) {
                deploymentInfo.setAuthorizationManager(new JbossAuthorizationManager(deploymentInfo.getAuthorizationManager()));
            }

            SessionConfigMetaData sessionConfig = mergedMetaData.getSessionConfig();
            if(sharedSessionManagerConfig != null && sharedSessionManagerConfig.getSessionConfig() != null) {
                sessionConfig = sharedSessionManagerConfig.getSessionConfig();
            }
            ServletSessionConfig config = null;
            //default session config
            SessionCookieConfig defaultSessionConfig = container.getValue().getSessionCookieConfig();
            if (defaultSessionConfig != null) {
                config = new ServletSessionConfig();
                if (defaultSessionConfig.getName() != null) {
                    config.setName(defaultSessionConfig.getName());
                }
                if (defaultSessionConfig.getDomain() != null) {
                    config.setDomain(defaultSessionConfig.getDomain());
                }
                if (defaultSessionConfig.getHttpOnly() != null) {
                    config.setHttpOnly(defaultSessionConfig.getHttpOnly());
                }
                if (defaultSessionConfig.getSecure() != null) {
                    config.setSecure(defaultSessionConfig.getSecure());
                }
                if (defaultSessionConfig.getMaxAge() != null) {
                    config.setMaxAge(defaultSessionConfig.getMaxAge());
                }
                if (defaultSessionConfig.getComment() != null) {
                    config.setComment(defaultSessionConfig.getComment());
                }
            }

            boolean sessionTimeoutSet = false;
            if (sessionConfig != null) {
                if (sessionConfig.getSessionTimeoutSet()) {
                    deploymentInfo.setDefaultSessionTimeout(sessionConfig.getSessionTimeout() * 60);
                    sessionTimeoutSet = true;
                }
                CookieConfigMetaData cookieConfig = sessionConfig.getCookieConfig();
                if (config == null) {
                    config = new ServletSessionConfig();
                }
                if (cookieConfig != null) {
                    if (cookieConfig.getName() != null) {
                        config.setName(cookieConfig.getName());
                    }
                    if (cookieConfig.getDomain() != null) {
                        config.setDomain(cookieConfig.getDomain());
                    }
                    if (cookieConfig.getComment() != null) {
                        config.setComment(cookieConfig.getComment());
                    }
                    config.setSecure(cookieConfig.getSecure());
                    config.setPath(cookieConfig.getPath());
                    config.setMaxAge(cookieConfig.getMaxAge());
                    config.setHttpOnly(cookieConfig.getHttpOnly());
                }
                List<SessionTrackingModeType> modes = sessionConfig.getSessionTrackingModes();
                if (modes != null && !modes.isEmpty()) {
                    final Set<SessionTrackingMode> trackingModes = new HashSet<>();
                    for (SessionTrackingModeType mode : modes) {
                        switch (mode) {
                            case COOKIE:
                                trackingModes.add(SessionTrackingMode.COOKIE);
                                break;
                            case SSL:
                                trackingModes.add(SessionTrackingMode.SSL);
                                break;
                            case URL:
                                trackingModes.add(SessionTrackingMode.URL);
                                break;
                        }
                    }
                    config.setSessionTrackingModes(trackingModes);
                }
            }
            if(!sessionTimeoutSet) {
                deploymentInfo.setDefaultSessionTimeout(container.getValue().getDefaultSessionTimeout() * 60);
            }
            if (config != null) {
                deploymentInfo.setServletSessionConfig(config);
            }

            for (final SetupAction action : setupActions) {
                deploymentInfo.addThreadSetupAction(new UndertowThreadSetupAction(action));
            }

            if (initialHandlerChainWrappers != null) {
                for (HandlerWrapper handlerWrapper : initialHandlerChainWrappers) {
                    deploymentInfo.addInitialHandlerChainWrapper(handlerWrapper);
                }
            }

            if (innerHandlerChainWrappers != null) {
                for (HandlerWrapper handlerWrapper : innerHandlerChainWrappers) {
                    deploymentInfo.addInnerHandlerChainWrapper(handlerWrapper);
                }
            }

            if (outerHandlerChainWrappers != null) {
                for (HandlerWrapper handlerWrapper : outerHandlerChainWrappers) {
                    deploymentInfo.addOuterHandlerChainWrapper(handlerWrapper);
                }
            }

            if (threadSetupActions != null) {
                for (ThreadSetupAction threadSetupAction : threadSetupActions) {
                    deploymentInfo.addThreadSetupAction(threadSetupAction);
                }
            }
            deploymentInfo.setServerName("WildFly "+ Version.AS_VERSION);
            if (undertowService.getValue().statisticsEnabled()){
                deploymentInfo.setMetricsCollector(new UndertowMetricsCollector());
            }
            this.deploymentInfo = deploymentInfo;
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }

    }

    @Override
    public synchronized void stop(final StopContext stopContext) {
        IoUtils.safeClose(this.deploymentInfo.getResourceManager());
        this.deploymentInfo.setConfidentialPortManager(null);
        this.deploymentInfo = null;
    }

    @Override
    public synchronized DeploymentInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentInfo;
    }

    /**
     * <p>Adds to the deployment the {@link JASPIAuthenticationMechanism}, if necessary. The handler will be added if the security domain
     * is configured with JASPI authentication.</p>
     *
     * @param deploymentInfo
     */
    private void handleJASPIMechanism(final DeploymentInfo deploymentInfo) {
        ApplicationPolicy applicationPolicy = SecurityConfiguration.getApplicationPolicy(this.securityDomain);

        if (applicationPolicy != null && JASPIAuthenticationInfo.class.isInstance(applicationPolicy.getAuthenticationInfo())) {
            String authMethod = null;
            LoginConfig loginConfig = deploymentInfo.getLoginConfig();
            if (loginConfig != null && loginConfig.getAuthMethods().size() > 0)
                authMethod = loginConfig.getAuthMethods().get(0).getName();

            deploymentInfo.setJaspiAuthenticationMechanism(new JASPIAuthenticationMechanism(this.securityDomain, authMethod));
            deploymentInfo.setSecurityContextFactory(new JASPICSecurityContextFactory(this.securityDomain));
        }
    }

    /**
     * <p>
     * Sets the {@link JACCAuthorizationManager} in the specified {@link DeploymentInfo} if the webapp security domain
     * has defined a JACC authorization module.
     * </p>
     *
     * @param deploymentInfo the {@link DeploymentInfo} instance.
     */
    private void handleJACCAuthorization(final DeploymentInfo deploymentInfo) {

        // TODO make the authorization manager implementation configurable in Undertow or jboss-web.xml
        ApplicationPolicy applicationPolicy = SecurityConfiguration.getApplicationPolicy(this.securityDomain);
        if (applicationPolicy != null) {
            AuthorizationInfo authzInfo = applicationPolicy.getAuthorizationInfo();
            if (authzInfo != null) {
                for (AuthorizationModuleEntry entry : authzInfo.getModuleEntries()) {
                    if (JACCAuthorizationModule.class.getName().equals(entry.getPolicyModuleName())) {
                        deploymentInfo.setAuthorizationManager(new JACCAuthorizationManager());
                        break;
                    }
                }
            }
        }
    }

    private void handleAdditionalAuthenticationMechanisms(final DeploymentInfo deploymentInfo){
        for (Map.Entry<String,AuthenticationMechanism> am: host.getValue().getAdditionalAuthenticationMechanisms().entrySet()){
            deploymentInfo.addFirstAuthenticationMechanism(am.getKey(), am.getValue());
        }
    }

    private void handleIdentityManager(final DeploymentInfo deploymentInfo) {

        SecurityDomainContext sdc = securityDomainContextValue.getValue();
        deploymentInfo.setIdentityManager(new JAASIdentityManagerImpl(sdc));
        AuditManager auditManager = sdc.getAuditManager();
        if (auditManager != null && !mergedMetaData.isDisableAudit()) {
            deploymentInfo.addNotificationReceiver(new AuditNotificationReceiver(auditManager));
        }
        deploymentInfo.setConfidentialPortManager(getConfidentialPortManager());
    }

    private ConfidentialPortManager getConfidentialPortManager() {
        return new ConfidentialPortManager() {

            @Override
            public int getConfidentialPort(HttpServerExchange exchange) {
                int port = exchange.getConnection().getLocalAddress(InetSocketAddress.class).getPort();
                return host.getValue().getServer().getValue().lookupSecurePort(port);
            }
        };
    }

    private void handleDistributable(final DeploymentInfo deploymentInfo) {
        SessionManagerFactory managerFactory = this.sessionManagerFactory.getOptionalValue();
        if (managerFactory != null) {
            deploymentInfo.setSessionManagerFactory(managerFactory);
        }
        SessionIdentifierCodec codec = this.sessionIdentifierCodec.getOptionalValue();
        if (codec != null) {
            deploymentInfo.setSessionConfigWrapper(new CodecSessionConfigWrapper(codec));
        }
    }

    /*
    This is to address WFLY-1894 but should probably be moved to some other place.
     */
    private String resolveContextPath() {
        if (deploymentName.equals(host.getValue().getDefaultWebModule())) {
            return "/";
        } else {
            return contextPath;
        }
    }

    private DeploymentInfo createServletConfig() throws StartException {
        final ComponentRegistry componentRegistry = componentRegistryInjectedValue.getValue();
        try {
            if (!mergedMetaData.isMetadataComplete()) {
                mergedMetaData.resolveAnnotations();
            }
            final DeploymentInfo d = new DeploymentInfo();
            d.setContextPath(resolveContextPath());
            if (mergedMetaData.getDescriptionGroup() != null) {
                d.setDisplayName(mergedMetaData.getDescriptionGroup().getDisplayName());
            }
            d.setDeploymentName(deploymentName);
            d.setHostName(host.getValue().getName());
            final ServletContainerService servletContainer = container.getValue();
            try {
                //TODO: make the caching limits configurable
                ResourceManager resourceManager = new ServletResourceManager(deploymentRoot, overlays, explodedDeployment);

                resourceManager = new CachingResourceManager(100, 10 * 1024 * 1024, servletContainer.getBufferCache(), resourceManager, explodedDeployment ? 2000 : -1);
                d.setResourceManager(resourceManager);
            } catch (IOException e) {
                throw new StartException(e);
            }

            File tempFile = new File(pathManagerInjector.getValue().getPathEntry(TEMP_DIR).resolvePath(), deploymentName);
            tempFile.mkdirs();
            d.setTempDir(tempFile);

            d.setClassLoader(module.getClassLoader());
            final String servletVersion = mergedMetaData.getServletVersion();
            if (servletVersion != null) {
                d.setMajorVersion(Integer.parseInt(servletVersion.charAt(0) + ""));
                d.setMinorVersion(Integer.parseInt(servletVersion.charAt(2) + ""));
            } else {
                d.setMajorVersion(3);
                d.setMinorVersion(1);
            }

            //in most cases flush just hurts performance for no good reason
            d.setIgnoreFlush(servletContainer.isIgnoreFlush());

            //controlls initizalization of filters on start of application
            d.setEagerFilterInit(servletContainer.isEagerFilterInit());

            d.setAllowNonStandardWrappers(servletContainer.isAllowNonStandardWrappers());
            d.setServletStackTraces(servletContainer.getStackTraces());

            if (servletContainer.getSessionPersistenceManager() != null) {
                d.setSessionPersistenceManager(servletContainer.getSessionPersistenceManager());
            }

            //for 2.2 apps we do not require a leading / in path mappings
            boolean is22OrOlder;
            if (d.getMajorVersion() == 1) {
                is22OrOlder = true;
            } else if (d.getMajorVersion() == 2) {
                is22OrOlder = d.getMinorVersion() < 3;
            } else {
                is22OrOlder = false;
            }
            JSPConfig jspConfig = servletContainer.getJspConfig();
            final Set<String> seenMappings = new HashSet<>();

            HashMap<String, TagLibraryInfo> tldInfo = createTldsInfo(tldsMetaData, sharedTlds);

            //default JSP servlet
            final ServletInfo jspServlet = jspConfig != null ? jspConfig.createJSPServletInfo() : null;
            if (jspServlet != null) { //this would be null if jsp support is disabled
                HashMap<String, JspPropertyGroup> propertyGroups = createJspConfig(mergedMetaData);
                JspServletBuilder.setupDeployment(d, propertyGroups, tldInfo, new UndertowJSPInstanceManager(new WebInjectionContainer(module.getClassLoader(), componentRegistryInjectedValue.getValue())));

                if (mergedMetaData.getJspConfig() != null) {
                    d.setJspConfigDescriptor(new JspConfigDescriptorImpl(tldInfo.values(), propertyGroups.values()));
                }

                d.addServlet(jspServlet);

                final Set<String> jspPropertyGroupMappings = propertyGroups.keySet();
                for (final String mapping : jspPropertyGroupMappings) {
                    jspServlet.addMapping(mapping);
                }
                seenMappings.addAll(jspPropertyGroupMappings);
                //setup JSP expression factory wrapper
                if (!expressionFactoryWrappers.isEmpty()) {
                    d.addListener(new ListenerInfo(JspInitializationListener.class));
                    d.addServletContextAttribute(JspInitializationListener.CONTEXT_KEY, expressionFactoryWrappers);
                }
            }

            d.setClassIntrospecter(new ComponentClassIntrospector(componentRegistry));

            final Map<String, List<ServletMappingMetaData>> servletMappings = new HashMap<>();

            if (mergedMetaData.getExecutorName() != null) {
                d.setExecutor(executorsByName.get(mergedMetaData.getExecutorName()).getValue());
            }

            if(servletExtensions != null) {
                for(ServletExtension extension : servletExtensions) {
                    d.addServletExtension(extension);
                }
            }

            if (mergedMetaData.getServletMappings() != null) {
                for (final ServletMappingMetaData mapping : mergedMetaData.getServletMappings()) {
                    List<ServletMappingMetaData> list = servletMappings.get(mapping.getServletName());
                    if (list == null) {
                        servletMappings.put(mapping.getServletName(), list = new ArrayList<>());
                    }
                    list.add(mapping);
                }
            }
            if (jspServlet != null) {
                List<ServletMappingMetaData> list = servletMappings.get(jspServlet.getName());
                if(list != null && ! list.isEmpty()) {
                    for (final ServletMappingMetaData mapping : list) {
                        for(String urlPattern : mapping.getUrlPatterns()) {
                            jspServlet.addMapping(urlPattern);
                        }
                        seenMappings.addAll(mapping.getUrlPatterns());
                    }
                }
            }

            final List<JBossServletMetaData> servlets = new ArrayList<JBossServletMetaData>();
            for (JBossServletMetaData servlet : mergedMetaData.getServlets()) {
                servlets.add(servlet);
            }

            for (final JBossServletMetaData servlet : mergedMetaData.getServlets()) {
                final ServletInfo s;

                if (servlet.getJspFile() != null) {
                    s = new ServletInfo(servlet.getName(), JspServlet.class);
                    s.addHandlerChainWrapper(JspFileHandler.jspFileHandlerWrapper(servlet.getJspFile()));
                } else {
                    if (servlet.getServletClass() == null) {
                        if(DEFAULT_SERVLET_NAME.equals(servlet.getName())) {
                            s = new ServletInfo(servlet.getName(), DefaultServlet.class);
                        } else {
                            throw UndertowLogger.ROOT_LOGGER.servletClassNotDefined(servlet.getServletName());
                        }
                    } else {
                        Class<? extends Servlet> servletClass = (Class<? extends Servlet>) module.getClassLoader().loadClass(servlet.getServletClass());
                        ManagedReferenceFactory creator = componentRegistry.createInstanceFactory(servletClass);
                        if (creator != null) {
                            InstanceFactory<Servlet> factory = createInstanceFactory(creator);
                            s = new ServletInfo(servlet.getName(), servletClass, factory);
                        } else {
                            s = new ServletInfo(servlet.getName(), servletClass);
                        }
                    }
                }
                s.setAsyncSupported(servlet.isAsyncSupported())
                        .setJspFile(servlet.getJspFile())
                        .setEnabled(servlet.isEnabled());
                if (servlet.getRunAs() != null) {
                    s.setRunAs(servlet.getRunAs().getRoleName());
                }
                if (servlet.getLoadOnStartupSet()) {//todo why not cleanup api and just use int everywhere
                    s.setLoadOnStartup(servlet.getLoadOnStartupInt());
                }

                if (servlet.getExecutorName() != null) {
                    s.setExecutor(executorsByName.get(servlet.getExecutorName()).getValue());
                }

                handleServletMappings(is22OrOlder, seenMappings, servletMappings, s);
                if (servlet.getInitParam() != null) {
                    for (ParamValueMetaData initParam : servlet.getInitParam()) {
                        if (!s.getInitParams().containsKey(initParam.getParamName())) {
                            s.addInitParam(initParam.getParamName(), initParam.getParamValue());
                        }
                    }
                }
                if (servlet.getServletSecurity() != null) {
                    ServletSecurityInfo securityInfo = new ServletSecurityInfo();
                    s.setServletSecurityInfo(securityInfo);
                    securityInfo.setEmptyRoleSemantic(servlet.getServletSecurity().getEmptyRoleSemantic() == EmptyRoleSemanticType.DENY ? DENY : PERMIT)
                        .setTransportGuaranteeType(transportGuaranteeType(servlet.getServletSecurity().getTransportGuarantee()))
                        .addRolesAllowed(servlet.getServletSecurity().getRolesAllowed());
                    if (servlet.getServletSecurity().getHttpMethodConstraints() != null) {
                        for (HttpMethodConstraintMetaData method : servlet.getServletSecurity().getHttpMethodConstraints()) {
                        securityInfo.addHttpMethodSecurityInfo(
                                new HttpMethodSecurityInfo()
                                    .setEmptyRoleSemantic(method.getEmptyRoleSemantic() == EmptyRoleSemanticType.DENY ? DENY : PERMIT)
                                    .setTransportGuaranteeType(transportGuaranteeType(method.getTransportGuarantee()))
                                    .addRolesAllowed(method.getRolesAllowed())
                                    .setMethod(method.getMethod()));
                        }
                    }
                }
                if (servlet.getSecurityRoleRefs() != null) {
                    for (final SecurityRoleRefMetaData ref : servlet.getSecurityRoleRefs()) {
                        s.addSecurityRoleRef(ref.getRoleName(), ref.getRoleLink());
                    }
                }

                if (servlet.getMultipartConfig() != null) {
                    MultipartConfigMetaData mp = servlet.getMultipartConfig();
                    s.setMultipartConfig(Servlets.multipartConfig(mp.getLocation(), mp.getMaxFileSize(), mp.getMaxRequestSize(), mp.getFileSizeThreshold()));
                }

                d.addServlet(s);
            }

            //we explicitly add the default servlet, to allow it to be mapped
            if (!mergedMetaData.getServlets().containsKey(ServletPathMatches.DEFAULT_SERVLET_NAME)) {
                ServletInfo defaultServlet = Servlets.servlet(DEFAULT_SERVLET_NAME, DefaultServlet.class);
                handleServletMappings(is22OrOlder, seenMappings, servletMappings, defaultServlet);

                d.addServlet(defaultServlet);
            }

            if (mergedMetaData.getFilters() != null) {
                for (final FilterMetaData filter : mergedMetaData.getFilters()) {
                    Class<? extends Filter> filterClass = (Class<? extends Filter>) module.getClassLoader().loadClass(filter.getFilterClass());
                    ManagedReferenceFactory creator = componentRegistry.createInstanceFactory(filterClass);
                    FilterInfo f;
                    if (creator != null) {
                        InstanceFactory<Filter> instanceFactory = createInstanceFactory(creator);
                        f = new FilterInfo(filter.getName(), filterClass, instanceFactory);
                    } else {
                        f = new FilterInfo(filter.getName(), filterClass);
                    }
                    f.setAsyncSupported(filter.isAsyncSupported());
                    d.addFilter(f);

                    if (filter.getInitParam() != null) {
                        for (ParamValueMetaData initParam : filter.getInitParam()) {
                            f.addInitParam(initParam.getParamName(), initParam.getParamValue());
                        }
                    }
                }
            }
            if (mergedMetaData.getFilterMappings() != null) {
                for (final FilterMappingMetaData mapping : mergedMetaData.getFilterMappings()) {
                    if (mapping.getUrlPatterns() != null) {
                        for (String url : mapping.getUrlPatterns()) {
                            if (is22OrOlder && !url.startsWith("*") && !url.startsWith("/")) {
                                url = "/" + url;
                            }
                            if (mapping.getDispatchers() != null && !mapping.getDispatchers().isEmpty()) {
                                for (DispatcherType dispatcher : mapping.getDispatchers()) {

                                    d.addFilterUrlMapping(mapping.getFilterName(), url, javax.servlet.DispatcherType.valueOf(dispatcher.name()));
                                }
                            } else {
                                d.addFilterUrlMapping(mapping.getFilterName(), url, javax.servlet.DispatcherType.REQUEST);
                            }
                        }
                    }
                    if (mapping.getServletNames() != null) {
                        for (String servletName : mapping.getServletNames()) {
                            if (mapping.getDispatchers() != null && !mapping.getDispatchers().isEmpty()) {
                                for (DispatcherType dispatcher : mapping.getDispatchers()) {
                                    d.addFilterServletNameMapping(mapping.getFilterName(), servletName, javax.servlet.DispatcherType.valueOf(dispatcher.name()));
                                }
                            } else {
                                d.addFilterServletNameMapping(mapping.getFilterName(), servletName, javax.servlet.DispatcherType.REQUEST);
                            }
                        }
                    }
                }
            }

            if (scisMetaData != null && scisMetaData.getHandlesTypes() != null) {
                for (final ServletContainerInitializer sci : scisMetaData.getScis()) {
                    final ImmediateInstanceFactory<ServletContainerInitializer> instanceFactory = new ImmediateInstanceFactory<>(sci);
                    d.addServletContainerInitalizer(new ServletContainerInitializerInfo(sci.getClass(), instanceFactory, scisMetaData.getHandlesTypes().get(sci)));
                }
            }

            if (mergedMetaData.getListeners() != null) {
                for (ListenerMetaData listener : mergedMetaData.getListeners()) {
                    addListener(module.getClassLoader(), componentRegistry, d, listener);
                }

            }
            if (mergedMetaData.getContextParams() != null) {
                for (ParamValueMetaData param : mergedMetaData.getContextParams()) {
                    d.addInitParameter(param.getParamName(), param.getParamValue());
                }
            }

            if (mergedMetaData.getWelcomeFileList() != null &&
                    mergedMetaData.getWelcomeFileList().getWelcomeFiles() != null) {
                List<String> welcomeFiles = mergedMetaData.getWelcomeFileList().getWelcomeFiles();
                for (String file : welcomeFiles) {
                    if (file.startsWith("/")) {
                        d.addWelcomePages(file.substring(1));
                    } else {
                        d.addWelcomePages(file);
                    }
                }
            } else {
                d.addWelcomePages("index.html", "index.htm", "index.jsp");
            }

            if (mergedMetaData.getErrorPages() != null) {
                for (final ErrorPageMetaData page : mergedMetaData.getErrorPages()) {
                    final ErrorPage errorPage;
                    if (page.getExceptionType() != null && !page.getExceptionType().isEmpty()) {
                        errorPage = new ErrorPage(page.getLocation(), (Class<? extends Throwable>) module.getClassLoader().loadClass(page.getExceptionType()));
                    } else if (page.getErrorCode() != null && !page.getErrorCode().isEmpty()) {
                        errorPage = new ErrorPage(page.getLocation(), Integer.parseInt(page.getErrorCode()));
                    } else {
                        errorPage = new ErrorPage(page.getLocation());
                    }
                    d.addErrorPages(errorPage);
                }
            }

            if (mergedMetaData.getMimeMappings() != null) {
                for (final MimeMappingMetaData mapping : mergedMetaData.getMimeMappings()) {
                    d.addMimeMapping(new MimeMapping(mapping.getExtension(), mapping.getMimeType()));
                }
            }

            d.setDenyUncoveredHttpMethods(mergedMetaData.getDenyUncoveredHttpMethods() != null);
            Set<String> securityRoleNames = mergedMetaData.getSecurityRoleNames();
            if (mergedMetaData.getSecurityConstraints() != null) {
                for (SecurityConstraintMetaData constraint : mergedMetaData.getSecurityConstraints()) {
                    SecurityConstraint securityConstraint = new SecurityConstraint()
                            .setTransportGuaranteeType(transportGuaranteeType(constraint.getTransportGuarantee()));

                    List<String> roleNames = constraint.getRoleNames();
                    if (constraint.getAuthConstraint() == null) {
                        // no auth constraint means we permit the empty roles
                        securityConstraint.setEmptyRoleSemantic(PERMIT);
                    } else if (roleNames.size() == 1 && roleNames.contains("*") && securityRoleNames.contains("*")) {
                        // AS7-6932 - Trying to do a * to * mapping which JBossWeb passed through, for Undertow enable
                        // authentication only mode.
                        // TODO - AS7-6933 - Revisit workaround added to allow switching between JBoss Web and Undertow.
                        securityConstraint.setEmptyRoleSemantic(AUTHENTICATE);
                    } else {
                        securityConstraint.addRolesAllowed(roleNames);
                    }

                    if (constraint.getResourceCollections() != null) {
                        for (final WebResourceCollectionMetaData resourceCollection : constraint.getResourceCollections()) {
                            securityConstraint.addWebResourceCollection(new WebResourceCollection()
                                    .addHttpMethods(resourceCollection.getHttpMethods())
                                    .addHttpMethodOmissions(resourceCollection.getHttpMethodOmissions())
                                    .addUrlPatterns(resourceCollection.getUrlPatterns()));
                        }
                    }
                    d.addSecurityConstraint(securityConstraint);
                }
            }
            final LoginConfigMetaData loginConfig = mergedMetaData.getLoginConfig();
            if (loginConfig != null) {
                List<AuthMethodConfig> authMethod = authMethod(loginConfig.getAuthMethod());
                if (loginConfig.getFormLoginConfig() != null) {
                    d.setLoginConfig(new LoginConfig(loginConfig.getRealmName(), loginConfig.getFormLoginConfig().getLoginPage(), loginConfig.getFormLoginConfig().getErrorPage()));
                } else {
                    d.setLoginConfig(new LoginConfig(loginConfig.getRealmName()));
                }
                for (AuthMethodConfig method : authMethod) {
                    d.getLoginConfig().addLastAuthMethod(method);
                }
            }

            d.addSecurityRoles(mergedMetaData.getSecurityRoleNames());


            Map<String, Set<String>> principalVersusRolesMap = mergedMetaData.getPrincipalVersusRolesMap();
            d.addThreadSetupAction(new SecurityContextThreadSetupAction(securityDomain, securityDomainContextValue.getValue(), principalVersusRolesMap));
            d.addInnerHandlerChainWrapper(SecurityContextAssociationHandler.wrapper(mergedMetaData.getRunAsIdentity()));
            d.addOuterHandlerChainWrapper(JACCContextIdHandler.wrapper(jaccContextId));

            d.addLifecycleInterceptor(new RunAsLifecycleInterceptor(mergedMetaData.getRunAsIdentity()));

            if (principalVersusRolesMap != null) {
                for (Map.Entry<String, Set<String>> entry : principalVersusRolesMap.entrySet()) {
                    d.addPrincipalVsRoleMappings(entry.getKey(), entry.getValue());
                }
            }

            // Setup an deployer configured ServletContext attributes
            for (ServletContextAttribute attribute : attributes) {
                d.addServletContextAttribute(attribute.getName(), attribute.getValue());
            }

            if (mergedMetaData.getLocalEncodings() != null &&
                    mergedMetaData.getLocalEncodings().getMappings() != null) {
                for (LocaleEncodingMetaData locale : mergedMetaData.getLocalEncodings().getMappings()) {
                    d.addLocaleCharsetMapping(locale.getLocale(), locale.getEncoding());
                }
            }

            if (predicatedHandlers != null && !predicatedHandlers.isEmpty()) {
                d.addInitialHandlerChainWrapper(new HandlerWrapper() {
                    @Override
                    public HttpHandler wrap(HttpHandler handler) {
                        if (predicatedHandlers.size() == 1) {
                            PredicatedHandler ph = predicatedHandlers.get(0);
                            return Handlers.predicate(ph.getPredicate(), ph.getHandler().wrap(handler), handler);
                        } else {
                            return Handlers.predicates(predicatedHandlers, handler);
                        }
                    }
                });
            }

            if (mergedMetaData.getDefaultEncoding()!=null){
                d.setDefaultEncoding(mergedMetaData.getDefaultEncoding());
            }else if (servletContainer.getDefaultEncoding()!=null){
                d.setDefaultEncoding(servletContainer.getDefaultEncoding());
            }

            return d;
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        }
    }

    private void handleServletMappings(boolean is22OrOlder, Set<String> seenMappings, Map<String, List<ServletMappingMetaData>> servletMappings, ServletInfo s) {
        List<ServletMappingMetaData> mappings = servletMappings.get(s.getName());
        if (mappings != null) {
            for (ServletMappingMetaData mapping : mappings) {
                for (String pattern : mapping.getUrlPatterns()) {
                    if (is22OrOlder && !pattern.startsWith("*") && !pattern.startsWith("/")) {
                        pattern = "/" + pattern;
                    }
                    if (!seenMappings.contains(pattern)) {
                        s.addMapping(pattern);
                        seenMappings.add(pattern);
                    }
                }
            }
        }
    }

    /**
     * Convert the authentication method name from the format specified in the web.xml to the format used by
     * {@link javax.servlet.http.HttpServletRequest}.
     * <p/>
     * If the auth method is not recognised then it is returned as-is.
     *
     * @return The converted auth method.
     * @throws NullPointerException if no configuredMethod is supplied.
     */
    private static List<AuthMethodConfig> authMethod(String configuredMethod) {
        if (configuredMethod == null) {
            return Collections.singletonList(new AuthMethodConfig(HttpServletRequest.BASIC_AUTH));
        }
        return AuthMethodParser.parse(configuredMethod, Collections.singletonMap("CLIENT-CERT", HttpServletRequest.CLIENT_CERT_AUTH));
    }

    private static io.undertow.servlet.api.TransportGuaranteeType transportGuaranteeType(final TransportGuaranteeType type) {
        if (type == null) {
            return io.undertow.servlet.api.TransportGuaranteeType.NONE;
        }
        switch (type) {
            case CONFIDENTIAL:
                return io.undertow.servlet.api.TransportGuaranteeType.CONFIDENTIAL;
            case INTEGRAL:
                return io.undertow.servlet.api.TransportGuaranteeType.INTEGRAL;
            case NONE:
                return io.undertow.servlet.api.TransportGuaranteeType.NONE;
        }
        throw new RuntimeException("UNREACHABLE");
    }

    private static HashMap<String, JspPropertyGroup> createJspConfig(JBossWebMetaData metaData) {
        final HashMap<String, JspPropertyGroup> result = new HashMap<>();
        // JSP Config
        JspConfigMetaData config = metaData.getJspConfig();
        if (config != null) {
            // JSP Property groups
            List<JspPropertyGroupMetaData> groups = config.getPropertyGroups();
            if (groups != null) {
                for (JspPropertyGroupMetaData group : groups) {
                    org.apache.jasper.deploy.JspPropertyGroup jspPropertyGroup = new org.apache.jasper.deploy.JspPropertyGroup();
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
                    for (String pattern : jspPropertyGroup.getUrlPatterns()) {
                        // Split off the groups to individual mappings
                        result.put(pattern, jspPropertyGroup);
                    }
                }
            }
        }

        //it looks like jasper needs these in order of least specified to most specific
        final LinkedHashMap<String, JspPropertyGroup> ret = new LinkedHashMap<>();
        final ArrayList<String> paths = new ArrayList<>(result.keySet());
        Collections.sort(paths, new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.length() - o2.length();
            }
        });
        for (String path : paths) {
            ret.put(path, result.get(path));
        }
        return ret;
    }

    private static HashMap<String, TagLibraryInfo> createTldsInfo(final TldsMetaData tldsMetaData, List<TldMetaData> sharedTlds) throws ClassNotFoundException {

        final HashMap<String, TagLibraryInfo> ret = new HashMap<>();
        if (tldsMetaData != null) {
            if (tldsMetaData.getTlds() != null) {
                for (Map.Entry<String, TldMetaData> tld : tldsMetaData.getTlds().entrySet()) {
                    createTldInfo(tld.getKey(), tld.getValue(), ret);
                }
            }
            if (sharedTlds != null) {
                for (TldMetaData metaData : sharedTlds) {

                    createTldInfo(null, metaData, ret);
                }
            }
        }

        //we also register them under the new namespaces
        for(String k : new HashSet<>(ret.keySet())) {
            if(k.startsWith(OLD_URI_PREFIX)) {
                String newUri = k.replace(OLD_URI_PREFIX, NEW_URI_PREFIX);
                ret.put(newUri, ret.get(k));
            }
        }

        return ret;
    }

    private static TagLibraryInfo createTldInfo(final String location, final TldMetaData tldMetaData, final HashMap<String, TagLibraryInfo> ret) throws ClassNotFoundException {
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
        if (tldMetaData.getJspVersion() == null) {
            tagLibraryInfo.setJspversion(tldMetaData.getVersion());
        } else {
            tagLibraryInfo.setJspversion(tldMetaData.getJspVersion());
        }
        tagLibraryInfo.setShortname(tldMetaData.getShortName());
        tagLibraryInfo.setUri(tldMetaData.getUri());
        if (tldMetaData.getDescriptionGroup() != null) {
            tagLibraryInfo.setInfo(tldMetaData.getDescriptionGroup().getDescription());
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
                if (tagMetaData.getBodyContent() != null) {
                    tagInfo.setBodyContent(tagMetaData.getBodyContent().toString());
                }
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
                        if (variableMetaData.getScope() != null) {
                            tagVariableInfo.setScope(variableMetaData.getScope().toString());
                        }
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
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
        } else if (jarPath == null) {
            tagLibraryInfo.setLocation("");
            tagLibraryInfo.setPath(relativeLocation);
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
            ret.put(relativeLocation, tagLibraryInfo);
        } else {
            tagLibraryInfo.setLocation(relativeLocation);
            tagLibraryInfo.setPath(jarPath);
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
            if (jarPath.equals("META-INF/taglib.tld")) {
                ret.put(relativeLocation, tagLibraryInfo);
            }
        }
        return tagLibraryInfo;
    }

    private static void addListener(final ClassLoader classLoader, final ComponentRegistry components, final DeploymentInfo d, final ListenerMetaData listener) throws ClassNotFoundException {

        ListenerInfo l;
        final Class<? extends EventListener> listenerClass = (Class<? extends EventListener>) classLoader.loadClass(listener.getListenerClass());
        ManagedReferenceFactory creator = components.createInstanceFactory(listenerClass);
        if (creator != null) {
            InstanceFactory<EventListener> factory = createInstanceFactory(creator);
            l = new ListenerInfo(listenerClass, factory);
        } else {
            l = new ListenerInfo(listenerClass);
        }
        d.addListener(l);
    }

    private static <T> InstanceFactory<T> createInstanceFactory(final ManagedReferenceFactory creator) {
        return new InstanceFactory<T>() {
            @Override
            public InstanceHandle<T> createInstance() throws InstantiationException {
                final ManagedReference instance = creator.getReference();
                return new InstanceHandle<T>() {
                    @Override
                    public T getInstance() {
                        return (T) instance.getInstance();
                    }

                    @Override
                    public void release() {
                        instance.release();
                    }
                };
            }
        };
    }

    public void addInjectedExecutor(final String name, final InjectedValue<Executor> injected) {
        executorsByName.put(name, injected);
    }

    public InjectedValue<ServletContainerService> getContainer() {
        return container;
    }

    public InjectedValue<SecurityDomainContext> getSecurityDomainContextValue() {
        return securityDomainContextValue;
    }

    public Injector<SessionManagerFactory> getSessionManagerFactoryInjector() {
        return this.sessionManagerFactory;
    }

    public Injector<SessionIdentifierCodec> getSessionIdentifierCodecInjector() {
        return this.sessionIdentifierCodec;
    }

    public InjectedValue<UndertowService> getUndertowService() {
        return undertowService;
    }

    public InjectedValue<PathManager> getPathManagerInjector() {
        return pathManagerInjector;
    }

    public InjectedValue<ComponentRegistry> getComponentRegistryInjectedValue() {
        return componentRegistryInjectedValue;
    }

    public InjectedValue<Host> getHost() {
        return host;
    }

    private static class ComponentClassIntrospector implements ClassIntrospecter {
        private final ComponentRegistry componentRegistry;

        public ComponentClassIntrospector(final ComponentRegistry componentRegistry) {
            this.componentRegistry = componentRegistry;
        }

        @Override
        public <T> InstanceFactory<T> createInstanceFactory(final Class<T> clazz) throws NoSuchMethodException {
            final ManagedReferenceFactory component = componentRegistry.createInstanceFactory(clazz);
            return new ManagedReferenceInstanceFactory<>(component);
        }
    }

    private static class ManagedReferenceInstanceFactory<T> implements InstanceFactory<T> {
        private final ManagedReferenceFactory component;

        public ManagedReferenceInstanceFactory(final ManagedReferenceFactory component) {
            this.component = component;
        }

        @Override
        public InstanceHandle<T> createInstance() throws InstantiationException {
            final ManagedReference reference = component.getReference();
            return new InstanceHandle<T>() {
                @Override
                public T getInstance() {
                    return (T) reference.getInstance();
                }

                @Override
                public void release() {
                    reference.release();
                }
            };
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JBossWebMetaData mergedMetaData;
        private String deploymentName;
        private TldsMetaData tldsMetaData;
        private List<TldMetaData> sharedTlds;
        private Module module;
        private ScisMetaData scisMetaData;
        private VirtualFile deploymentRoot;
        private String jaccContextId;
        private List<ServletContextAttribute> attributes;
        private String contextPath;
        private String securityDomain;
        private List<SetupAction> setupActions;
        private Set<VirtualFile> overlays;
        private List<ExpressionFactoryWrapper> expressionFactoryWrappers;
        private List<PredicatedHandler> predicatedHandlers;
        private List<HandlerWrapper> initialHandlerChainWrappers;
        private List<HandlerWrapper> innerHandlerChainWrappers;
        private List<HandlerWrapper> outerHandlerChainWrappers;
        private List<ThreadSetupAction> threadSetupActions;
        private List<ServletExtension> servletExtensions;
        private SharedSessionManagerConfig sharedSessionManagerConfig;
        private boolean explodedDeployment;

        Builder setMergedMetaData(final JBossWebMetaData mergedMetaData) {
            this.mergedMetaData = mergedMetaData;
            return this;
        }

        public Builder setDeploymentName(final String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder setTldsMetaData(final TldsMetaData tldsMetaData) {
            this.tldsMetaData = tldsMetaData;
            return this;
        }

        public Builder setSharedTlds(final List<TldMetaData> sharedTlds) {
            this.sharedTlds = sharedTlds;
            return this;
        }

        public Builder setModule(final Module module) {
            this.module = module;
            return this;
        }

        public Builder setScisMetaData(final ScisMetaData scisMetaData) {
            this.scisMetaData = scisMetaData;
            return this;
        }

        public Builder setDeploymentRoot(final VirtualFile deploymentRoot) {
            this.deploymentRoot = deploymentRoot;
            return this;
        }

        public Builder setJaccContextId(final String jaccContextId) {
            this.jaccContextId = jaccContextId;
            return this;
        }

        public Builder setAttributes(final List<ServletContextAttribute> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder setContextPath(final String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public Builder setSetupActions(final List<SetupAction> setupActions) {
            this.setupActions = setupActions;
            return this;
        }

        public Builder setSecurityDomain(final String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public Builder setOverlays(final Set<VirtualFile> overlays) {
            this.overlays = overlays;
            return this;
        }

        public Builder setExpressionFactoryWrappers(final List<ExpressionFactoryWrapper> expressionFactoryWrappers) {
            this.expressionFactoryWrappers = expressionFactoryWrappers;
            return this;
        }

        public Builder setPredicatedHandlers(List<PredicatedHandler> predicatedHandlers) {
            this.predicatedHandlers = predicatedHandlers;
            return this;
        }

        public Builder setInitialHandlerChainWrappers(List<HandlerWrapper> initialHandlerChainWrappers) {
            this.initialHandlerChainWrappers = initialHandlerChainWrappers;
            return this;
        }

        public Builder setInnerHandlerChainWrappers(List<HandlerWrapper> innerHandlerChainWrappers) {
            this.innerHandlerChainWrappers = innerHandlerChainWrappers;
            return this;
        }

        public Builder setOuterHandlerChainWrappers(List<HandlerWrapper> outerHandlerChainWrappers) {
            this.outerHandlerChainWrappers = outerHandlerChainWrappers;
            return this;
        }

        public Builder setThreadSetupActions(List<ThreadSetupAction> threadSetupActions) {
            this.threadSetupActions = threadSetupActions;
            return this;
        }

        public Builder setExplodedDeployment(boolean explodedDeployment) {
            this.explodedDeployment = explodedDeployment;
            return this;
        }

        public List<ServletExtension> getServletExtensions() {
            return servletExtensions;
        }

        public Builder setServletExtensions(List<ServletExtension> servletExtensions) {
            this.servletExtensions = servletExtensions;
            return this;
        }

        public Builder setSharedSessionManagerConfig(SharedSessionManagerConfig sharedSessionManagerConfig) {
            this.sharedSessionManagerConfig = sharedSessionManagerConfig;
            return this;
        }

        public UndertowDeploymentInfoService createUndertowDeploymentInfoService() {
            return new UndertowDeploymentInfoService(mergedMetaData, deploymentName, tldsMetaData, sharedTlds, module, scisMetaData, deploymentRoot, jaccContextId, securityDomain, attributes, contextPath, setupActions, overlays, expressionFactoryWrappers, predicatedHandlers, initialHandlerChainWrappers, innerHandlerChainWrappers, outerHandlerChainWrappers, threadSetupActions, explodedDeployment, servletExtensions, sharedSessionManagerConfig);
        }
    }

    private static class UndertowThreadSetupAction implements ThreadSetupAction {

        private final Handle handle;
        private final SetupAction action;

        public UndertowThreadSetupAction(SetupAction action) {
            this.action = action;
            handle = new Handle() {
                @Override
                public void tearDown() {
                    UndertowThreadSetupAction.this.action.teardown(Collections.<String, Object>emptyMap());
                }
            };
        }

        @Override
        public Handle setup(final HttpServerExchange exchange) {
            action.setup(Collections.<String, Object>emptyMap());
            return handle;
        }
    }
}

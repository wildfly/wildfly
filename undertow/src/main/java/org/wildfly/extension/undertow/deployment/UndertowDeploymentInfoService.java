/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import io.undertow.predicate.Predicate;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.builder.PredicatedHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
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
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.handlers.ServletPathMatches;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.jasper.servlet.JspServlet;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.metadata.javaee.jboss.RunAsIdentityMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.SecurityRoleRefMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.CookieConfigMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.EmptyRoleSemanticType;
import org.jboss.metadata.web.spec.ErrorPageMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
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
import org.jboss.metadata.web.spec.TransportGuaranteeType;
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
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.JSPConfig;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.SessionCookieConfig;
import org.wildfly.extension.undertow.SingleSignOnService;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.ApplicationSecurityDomainDefinition.Registration;
import org.wildfly.extension.undertow.security.AuditNotificationReceiver;
import org.wildfly.extension.undertow.security.JAASIdentityManagerImpl;
import org.wildfly.extension.undertow.security.JbossAuthorizationManager;
import org.wildfly.extension.undertow.security.LogoutNotificationReceiver;
import org.wildfly.extension.undertow.security.RunAsLifecycleInterceptor;
import org.wildfly.extension.undertow.security.SecurityContextAssociationHandler;
import org.wildfly.extension.undertow.security.SecurityContextThreadSetupAction;
import org.wildfly.extension.undertow.security.jacc.JACCAuthorizationManager;
import org.wildfly.extension.undertow.security.jacc.JACCContextIdHandler;
import org.wildfly.extension.undertow.security.jaspi.JASPICAuthenticationMechanism;
import org.wildfly.extension.undertow.security.jaspi.JASPICSecureResponseHandler;
import org.wildfly.extension.undertow.security.jaspi.JASPICSecurityContextFactory;
import org.wildfly.extension.undertow.session.CodecSessionConfigWrapper;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.xnio.IoUtils;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.security.AuthenticationManager;

import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.AUTHENTICATE;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.DENY;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.PERMIT;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.security.authentication.JBossCachedAuthenticationManager;

/**
 * Service that builds up the undertow metadata.
 *
 * @author Stuart Douglas
 */
public class UndertowDeploymentInfoService implements Service<DeploymentInfo> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("UndertowDeploymentInfoService");

    public static final String DEFAULT_SERVLET_NAME = "default";
    public static final String UNDERTOW = "undertow";

    private DeploymentInfo deploymentInfo;
    private Registration registration;

    private final JBossWebMetaData mergedMetaData;
    private final String deploymentName;
    private final Module module;
    private final HashMap<String, TagLibraryInfo> tldInfo;
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
    private final List<ThreadSetupHandler> threadSetupActions;
    private final List<ServletExtension> servletExtensions;
    private final SharedSessionManagerConfig sharedSessionManagerConfig;
    private final boolean explodedDeployment;

    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private final InjectedValue<SessionManagerFactory> sessionManagerFactory = new InjectedValue<>();
    private final InjectedValue<SessionIdentifierCodec> sessionIdentifierCodec = new InjectedValue<>();
    private final InjectedValue<SecurityDomainContext> securityDomainContextValue = new InjectedValue<SecurityDomainContext>();
    private final InjectedValue<ServletContainerService> container = new InjectedValue<>();
    private final InjectedValue<ComponentRegistry> componentRegistryInjectedValue = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final InjectedValue<ControlPoint> controlPointInjectedValue = new InjectedValue<>();
    private final InjectedValue<SuspendController> suspendControllerInjectedValue = new InjectedValue<>();
    private final InjectedValue<ServerEnvironment> serverEnvironmentInjectedValue = new InjectedValue<>();
    private final Map<String, InjectedValue<Executor>> executorsByName = new HashMap<String, InjectedValue<Executor>>();
    private final WebSocketDeploymentInfo webSocketDeploymentInfo;
    private final File tempDir;
    private final List<File> externalResources;
    private final InjectedValue<BiFunction> securityFunction = new InjectedValue<>();
    private final List<Predicate> allowSuspendedRequests;

    private UndertowDeploymentInfoService(final JBossWebMetaData mergedMetaData, final String deploymentName, final HashMap<String, TagLibraryInfo> tldInfo, final Module module, final ScisMetaData scisMetaData, final VirtualFile deploymentRoot, final String jaccContextId, final String securityDomain, final List<ServletContextAttribute> attributes, final String contextPath, final List<SetupAction> setupActions, final Set<VirtualFile> overlays, final List<ExpressionFactoryWrapper> expressionFactoryWrappers, List<PredicatedHandler> predicatedHandlers, List<HandlerWrapper> initialHandlerChainWrappers, List<HandlerWrapper> innerHandlerChainWrappers, List<HandlerWrapper> outerHandlerChainWrappers, List<ThreadSetupHandler> threadSetupActions, boolean explodedDeployment, List<ServletExtension> servletExtensions, SharedSessionManagerConfig sharedSessionManagerConfig, WebSocketDeploymentInfo webSocketDeploymentInfo, File tempDir, List<File> externalResources, List<Predicate> allowSuspendedRequests) {
        this.mergedMetaData = mergedMetaData;
        this.deploymentName = deploymentName;
        this.tldInfo = tldInfo;
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
        this.webSocketDeploymentInfo = webSocketDeploymentInfo;
        this.tempDir = tempDir;
        this.externalResources = externalResources;
        this.allowSuspendedRequests = allowSuspendedRequests;
    }

    @Override
    public synchronized void start(final StartContext startContext) throws StartException {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(module.getClassLoader());
            DeploymentInfo deploymentInfo = createServletConfig();

            deploymentInfo.setConfidentialPortManager(getConfidentialPortManager());

            handleDistributable(deploymentInfo);
            if (securityFunction.getOptionalValue() == null) {
                if (securityDomain != null) {
                    handleIdentityManager(deploymentInfo);
                    handleJASPIMechanism(deploymentInfo);
                    handleJACCAuthorization(deploymentInfo);
                    handleAuthManagerLogout(deploymentInfo, mergedMetaData);
                } else {
                    deploymentInfo.setSecurityDisabled(true);
                }

                if(mergedMetaData.isUseJBossAuthorization()) {
                    deploymentInfo.setAuthorizationManager(new JbossAuthorizationManager(deploymentInfo.getAuthorizationManager()));
                }
            }
            handleAdditionalAuthenticationMechanisms(deploymentInfo);


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
            SecureRandomSessionIdGenerator sessionIdGenerator = new SecureRandomSessionIdGenerator();
            sessionIdGenerator.setLength(container.getValue().getSessionIdLength());
            deploymentInfo.setSessionIdGenerator(sessionIdGenerator);

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
                for (ThreadSetupHandler threadSetupAction : threadSetupActions) {
                    deploymentInfo.addThreadSetupAction(threadSetupAction);
                }
            }
            deploymentInfo.setServerName(serverEnvironmentInjectedValue.getValue().getProductConfig().getPrettyVersionString());
            if (undertowService.getValue().isStatisticsEnabled()) {
                deploymentInfo.setMetricsCollector(new UndertowMetricsCollector());
            }

            ControlPoint controlPoint = controlPointInjectedValue.getOptionalValue();
            if (controlPoint != null) {
                deploymentInfo.addOuterHandlerChainWrapper(GlobalRequestControllerHandler.wrapper(controlPoint, allowSuspendedRequests));
            }

            for (Map.Entry<String, AuthenticationMechanismFactory> e : container.getValue().getAuthenticationMechanisms().entrySet()) {
                deploymentInfo.addAuthenticationMechanism(e.getKey(), e.getValue());
            }
            deploymentInfo.setUseCachedAuthenticationMechanism(!deploymentInfo.getAuthenticationMechanisms().containsKey(SingleSignOnService.AUTHENTICATION_MECHANISM_NAME));

            this.deploymentInfo = deploymentInfo;
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }

    }

    private void handleAuthManagerLogout(DeploymentInfo deploymentInfo, JBossWebMetaData mergedMetaData) {
        AuthenticationManager manager = securityDomainContextValue.getValue().getAuthenticationManager();
        deploymentInfo.addNotificationReceiver(new LogoutNotificationReceiver(manager, securityDomain));
        if(mergedMetaData.isFlushOnSessionInvalidation()) {
            LogoutSessionListener listener = new LogoutSessionListener(manager);
            deploymentInfo.addListener(Servlets.listener(LogoutSessionListener.class, new ImmediateInstanceFactory<EventListener>(listener)));
        }
    }

    @Override
    public synchronized void stop(final StopContext stopContext) {
        IoUtils.safeClose(this.deploymentInfo.getResourceManager());
        if (securityDomain != null && securityFunction.getOptionalValue() == null) {
            AuthenticationManager authManager = securityDomainContextValue.getValue().getAuthenticationManager();
            if (authManager != null && authManager instanceof JBossCachedAuthenticationManager) {
                ((JBossCachedAuthenticationManager) authManager).releaseModuleEntries(module.getClassLoader());
            }
        }
        this.deploymentInfo.setConfidentialPortManager(null);
        this.deploymentInfo = null;
        if (registration != null) {
            registration.cancel();
        }
    }

    @Override
    public synchronized DeploymentInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return deploymentInfo;
    }

    /**
     * <p>Adds to the deployment the {@link org.wildfly.extension.undertow.security.jaspi.JASPICAuthenticationMechanism}, if necessary. The handler will be added if the security domain
     * is configured with JASPI authentication.</p>
     *
     * @param deploymentInfo
     */
    private void handleJASPIMechanism(final DeploymentInfo deploymentInfo) {
        ApplicationPolicy applicationPolicy = SecurityConfiguration.getApplicationPolicy(this.securityDomain);

        if (applicationPolicy != null && JASPIAuthenticationInfo.class.isInstance(applicationPolicy.getAuthenticationInfo())) {
            String authMethod = null;
            LoginConfig loginConfig = deploymentInfo.getLoginConfig();
            if (loginConfig != null && loginConfig.getAuthMethods().size() > 0) {
                authMethod = loginConfig.getAuthMethods().get(0).getName();
            }
            deploymentInfo.setJaspiAuthenticationMechanism(new JASPICAuthenticationMechanism(securityDomain, authMethod));
            deploymentInfo.setSecurityContextFactory(new JASPICSecurityContextFactory(this.securityDomain));
            deploymentInfo.addOuterHandlerChainWrapper(next -> new JASPICSecureResponseHandler(next));
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
                        deploymentInfo.setAuthorizationManager(JACCAuthorizationManager.INSTANCE);
                        break;
                    }
                }
            }
        }
    }

    private void handleAdditionalAuthenticationMechanisms(final DeploymentInfo deploymentInfo) {
        for (Map.Entry<String, AuthenticationMechanism> am : host.getValue().getAdditionalAuthenticationMechanisms().entrySet()) {
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
    }

    private ConfidentialPortManager getConfidentialPortManager() {
        return new ConfidentialPortManager() {

            @Override
            public int getConfidentialPort(HttpServerExchange exchange) {
                int port = exchange.getConnection().getLocalAddress(InetSocketAddress.class).getPort();
                if (port<0){
                    UndertowLogger.ROOT_LOGGER.debugf("Confidential port not defined for port %s", port);
                }
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
            mergedMetaData.resolveRunAs();
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
                List<String> externalOverlays = mergedMetaData.getOverlays();

                ResourceManager resourceManager = new ServletResourceManager(deploymentRoot, overlays, explodedDeployment, mergedMetaData.isSymbolicLinkingEnabled(), servletContainer.isDisableFileWatchService(), externalOverlays);

                resourceManager = new CachingResourceManager(servletContainer.getFileCacheMetadataSize(), servletContainer.getFileCacheMaxFileSize(), servletContainer.getBufferCache(), resourceManager, servletContainer.getFileCacheTimeToLive() == null ? (explodedDeployment ? 2000 : -1) : servletContainer.getFileCacheTimeToLive());
                if(externalResources != null && !externalResources.isEmpty()) {
                    //TODO: we don't cache external deployments, as they are intended for development use
                    //should be make this configurable or something?
                    List<ResourceManager> delegates = new ArrayList<>();
                    for(File resource : externalResources) {
                        delegates.add(new FileResourceManager(resource.getCanonicalFile(), 1024, true, mergedMetaData.isSymbolicLinkingEnabled(), "/"));
                    }
                    delegates.add(resourceManager);
                    resourceManager = new DelegatingResourceManager(delegates);
                }

                d.setResourceManager(resourceManager);
            } catch (IOException e) {
                throw new StartException(e);
            }

            d.setTempDir(tempDir);

            d.setClassLoader(module.getClassLoader());
            final String servletVersion = mergedMetaData.getServletVersion();
            if (servletVersion != null) {
                d.setMajorVersion(Integer.parseInt(servletVersion.charAt(0) + ""));
                d.setMinorVersion(Integer.parseInt(servletVersion.charAt(2) + ""));
            } else {
                d.setMajorVersion(3);
                d.setMinorVersion(1);
            }

            d.setDefaultCookieVersion(servletContainer.getDefaultCookieVersion());

            //in most cases flush just hurts performance for no good reason
            d.setIgnoreFlush(servletContainer.isIgnoreFlush());

            //controls initialization of filters on start of application
            d.setEagerFilterInit(servletContainer.isEagerFilterInit());

            d.setAllowNonStandardWrappers(servletContainer.isAllowNonStandardWrappers());
            d.setServletStackTraces(servletContainer.getStackTraces());
            d.setDisableCachingForSecuredPages(servletContainer.isDisableCachingForSecuredPages());
            if(servletContainer.isDisableSessionIdReuse()) {
                d.setCheckOtherSessionManagers(false);
            }

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

            //default JSP servlet
            final ServletInfo jspServlet = jspConfig != null ? jspConfig.createJSPServletInfo() : null;
            if (jspServlet != null) { //this would be null if jsp support is disabled
                HashMap<String, JspPropertyGroup> propertyGroups = createJspConfig(mergedMetaData);
                JspServletBuilder.setupDeployment(d, propertyGroups, tldInfo, new UndertowJSPInstanceManager(new WebInjectionContainer(module.getClassLoader(), componentRegistryInjectedValue.getValue())));

                if (mergedMetaData.getJspConfig() != null) {
                    Collection<JspPropertyGroup> values = new LinkedHashSet<>(propertyGroups.values());
                    d.setJspConfigDescriptor(new JspConfigDescriptorImpl(tldInfo.values(), values));
                }

                d.addServlet(jspServlet);

                final Set<String> jspPropertyGroupMappings = propertyGroups.keySet();
                for (final String mapping : jspPropertyGroupMappings) {
                    if(!jspServlet.getMappings().contains(mapping)) {
                        jspServlet.addMapping(mapping);
                    }
                }
                seenMappings.addAll(jspPropertyGroupMappings);
                //setup JSP application context initializing listener
                d.addListener(new ListenerInfo(JspInitializationListener.class));
                d.addServletContextAttribute(JspInitializationListener.CONTEXT_KEY, expressionFactoryWrappers);
            }

            d.setClassIntrospecter(new ComponentClassIntrospector(componentRegistry));

            final Map<String, List<ServletMappingMetaData>> servletMappings = new HashMap<>();

            if (mergedMetaData.getExecutorName() != null) {
                d.setExecutor(executorsByName.get(mergedMetaData.getExecutorName()).getValue());
            }

            Boolean proactiveAuthentication = mergedMetaData.getProactiveAuthentication();
            if(proactiveAuthentication == null) {
                proactiveAuthentication = container.getValue().isProactiveAuth();
            }
            d.setAuthenticationMode(proactiveAuthentication ? AuthenticationMode.PRO_ACTIVE : AuthenticationMode.CONSTRAINT_DRIVEN);

            if (servletExtensions != null) {
                for (ServletExtension extension : servletExtensions) {
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
                jspServlet.addHandlerChainWrapper(JspFileHandler.jspFileHandlerWrapper(null)); // we need to clear the file attribute if it is set (WFLY-4106)
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

            if(jspServlet != null) {
                if(!seenMappings.contains("*.jsp")) {
                    jspServlet.addMapping("*.jsp");
                }
                if(!seenMappings.contains("*.jspx")) {
                    jspServlet.addMapping("*.jspx");
                }
            }

            //we explicitly add the default servlet, to allow it to be mapped
            if (!mergedMetaData.getServlets().containsKey(ServletPathMatches.DEFAULT_SERVLET_NAME)) {
                ServletInfo defaultServlet = Servlets.servlet(DEFAULT_SERVLET_NAME, DefaultServlet.class);
                handleServletMappings(is22OrOlder, seenMappings, servletMappings, defaultServlet);

                d.addServlet(defaultServlet);
            }

            if(servletContainer.getDirectoryListingEnabled() != null) {
                ServletInfo defaultServlet = d.getServlets().get(DEFAULT_SERVLET_NAME);
                defaultServlet.addInitParam(DefaultServlet.DIRECTORY_LISTING, servletContainer.getDirectoryListingEnabled().toString());
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
                Set<String> tldListeners = new HashSet<>();
                for(Map.Entry<String, TagLibraryInfo> e : tldInfo.entrySet()) {
                    tldListeners.addAll(Arrays.asList(e.getValue().getListeners()));
                }
                for (ListenerMetaData listener : mergedMetaData.getListeners()) {
                    addListener(module.getClassLoader(), componentRegistry, d, listener, tldListeners.contains(listener.getListenerClass()));
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
            d.addWelcomePages(servletContainer.getWelcomeFiles());

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

            for(Map.Entry<String, String> entry : servletContainer.getMimeMappings().entrySet()) {
                d.addMimeMapping(new MimeMapping(entry.getKey(), entry.getValue()));
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

            BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration> securityFunction = this.securityFunction.getOptionalValue();
            if (securityFunction != null) {
                Map<String, RunAsIdentityMetaData> runAsIdentityMap = mergedMetaData.getRunAsIdentity();
                registration = securityFunction.apply(d, runAsIdentityMap::get);
                d.addOuterHandlerChainWrapper(JACCContextIdHandler.wrapper(jaccContextId));
                if(mergedMetaData.isUseJBossAuthorization()) {
                    UndertowLogger.ROOT_LOGGER.configurationOptionIgnoredWhenUsingElytron("use-jboss-authorization");
                }
            } else {
                if (securityDomain != null) {
                    d.addThreadSetupAction(new SecurityContextThreadSetupAction(securityDomain, securityDomainContextValue.getValue(), principalVersusRolesMap));

                    d.addInnerHandlerChainWrapper(SecurityContextAssociationHandler.wrapper(mergedMetaData.getRunAsIdentity()));
                    d.addOuterHandlerChainWrapper(JACCContextIdHandler.wrapper(jaccContextId));

                    d.addLifecycleInterceptor(new RunAsLifecycleInterceptor(mergedMetaData.getRunAsIdentity()));
                }
            }

            if (principalVersusRolesMap != null) {
                for (Map.Entry<String, Set<String>> entry : principalVersusRolesMap.entrySet()) {
                    d.addPrincipalVsRoleMappings(entry.getKey(), entry.getValue());
                }
            }

            // Setup an deployer configured ServletContext attributes
            if(attributes != null) {
                for (ServletContextAttribute attribute : attributes) {
                    d.addServletContextAttribute(attribute.getName(), attribute.getValue());
                }
            }

            //now setup websockets if they are enabled
            if(servletContainer.isWebsocketsEnabled() && webSocketDeploymentInfo != null) {
                webSocketDeploymentInfo.setBuffers(servletContainer.getWebsocketsBufferPool().getValue());
                webSocketDeploymentInfo.setWorker(servletContainer.getWebsocketsWorker().getValue());
                webSocketDeploymentInfo.setDispatchToWorkerThread(servletContainer.isDispatchWebsocketInvocationToWorker());

                if(servletContainer.isPerMessageDeflate()) {
                    PerMessageDeflateHandshake perMessageDeflate = new PerMessageDeflateHandshake(false, servletContainer.getDeflaterLevel());
                    webSocketDeploymentInfo.addExtension(perMessageDeflate);
                }

                final AtomicReference<ServerActivity> serverActivity = new AtomicReference<>();
                webSocketDeploymentInfo.addListener(wsc -> {
                    serverActivity.set(new ServerActivity() {
                        @Override
                        public void preSuspend(ServerActivityCallback listener) {
                            listener.done();
                        }

                        @Override
                        public void suspended(final ServerActivityCallback listener) {
                            if(wsc.getConfiguredServerEndpoints().isEmpty()) {
                                //TODO: remove this once undertow bug fix is upstream
                                listener.done();
                                return;
                            }
                            wsc.pause(new ServerWebSocketContainer.PauseListener() {
                                @Override
                                public void paused() {
                                    listener.done();
                                }

                                @Override
                                public void resumed() {
                                }
                            });
                        }

                        @Override
                        public void resume() {
                            wsc.resume();
                        }
                    });
                    suspendControllerInjectedValue.getValue().registerActivity(serverActivity.get());
                });
                ServletContextListener sl = new ServletContextListener() {
                    @Override
                    public void contextInitialized(ServletContextEvent sce) {}

                    @Override
                    public void contextDestroyed(ServletContextEvent sce) {
                        final ServerActivity activity = serverActivity.get();
                        if(activity != null) {
                            suspendControllerInjectedValue.getValue().unRegisterActivity(activity);
                        }
                    }
                };
                d.addListener(new ListenerInfo(sl.getClass(), new ImmediateInstanceFactory<EventListener>(sl)));

                d.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSocketDeploymentInfo);

            }


            if (mergedMetaData.getLocalEncodings() != null &&
                    mergedMetaData.getLocalEncodings().getMappings() != null) {
                for (LocaleEncodingMetaData locale : mergedMetaData.getLocalEncodings().getMappings()) {
                    d.addLocaleCharsetMapping(locale.getLocale(), locale.getEncoding());
                }
            }

            if (predicatedHandlers != null && !predicatedHandlers.isEmpty()) {
                d.addOuterHandlerChainWrapper(new RewriteCorrectingHandlerWrappers.PostWrapper());
                d.addOuterHandlerChainWrapper(new HandlerWrapper() {
                    @Override
                    public HttpHandler wrap(HttpHandler handler) {
                        return Handlers.predicates(predicatedHandlers, handler);
                    }
                });
                d.addOuterHandlerChainWrapper(new RewriteCorrectingHandlerWrappers.PreWrapper());
            }

            if (mergedMetaData.getDefaultEncoding() != null) {
                d.setDefaultEncoding(mergedMetaData.getDefaultEncoding());
            } else if (servletContainer.getDefaultEncoding() != null) {
                d.setDefaultEncoding(servletContainer.getDefaultEncoding());
            }
            d.setCrawlerSessionManagerConfig(servletContainer.getCrawlerSessionManagerConfig());

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
                    } else {
                        UndertowLogger.ROOT_LOGGER.duplicateServletMapping(pattern);
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

    private static void addListener(final ClassLoader classLoader, final ComponentRegistry components, final DeploymentInfo d, final ListenerMetaData listener, boolean programatic) throws ClassNotFoundException {

        ListenerInfo l;
        final Class<? extends EventListener> listenerClass = (Class<? extends EventListener>) classLoader.loadClass(listener.getListenerClass());
        ManagedReferenceFactory creator = components.createInstanceFactory(listenerClass);
        if (creator != null) {
            InstanceFactory<EventListener> factory = createInstanceFactory(creator);
            l = new ListenerInfo(listenerClass, factory, programatic);
        } else {
            l = new ListenerInfo(listenerClass, programatic);
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

    public InjectedValue<ControlPoint> getControlPointInjectedValue() {
        return controlPointInjectedValue;
    }

    public InjectedValue<ComponentRegistry> getComponentRegistryInjectedValue() {
        return componentRegistryInjectedValue;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjectedValue() {
        return suspendControllerInjectedValue;
    }

    public InjectedValue<ServerEnvironment> getServerEnvironmentInjectedValue() {
        return serverEnvironmentInjectedValue;
    }

    public Injector<BiFunction> getSecurityFunctionInjector() {
        return securityFunction;
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
        private HashMap<String, TagLibraryInfo> tldInfo;
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
        private List<ThreadSetupHandler> threadSetupActions;
        private List<ServletExtension> servletExtensions;
        private SharedSessionManagerConfig sharedSessionManagerConfig;
        private boolean explodedDeployment;
        private WebSocketDeploymentInfo webSocketDeploymentInfo;
        private File tempDir;
        private List<File> externalResources;
        List<Predicate> allowSuspendedRequests;

        Builder setMergedMetaData(final JBossWebMetaData mergedMetaData) {
            this.mergedMetaData = mergedMetaData;
            return this;
        }

        public Builder setDeploymentName(final String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder setTldInfo(HashMap<String, TagLibraryInfo> tldInfo) {
            this.tldInfo = tldInfo;
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

        public Builder setThreadSetupActions(List<ThreadSetupHandler> threadSetupActions) {
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

        public Builder setWebSocketDeploymentInfo(WebSocketDeploymentInfo webSocketDeploymentInfo) {
            this.webSocketDeploymentInfo = webSocketDeploymentInfo;
            return this;
        }

        public File getTempDir() {
            return tempDir;
        }

        public Builder setTempDir(File tempDir) {
            this.tempDir = tempDir;
            return this;
        }

        public Builder setAllowSuspendedRequests(List<Predicate> allowSuspendedRequests) {
            this.allowSuspendedRequests = allowSuspendedRequests;
            return this;
        }

        public Builder setExternalResources(List<File> externalResources) {
            this.externalResources = externalResources;
            return this;
        }

        public UndertowDeploymentInfoService createUndertowDeploymentInfoService() {
            return new UndertowDeploymentInfoService(mergedMetaData, deploymentName, tldInfo, module,
                    scisMetaData, deploymentRoot, jaccContextId, securityDomain, attributes, contextPath, setupActions, overlays,
                    expressionFactoryWrappers, predicatedHandlers, initialHandlerChainWrappers, innerHandlerChainWrappers, outerHandlerChainWrappers,
                    threadSetupActions, explodedDeployment, servletExtensions, sharedSessionManagerConfig, webSocketDeploymentInfo, tempDir, externalResources, allowSuspendedRequests);
        }
    }

    private static class UndertowThreadSetupAction implements ThreadSetupHandler {

        private final SetupAction action;

        private UndertowThreadSetupAction(SetupAction action) {
            this.action = action;
        }

        @Override
        public <T, C> Action<T, C> create(Action<T, C> action) {
            return (exchange, context) -> {
                UndertowThreadSetupAction.this.action.setup(Collections.emptyMap());
                try {
                    return action.call(exchange, context);
                } finally {
                    UndertowThreadSetupAction.this.action.teardown(Collections.emptyMap());
                }
            };
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.Handlers;
import io.undertow.jsp.JspFileHandler;
import io.undertow.jsp.JspServletBuilder;
import io.undertow.predicate.Predicate;
import io.undertow.security.api.AuthenticationMechanism;
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
import io.undertow.servlet.api.SessionConfigWrapper;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.handlers.ServletPathMatches;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.UndertowContainerProvider;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.jasper.servlet.JspServlet;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.web.common.CachingWebInjectionContainer;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.session.SharedSessionManagerConfig;
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.JSPConfig;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.CookieConfig;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.ApplicationSecurityDomainDefinition.Registration;
import org.wildfly.extension.undertow.security.jacc.JACCContextIdHandler;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.MechanismConfiguration;
import org.wildfly.security.auth.server.MechanismConfigurationSelector;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.xnio.IoUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.AUTHENTICATE;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.DENY;
import static io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic.PERMIT;

import org.jboss.as.server.ServerEnvironment;

/**
 * Service that builds up the undertow metadata.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UndertowDeploymentInfoService implements Service<DeploymentInfo> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("UndertowDeploymentInfoService");

    public static final String DEFAULT_SERVLET_NAME = "default";
    public static final String UNDERTOW = "undertow";

    private DeploymentInfo deploymentInfo;
    private Registration registration;

    private final AtomicReference<ServerActivity> serverActivity = new AtomicReference<>();
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

    private final Consumer<DeploymentInfo> deploymentInfoConsumer;
    private final Supplier<UndertowService> undertowService;
    private final Supplier<SessionManagerFactory> sessionManagerFactory;
    private final Supplier<Function<CookieConfig, SessionConfigWrapper>> sessionConfigWrapperFactory;
    private final Supplier<ServletContainerService> container;
    private final Supplier<ComponentRegistry> componentRegistry;
    private final Supplier<Host> host;
    private final Supplier<ControlPoint> controlPoint;
    private final Supplier<SuspendController> suspendController;
    private final Supplier<ServerEnvironment> serverEnvironment;
    private final Supplier<SecurityDomain> rawSecurityDomain;
    private final Supplier<HttpServerAuthenticationMechanismFactory> rawMechanismFactory;
    private final Supplier<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> applySecurityFunction;
    private final Map<String, Supplier<Executor>> executorsByName = new HashMap<>();
    private final WebSocketDeploymentInfo webSocketDeploymentInfo;
    private final File tempDir;
    private final List<File> externalResources;
    private final List<Predicate> allowSuspendedRequests;

    private UndertowDeploymentInfoService(
            final Consumer<DeploymentInfo> deploymentInfoConsumer,
            final Supplier<UndertowService> undertowService,
            final Supplier<SessionManagerFactory> sessionManagerFactory,
            final Supplier<Function<CookieConfig, SessionConfigWrapper>> sessionConfigWrapperFactory,
            final Supplier<ServletContainerService> container,
            final Supplier<ComponentRegistry> componentRegistry,
            final Supplier<Host> host,
            final Supplier<ControlPoint> controlPoint,
            final Supplier<SuspendController> suspendController,
            final Supplier<ServerEnvironment> serverEnvironment,
            final Supplier<SecurityDomain> rawSecurityDomain,
            final Supplier<HttpServerAuthenticationMechanismFactory> rawMechanismFactory,
            final Supplier<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> applySecurityFunction,
            final JBossWebMetaData mergedMetaData, final String deploymentName, final HashMap<String, TagLibraryInfo> tldInfo, final Module module, final ScisMetaData scisMetaData, final VirtualFile deploymentRoot, final String jaccContextId, final String securityDomain, final List<ServletContextAttribute> attributes, final String contextPath, final List<SetupAction> setupActions, final Set<VirtualFile> overlays, final List<ExpressionFactoryWrapper> expressionFactoryWrappers, List<PredicatedHandler> predicatedHandlers, List<HandlerWrapper> initialHandlerChainWrappers, List<HandlerWrapper> innerHandlerChainWrappers, List<HandlerWrapper> outerHandlerChainWrappers, List<ThreadSetupHandler> threadSetupActions, boolean explodedDeployment, List<ServletExtension> servletExtensions, SharedSessionManagerConfig sharedSessionManagerConfig, WebSocketDeploymentInfo webSocketDeploymentInfo, File tempDir, List<File> externalResources, List<Predicate> allowSuspendedRequests) {
        this.deploymentInfoConsumer = deploymentInfoConsumer;
        this.undertowService = undertowService;
        this.sessionManagerFactory = sessionManagerFactory;
        this.sessionConfigWrapperFactory = sessionConfigWrapperFactory;
        this.container = container;
        this.componentRegistry = componentRegistry;
        this.host = host;
        this.controlPoint = controlPoint;
        this.suspendController = suspendController;
        this.serverEnvironment = serverEnvironment;
        this.rawSecurityDomain = rawSecurityDomain;
        this.rawMechanismFactory = rawMechanismFactory;
        this.applySecurityFunction = applySecurityFunction;
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
            if (!isElytronActive()) {
                if (securityDomain != null || mergedMetaData.isUseJBossAuthorization()) {
                    throw UndertowLogger.ROOT_LOGGER.legacySecurityUnsupported();
                } else {
                    deploymentInfo.setSecurityDisabled(true);
                }
            }
            handleAdditionalAuthenticationMechanisms(deploymentInfo);


            SessionConfigMetaData sessionConfig = mergedMetaData.getSessionConfig();
            if(sharedSessionManagerConfig != null && sharedSessionManagerConfig.getSessionConfig() != null) {
                sessionConfig = sharedSessionManagerConfig.getSessionConfig();
            }
            ServletSessionConfig config = null;
            //default session config
            CookieConfig defaultSessionConfig = container.get().getSessionCookieConfig();
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
            }
            SecureRandomSessionIdGenerator sessionIdGenerator = new SecureRandomSessionIdGenerator();
            sessionIdGenerator.setLength(container.get().getSessionIdLength());
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
                deploymentInfo.setDefaultSessionTimeout(container.get().getDefaultSessionTimeout() * 60);
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
            deploymentInfo.setServerName(serverEnvironment.get().getProductConfig().getPrettyVersionString());
            if (undertowService.get().isStatisticsEnabled()) {
                deploymentInfo.setMetricsCollector(new UndertowMetricsCollector());
            }

            ControlPoint controlPoint = this.controlPoint != null ? this.controlPoint.get() : null;
            if (controlPoint != null) {
                deploymentInfo.addOuterHandlerChainWrapper(GlobalRequestControllerHandler.wrapper(controlPoint, allowSuspendedRequests));
            }

            deploymentInfoConsumer.accept(this.deploymentInfo = deploymentInfo);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }

    }

    @Override
    public synchronized void stop(final StopContext stopContext) {
        // Remove the server activity
        final ServerActivity activity = serverActivity.get();
        if(activity != null) {
            suspendController.get().unRegisterActivity(activity);
        }
        if (System.getSecurityManager() == null) {
            UndertowContainerProvider.removeContainer(module.getClassLoader());
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    UndertowContainerProvider.removeContainer(module.getClassLoader());
                    return null;
                }
            });
        }
        deploymentInfoConsumer.accept(null);
        IoUtils.safeClose(this.deploymentInfo.getResourceManager());
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

    private void handleAdditionalAuthenticationMechanisms(final DeploymentInfo deploymentInfo) {
        for (Map.Entry<String, AuthenticationMechanism> am : host.get().getAdditionalAuthenticationMechanisms().entrySet()) {
            deploymentInfo.addFirstAuthenticationMechanism(am.getKey(), am.getValue());
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
                return host.get().getServer().lookupSecurePort(port);
            }
        };
    }

    private void handleDistributable(final DeploymentInfo deploymentInfo) {
        SessionManagerFactory managerFactory = this.sessionManagerFactory != null ? this.sessionManagerFactory.get() : null;
        if (managerFactory != null) {
            deploymentInfo.setSessionManagerFactory(managerFactory);
        }

        Function<CookieConfig, SessionConfigWrapper> sessionConfigWrapperFactory = this.sessionConfigWrapperFactory != null ? this.sessionConfigWrapperFactory.get() : null;
        if (sessionConfigWrapperFactory != null) {
            deploymentInfo.setSessionConfigWrapper(sessionConfigWrapperFactory.apply(this.container.get().getAffinityCookieConfig()));
        }
    }

    private DeploymentInfo createServletConfig() throws StartException {
        final ComponentRegistry componentRegistry = this.componentRegistry.get();
        try {
            if (!mergedMetaData.isMetadataComplete()) {
                mergedMetaData.resolveAnnotations();
            }
            mergedMetaData.resolveRunAs();
            final DeploymentInfo d = new DeploymentInfo();
            d.setContextPath(contextPath);
            if (mergedMetaData.getDescriptionGroup() != null) {
                d.setDisplayName(mergedMetaData.getDescriptionGroup().getDisplayName());
            }
            d.setDeploymentName(deploymentName);
            d.setHostName(host.get().getName());

            final ServletContainerService servletContainer = container.get();
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
            d.setOrphanSessionAllowed(servletContainer.isOrphanSessionAllowed());

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

            //default Jakarta Server Pages servlet
            final ServletInfo jspServlet = jspConfig != null ? jspConfig.createJSPServletInfo() : null;
            if (jspServlet != null) { //this would be null if jsp support is disabled
                HashMap<String, JspPropertyGroup> propertyGroups = createJspConfig(mergedMetaData);
                JspServletBuilder.setupDeployment(d, propertyGroups, tldInfo, new UndertowJSPInstanceManager(new CachingWebInjectionContainer(module.getClassLoader(), componentRegistry)));

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
                //setup Jakarta Server Pages application context initializing listener
                d.addListener(new ListenerInfo(JspInitializationListener.class));
                d.addServletContextAttribute(JspInitializationListener.CONTEXT_KEY, expressionFactoryWrappers);
            }

            d.setClassIntrospecter(new ComponentClassIntrospector(componentRegistry));

            final Map<String, List<ServletMappingMetaData>> servletMappings = new HashMap<>();

            if (mergedMetaData.getExecutorName() != null) {
                d.setExecutor(executorsByName.get(mergedMetaData.getExecutorName()).get());
            }

            Boolean proactiveAuthentication = mergedMetaData.getProactiveAuthentication();
            if(proactiveAuthentication == null) {
                proactiveAuthentication = container.get().isProactiveAuth();
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
                        ManagedReferenceFactory creator = componentRegistry.createInstanceFactory(servletClass, true);
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
                    s.setExecutor(executorsByName.get(servlet.getExecutorName()).get());
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

                                    d.addFilterUrlMapping(mapping.getFilterName(), url, jakarta.servlet.DispatcherType.valueOf(dispatcher.name()));
                                }
                            } else {
                                d.addFilterUrlMapping(mapping.getFilterName(), url, jakarta.servlet.DispatcherType.REQUEST);
                            }
                        }
                    }
                    if (mapping.getServletNames() != null) {
                        for (String servletName : mapping.getServletNames()) {
                            if (mapping.getDispatchers() != null && !mapping.getDispatchers().isEmpty()) {
                                for (DispatcherType dispatcher : mapping.getDispatchers()) {
                                    d.addFilterServletNameMapping(mapping.getFilterName(), servletName, jakarta.servlet.DispatcherType.valueOf(dispatcher.name()));
                                }
                            } else {
                                d.addFilterServletNameMapping(mapping.getFilterName(), servletName, jakarta.servlet.DispatcherType.REQUEST);
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

            if (isElytronActive()) {
                Map<String, RunAsIdentityMetaData> runAsIdentityMap = mergedMetaData.getRunAsIdentity();
                applyElytronSecurity(d, runAsIdentityMap::get);
            } else {
                if (securityDomain != null) {
                    throw UndertowLogger.ROOT_LOGGER.legacySecurityUnsupported();
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
                webSocketDeploymentInfo.setBuffers(servletContainer.getWebsocketsBufferPool());
                webSocketDeploymentInfo.setWorker(servletContainer.getWebsocketsWorker());
                webSocketDeploymentInfo.setDispatchToWorkerThread(servletContainer.isDispatchWebsocketInvocationToWorker());

                if(servletContainer.isPerMessageDeflate()) {
                    PerMessageDeflateHandshake perMessageDeflate = new PerMessageDeflateHandshake(false, servletContainer.getDeflaterLevel());
                    webSocketDeploymentInfo.addExtension(perMessageDeflate);
                }

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
                    suspendController.get().registerActivity(serverActivity.get());
                });

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

            if (mergedMetaData.getResponseCharacterEncoding() != null) {
                d.setDefaultResponseEncoding(mergedMetaData.getResponseCharacterEncoding());
            }
            if (mergedMetaData.getRequestCharacterEncoding() != null) {
                d.setDefaultRequestEncoding(mergedMetaData.getRequestCharacterEncoding());
            }

            d.setCrawlerSessionManagerConfig(servletContainer.getCrawlerSessionManagerConfig());

            d.setPreservePathOnForward(servletContainer.isPreservePathOnForward());

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
     * {@link jakarta.servlet.http.HttpServletRequest}.
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
        // Jakarta Server Pages Config
        JspConfigMetaData config = metaData.getJspConfig();
        if (config != null) {
            // Jakarta Server Pages Property groups
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

    /*
     * Elytron Security Methods
     */

    private boolean isElytronActive() {
        return (applySecurityFunction != null && applySecurityFunction.get() != null) || (rawSecurityDomain != null && rawSecurityDomain.get() != null);
    }

    private void applyElytronSecurity(final DeploymentInfo deploymentInfo, Function<String, RunAsIdentityMetaData> runAsMapping) {
        BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration> securityFunction = applySecurityFunction != null ? applySecurityFunction.get() : null;
        if (securityFunction != null) {
            registration = securityFunction.apply(deploymentInfo, runAsMapping);
        } else {
            HttpServerAuthenticationMechanismFactory mechanismFactory = rawMechanismFactory == null ? null : rawMechanismFactory.get();
            SecurityDomain securityDomain = rawSecurityDomain.get();

            org.wildfly.elytron.web.undertow.server.servlet.AuthenticationManager.Builder builder =
                    org.wildfly.elytron.web.undertow.server.servlet.AuthenticationManager.builder();

            if (mechanismFactory != null) {
                HttpAuthenticationFactory httpAuthenticationFactory = HttpAuthenticationFactory.builder()
                        .setFactory(mechanismFactory)
                        .setSecurityDomain(securityDomain)
                        .setMechanismConfigurationSelector(MechanismConfigurationSelector.constantSelector(MechanismConfiguration.EMPTY))
                        .build();
                builder.setHttpAuthenticationFactory(httpAuthenticationFactory);
                builder.setOverrideDeploymentConfig(true).setRunAsMapper(runAsMapping);
            } else {
                builder = builder.setSecurityDomain(securityDomain);
                builder.setOverrideDeploymentConfig(true)
                        .setRunAsMapper(runAsMapping)
                        .setIntegratedJaspi(false)
                        .setEnableJaspi(true);
            }

            org.wildfly.elytron.web.undertow.server.servlet.AuthenticationManager authenticationManager = builder.build();
            authenticationManager.configure(deploymentInfo);
        }

        deploymentInfo.addOuterHandlerChainWrapper(JACCContextIdHandler.wrapper(jaccContextId));
        if(mergedMetaData.isUseJBossAuthorization()) {
            UndertowLogger.ROOT_LOGGER.configurationOptionIgnoredWhenUsingElytron("use-jboss-authorization");
        }
    }

    public void addInjectedExecutor(final String name, final Supplier<Executor> injected) {
        executorsByName.put(name, injected);
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

        public UndertowDeploymentInfoService createUndertowDeploymentInfoService(
                final Consumer<DeploymentInfo> deploymentInfoConsumer,
                final Supplier<UndertowService> undertowService,
                final Supplier<SessionManagerFactory> sessionManagerFactory,
                final Supplier<Function<CookieConfig, SessionConfigWrapper>> sessionConfigWrapperFactory,
                final Supplier<ServletContainerService> container,
                final Supplier<ComponentRegistry> componentRegistry,
                final Supplier<Host> host,
                final Supplier<ControlPoint> controlPoint,
                final Supplier<SuspendController> suspendController,
                final Supplier<ServerEnvironment> serverEnvironment,
                final Supplier<SecurityDomain> rawSecurityDomain,
                final Supplier<HttpServerAuthenticationMechanismFactory> rawMechanismFactory,
                final Supplier<BiFunction<DeploymentInfo, Function<String, RunAsIdentityMetaData>, Registration>> applySecurityFunction
        ) {
            return new UndertowDeploymentInfoService(deploymentInfoConsumer, undertowService, sessionManagerFactory,
                    sessionConfigWrapperFactory, container, componentRegistry, host, controlPoint,
                    suspendController, serverEnvironment, rawSecurityDomain, rawMechanismFactory, applySecurityFunction, mergedMetaData, deploymentName, tldInfo, module,
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

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.publish;

import java.io.File;
import java.security.AccessController;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.host.ServletBuilder;
import org.jboss.as.web.host.WebDeploymentBuilder;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebHost;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.deployers.AllowWSRequestPredicate;
import org.jboss.as.webservices.deployers.EndpointServiceDeploymentAspect;
import org.jboss.as.webservices.deployers.deployment.DeploymentAspectsProvider;
import org.jboss.as.webservices.deployers.deployment.WSDeploymentBuilder;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.ws.common.deployment.DeploymentAspectManagerImpl;
import org.jboss.ws.common.deployment.EndpointHandlerDeploymentAspect;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.ws.common.invocation.InvocationHandlerJAXWS;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.DeploymentAspectManager;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.WSFServlet;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.wsf.spi.publish.Context;
import org.jboss.wsf.spi.publish.EndpointPublisher;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * WS endpoint publisher, allows for publishing a WS endpoint on AS 7
 *
 * @author alessio.soldano@jboss.com
 * @since 12-Jul-2011
 */
public final class EndpointPublisherImpl implements EndpointPublisher {

    private final WebHost host;
    private final boolean runningInService;
    private static List<DeploymentAspect> publisherDepAspects = null;
    private static List<DeploymentAspect> depAspects = null;

    protected EndpointPublisherImpl(WebHost host) {
        this(host, false);
    }

    protected EndpointPublisherImpl(WebHost host, boolean runningInService) {
        this.host = host;
        this.runningInService = runningInService;
    }

    protected EndpointPublisherImpl(boolean runningInService) {
        this(null, runningInService);
    }

    @Override
    public Context publish(String context, ClassLoader loader, Map<String, String> urlPatternToClassNameMap) throws Exception {
        return publish(getBaseTarget(), context, loader, urlPatternToClassNameMap, null, null, null);
    }

    @Override
    public Context publish(String context, ClassLoader loader, Map<String, String> urlPatternToClassNameMap, WebservicesMetaData metadata) throws Exception {
        return publish(getBaseTarget(), context, loader, urlPatternToClassNameMap, null, metadata, null);
    }

    @Override
    public Context publish(String context, ClassLoader loader, Map<String, String> urlPatternToClassNameMap,
            WebservicesMetaData metadata, JBossWebservicesMetaData jbwsMetadata) throws Exception {
        return publish(getBaseTarget(), context, loader, urlPatternToClassNameMap, null, metadata, jbwsMetadata);
    }

    protected Context publish(ServiceTarget target, String context, ClassLoader loader,
            Map<String, String> urlPatternToClassNameMap, JBossWebMetaData jbwmd, WebservicesMetaData metadata, JBossWebservicesMetaData jbwsMetadata)
            throws Exception {
        DeploymentUnit unit = doPrepare(context, loader, urlPatternToClassNameMap, jbwmd, metadata, jbwsMetadata);
        doDeploy(target, unit);
        return doPublish(target, unit);
    }

    private static ServiceTarget getBaseTarget() {
        return currentServiceContainer().getService(WSServices.CONFIG_SERVICE).getServiceContainer();
    }

    /**
     * Prepare the ws Deployment and return a DeploymentUnit containing it
     *
     * @param context
     * @param loader
     * @param urlPatternToClassNameMap
     * @param jbwmd
     * @param metadata
     * @param jbwsMetadata
     * @return
     */
    protected DeploymentUnit doPrepare(String context, ClassLoader loader,
            Map<String, String> urlPatternToClassNameMap, JBossWebMetaData jbwmd, WebservicesMetaData metadata, JBossWebservicesMetaData jbwsMetadata) {
        ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        WSEndpointDeploymentUnit unit = new WSEndpointDeploymentUnit(loader, context, urlPatternToClassNameMap, jbwmd, metadata, jbwsMetadata);
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            WSDeploymentBuilder.getInstance().build(unit);
            return unit;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
        }
    }

    /**
     * Triggers the WS deployment aspects, which process the deployment and
     * install the endpoint services.
     *
     * @param target
     * @param unit
     */
    protected void doDeploy(ServiceTarget target, DeploymentUnit unit) {
        List<DeploymentAspect> aspects = getDeploymentAspects();
        ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Deployment dep = null;
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            dep = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
            dep.addAttachment(ServiceTarget.class, target);
            DeploymentAspectManager dam = new DeploymentAspectManagerImpl();
            dam.setDeploymentAspects(aspects);
            dam.deploy(dep);
        } finally {
            if (dep != null) {
                dep.removeAttachment(ServiceTarget.class);
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
        }
    }

    /**
     * Publish the webapp for the WS deployment unit
     *
     * @param target
     * @param unit
     * @return
     * @throws Exception
     */
    protected Context doPublish(ServiceTarget target, DeploymentUnit unit) throws Exception {
        Deployment deployment = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
        List<Endpoint> endpoints = deployment.getService().getEndpoints();
        //If we're running in a Service, that will already have proper dependencies set on the installed endpoint services,
        //otherwise we need to explicitly wait for the endpoint services to be started before creating the webapp.
        if (!runningInService) {
            final ServiceRegistry registry = unit.getServiceRegistry();
            for (Endpoint ep : endpoints) {
                final ServiceName serviceName = EndpointService.getServiceName(unit, ep.getShortName());
                registry.getRequiredService(serviceName).awaitValue();
            }
        }
        deployment.addAttachment(WebDeploymentController.class, startWebApp(host, unit)); //TODO simplify and use findChild later in destroy()/stopWebApp()
        return new Context(unit.getAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY).getContextRoot(), endpoints);
    }

    private static WebDeploymentController startWebApp(WebHost host, DeploymentUnit unit) throws Exception {
        WebDeploymentBuilder deployment = new WebDeploymentBuilder();
        WebDeploymentController handle;
        try {
            JBossWebMetaData jbwebMD = unit.getAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY);
            deployment.setContextRoot(jbwebMD.getContextRoot());
            ServerConfigService config = (ServerConfigService)unit.getServiceRegistry().getService(WSServices.CONFIG_SERVICE).getService();
            File docBase = new File(config.getValue().getServerTempDir(), jbwebMD.getContextRoot());
            if (!docBase.exists()) {
                docBase.mkdirs();
            }
            deployment.setDocumentRoot(docBase);
            deployment.setClassLoader(unit.getAttachment(WSAttachmentKeys.CLASSLOADER_KEY));
            deployment.addAllowedRequestPredicate(new AllowWSRequestPredicate());

            addServlets(jbwebMD, deployment);

            handle = host.addWebDeployment(deployment);
            handle.create();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.createContextPhaseFailed(e);
        }
        try {
            handle.start();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.startContextPhaseFailed(e);
        }
        return handle;
    }

    private static void addServlets(JBossWebMetaData jbwebMD, WebDeploymentBuilder deployment) {
        for (JBossServletMetaData smd : jbwebMD.getServlets()) {
            final String sc = smd.getServletClass();
            if (sc.equals(WSFServlet.class.getName())) {
                ServletBuilder servletBuilder = new ServletBuilder();
                final String servletName = smd.getServletName();

                List<ParamValueMetaData> params = smd.getInitParam();
                List<String> urlPatterns = null;
                for (ServletMappingMetaData smmd : jbwebMD.getServletMappings()) {
                    if (smmd.getServletName().equals(servletName)) {
                        urlPatterns = smmd.getUrlPatterns();
                        servletBuilder.addUrlMappings(urlPatterns);
                        break;
                    }
                }

                WSFServlet wsfs = new WSFServlet();
                servletBuilder.setServletName(servletName);
                servletBuilder.setServlet(wsfs);
                servletBuilder.setServletClass(WSFServlet.class);
                for (ParamValueMetaData param : params) {
                    servletBuilder.addInitParam(param.getParamName(), param.getParamValue());
                }
                deployment.addServlet(servletBuilder);
            }
        }
    }

    @Override
    public void destroy(Context context) throws Exception {
        List<Endpoint> eps = context.getEndpoints();
        if (eps == null || eps.isEmpty()) {
            return;
        }
        Deployment dep = eps.get(0).getService().getDeployment();
        try {
            stopWebApp(dep);
        } finally {
            undeploy(dep);
        }
    }

    /**
     * Triggers the WS deployment aspects, which process
     * the deployment unit and stop the endpoint services.
     *
     * @param context
     * @throws Exception
     */
    protected void undeploy(DeploymentUnit unit) throws Exception {
        undeploy(unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY));
    }

    protected void undeploy(Deployment deployment) throws Exception {
        List<DeploymentAspect> aspects = getDeploymentAspects();
        ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            DeploymentAspectManager dam = new DeploymentAspectManagerImpl();
            dam.setDeploymentAspects(aspects);
            dam.undeploy(deployment);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
        }
    }

    /**
     * Stops the webapp serving the provided ws deployment
     *
     * @param deployment
     * @throws Exception
     */
    protected void stopWebApp(Deployment deployment) throws Exception {
        WebDeploymentController context;
        try {
            context = deployment.getAttachment(WebDeploymentController.class);
            context.stop();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.stopContextPhaseFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            throw WSLogger.ROOT_LOGGER.destroyContextPhaseFailed(e);
        }
    }

    private List<DeploymentAspect> getDeploymentAspects() {
        return runningInService ? getReplacedDeploymentAspects() : getPublisherDeploymentAspects();
    }

    private static synchronized List<DeploymentAspect> getReplacedDeploymentAspects() {
        if (depAspects == null) {
            depAspects = new LinkedList<DeploymentAspect>();
            List<DeploymentAspect> serverAspects = DeploymentAspectsProvider.getSortedDeploymentAspects();
            for (DeploymentAspect aspect : serverAspects) {
                if(aspect instanceof EndpointHandlerDeploymentAspect) {
                    depAspects.add(aspect);
                    //add another aspect to set InvocationHandlerJAXWS to each endpoint
                    ForceJAXWSInvocationHandlerDeploymentAspect handlerAspect = new ForceJAXWSInvocationHandlerDeploymentAspect();
                    depAspects.add(handlerAspect);
                } else {
                    depAspects.add(aspect);
                }
            }
        }
        return depAspects;
    }

    private static synchronized List<DeploymentAspect> getPublisherDeploymentAspects() {
        if (publisherDepAspects == null) {
            publisherDepAspects = new LinkedList<DeploymentAspect>();
            // copy to replace the EndpointServiceDeploymentAspect
            List<DeploymentAspect> serverAspects = DeploymentAspectsProvider.getSortedDeploymentAspects();
            for (DeploymentAspect aspect : serverAspects) {
                if (aspect instanceof EndpointServiceDeploymentAspect) {
                    final EndpointServiceDeploymentAspect a = (EndpointServiceDeploymentAspect) aspect;
                    EndpointServiceDeploymentAspect clone = (EndpointServiceDeploymentAspect) (a.clone());
                    clone.setStopServices(true);
                    publisherDepAspects.add(clone);
                } else if(aspect instanceof EndpointHandlerDeploymentAspect) {
                    publisherDepAspects.add(aspect);
                    //add another aspect to set InvocationHandlerJAXWS to each endpoint
                    ForceJAXWSInvocationHandlerDeploymentAspect handlerAspect = new ForceJAXWSInvocationHandlerDeploymentAspect();
                    publisherDepAspects.add(handlerAspect);
                } else {
                    publisherDepAspects.add(aspect);
                }
            }
        }
        return publisherDepAspects;
    }

    static class ForceJAXWSInvocationHandlerDeploymentAspect extends AbstractDeploymentAspect {
        public ForceJAXWSInvocationHandlerDeploymentAspect() {
        }
        @Override
        public void start(final Deployment dep) {
            for (final Endpoint ep : dep.getService().getEndpoints()) {
                ep.setInvocationHandler(new InvocationHandlerJAXWS());
            }
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.webservices.WSMessages.MESSAGES;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import org.jboss.as.web.host.ServletBuilder;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebDeploymentBuilder;
import org.jboss.as.web.host.WebHost;
import org.jboss.as.webservices.deployers.EndpointServiceDeploymentAspect;
import org.jboss.as.webservices.deployers.deployment.DeploymentAspectsProvider;
import org.jboss.as.webservices.deployers.deployment.WSDeploymentBuilder;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.ws.common.deployment.DeploymentAspectManagerImpl;
import org.jboss.ws.common.invocation.InvocationHandlerJAXWS;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.DeploymentAspectManager;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.EndpointState;
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

   private WebHost host;
    private boolean runningInService = false;
    private static List<DeploymentAspect> depAspects = null;

    public EndpointPublisherImpl(WebHost host) {
        this.host = host;
    }

    public EndpointPublisherImpl(WebHost host, boolean runningInService) {
        this(host);
        this.runningInService = runningInService;
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

    public Context publish(ServiceTarget target, String context, ClassLoader loader,
            Map<String, String> urlPatternToClassNameMap, JBossWebMetaData jbwmd, WebservicesMetaData metadata, JBossWebservicesMetaData jbwsMetadata)
            throws Exception {
        WSEndpointDeploymentUnit unit = new WSEndpointDeploymentUnit(loader, context, urlPatternToClassNameMap, jbwmd, metadata, jbwsMetadata);
        return new Context(context, publish(target, unit));
    }

    private static ServiceTarget getBaseTarget() {
        return WSServices.getContainerRegistry().getService(WSServices.CONFIG_SERVICE).getServiceContainer();
    }

    /**
     * Publishes the endpoints declared to the provided WSEndpointDeploymentUnit
     */
    public List<Endpoint> publish(ServiceTarget target, WSEndpointDeploymentUnit unit) throws Exception {
        List<DeploymentAspect> aspects = getDeploymentAspects();
        ClassLoader origClassLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Deployment dep = null;
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            WSDeploymentBuilder.getInstance().build(unit);
            dep = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
            dep.addAttachment(ServiceTarget.class, target);
            DeploymentAspectManager dam = new DeploymentAspectManagerImpl();
            dam.setDeploymentAspects(aspects);
            dam.deploy(dep);
            // [JBWS-3441] hack - fallback JAXWS invocation handler for dynamically generated deployments
            for (Endpoint ep : dep.getService().getEndpoints()) {
                synchronized(ep) {
                    ep.setState(EndpointState.STOPPED);
                    ep.setInvocationHandler(new InvocationHandlerJAXWS());
                    ep.setState(EndpointState.STARTED);
                }
            }
        } finally {
            if (dep != null) {
                dep.removeAttachment(ServiceTarget.class);
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origClassLoader);
        }
        Deployment deployment = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
        deployment.addAttachment(WebDeploymentController.class, startWebApp(host, unit)); //TODO simplify and use findChild later in destroy()/stopWebApp()
        return deployment.getService().getEndpoints();
    }

    private static WebDeploymentController startWebApp(WebHost host, WSEndpointDeploymentUnit unit) throws Exception {
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

            addServlets(jbwebMD, deployment);

            handle = host.addWebDeployment(deployment);
            handle.create();
        } catch (Exception e) {
            throw MESSAGES.createContextPhaseFailed(e);
        }
        try {
            handle.start();
        } catch (Exception e) {
            throw MESSAGES.startContextPhaseFailed(e);
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
        Deployment deployment = eps.get(0).getService().getDeployment();
        List<DeploymentAspect> aspects = getDeploymentAspects();
        try {
            stopWebApp(deployment.getAttachment(WebDeploymentController.class));
        } finally {
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
    }

    private static void stopWebApp(WebDeploymentController context) throws Exception {
        try {
            context.stop();
        } catch (Exception e) {
            throw MESSAGES.stopContextPhaseFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            throw MESSAGES.destroyContextPhaseFailed(e);
        }
    }

    private List<DeploymentAspect> getDeploymentAspects() {
        return runningInService ? DeploymentAspectsProvider.getSortedDeploymentAspects() : getPublisherDeploymentAspects();
    }

    private static synchronized List<DeploymentAspect> getPublisherDeploymentAspects() {
        if (depAspects == null) {
            depAspects = new LinkedList<DeploymentAspect>();
            final List<DeploymentAspect> serverAspects = DeploymentAspectsProvider.getSortedDeploymentAspects();
            // copy to replace the EndpointServiceDeploymentAspect
            for (DeploymentAspect aspect : serverAspects) {
                if (aspect instanceof EndpointServiceDeploymentAspect) {
                    final EndpointServiceDeploymentAspect a = (EndpointServiceDeploymentAspect) aspect;
                    EndpointServiceDeploymentAspect clone = (EndpointServiceDeploymentAspect) (a.clone());
                    clone.setStopServices(true);
                    depAspects.add(clone);
                } else {
                    depAspects.add(aspect);
                }
            }
        }
        return depAspects;
    }

}

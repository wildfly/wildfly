/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.web.deployment.WebCtxLoader;
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
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.DeploymentAspectManager;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.WSFServlet;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.wsf.spi.publish.Context;
import org.jboss.wsf.spi.publish.EndpointPublisher;

/**
 * WS endpoint publisher, allows for publishing a WS endpoint on AS 7
 *
 * @author alessio.soldano@jboss.com
 * @since 12-Jul-2011
 */
public final class EndpointPublisherImpl implements EndpointPublisher {

    private Host host;

    public EndpointPublisherImpl(Host host) {
        this.host = host;
    }

    @Override
    public Context publish(String context, ClassLoader loader, Map<String, String> urlPatternToClassNameMap) throws Exception {
        return publish(null, context, loader, urlPatternToClassNameMap, null);
    }

    public Context publish(String context, ClassLoader loader, Map<String, String> urlPatternToClassNameMap, WebservicesMetaData metadata) throws Exception {
        return publish(null, context, loader, urlPatternToClassNameMap, metadata);
    }

    public Context publish(ServiceTarget target, String context, ClassLoader loader, Map<String, String> urlPatternToClassNameMap, WebservicesMetaData metadata) throws Exception {
        WSEndpointDeploymentUnit unit = new WSEndpointDeploymentUnit(loader, context, urlPatternToClassNameMap, metadata);
        return new Context(context, publish(target, unit));
    }

    /**
     * Publishes the endpoints declared to the provided WSEndpointDeploymentUnit
     */
    public List<Endpoint> publish(ServiceTarget target, WSEndpointDeploymentUnit unit) throws Exception {
        List<DeploymentAspect> aspects = DeploymentAspectsProvider.getSortedDeploymentAspects();
        ClassLoader origClassLoader = SecurityActions.getContextClassLoader();
        Deployment dep = null;
        try {
            SecurityActions.setContextClassLoader(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
            WSDeploymentBuilder.getInstance().build(unit);
            dep = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
            dep.addAttachment(ServiceTarget.class, target);
            DeploymentAspectManager dam = new DeploymentAspectManagerImpl();
            dam.setDeploymentAspects(aspects);
            dam.deploy(dep);
        } finally {
            if (dep != null) {
                dep.removeAttachment(ServiceTarget.class);
            }
            SecurityActions.setContextClassLoader(origClassLoader);
        }
        Deployment deployment = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
        deployment.addAttachment(StandardContext.class, startWebApp(host, unit)); //TODO simplify and use findChild later in destroy()/stopWebApp()
        return deployment.getService().getEndpoints();
    }

    private static StandardContext startWebApp(Host host, WSEndpointDeploymentUnit unit) throws Exception {
        StandardContext context = new StandardContext();
        try {
            JBossWebMetaData jbwebMD = unit.getAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY);
            context.setPath(jbwebMD.getContextRoot());
            context.addLifecycleListener(new ContextConfig());
            ServerConfigService config = (ServerConfigService)unit.getServiceRegistry().getService(WSServices.CONFIG_SERVICE).getService();
            File docBase = new File(config.getValue().getServerTempDir(), jbwebMD.getContextRoot());
            if (!docBase.exists()) {
                docBase.mkdirs();
            }
            context.setDocBase(docBase.getPath());

            final Loader loader = new WebCtxLoader(unit.getAttachment(WSAttachmentKeys.CLASSLOADER_KEY));
            loader.setContainer(host);
            context.setLoader(loader);
            context.setInstanceManager(new LocalInstanceManager());

            addServlets(jbwebMD, context);

            host.addChild(context);
            context.create();
        } catch (Exception e) {
            throw MESSAGES.createContextPhaseFailed(e);
        }
        try {
            context.start();
        } catch (LifecycleException e) {
            throw MESSAGES.startContextPhaseFailed(e);
        }
        return context;
    }

    private static void addServlets(JBossWebMetaData jbwebMD, StandardContext context) {
        for (JBossServletMetaData smd : jbwebMD.getServlets()) {
            final String sc = smd.getServletClass();
            if (sc.equals(WSFServlet.class.getName())) {
                final String servletName = smd.getServletName();
                List<ParamValueMetaData> params = smd.getInitParam();
                List<String> urlPatterns = null;
                for (ServletMappingMetaData smmd : jbwebMD.getServletMappings()) {
                    if (smmd.getServletName().equals(servletName)) {
                        urlPatterns = smmd.getUrlPatterns();
                        break;
                    }
                }

                WSFServlet wsfs = new WSFServlet();
                Wrapper wsfsWrapper = context.createWrapper();
                wsfsWrapper.setName(servletName);
                wsfsWrapper.setServlet(wsfs);
                wsfsWrapper.setServletClass(WSFServlet.class.getName());
                for (ParamValueMetaData param : params) {
                    wsfsWrapper.addInitParameter(param.getParamName(), param.getParamValue());
                }
                context.addChild(wsfsWrapper);
                for (String urlPattern : urlPatterns) {
                    context.addServletMapping(urlPattern, servletName);
                }
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
        List<DeploymentAspect> aspects = DeploymentAspectsProvider.getSortedDeploymentAspects();
        try {
            stopWebApp(deployment.getAttachment(StandardContext.class));
        } finally {
            ClassLoader origClassLoader = SecurityActions.getContextClassLoader();
            try {
                SecurityActions.setContextClassLoader(ClassLoaderProvider.getDefaultProvider().getServerIntegrationClassLoader());
                DeploymentAspectManager dam = new DeploymentAspectManagerImpl();
                dam.setDeploymentAspects(aspects);
                dam.undeploy(deployment);
            } finally {
                SecurityActions.setContextClassLoader(origClassLoader);
            }
        }
    }

    private static void stopWebApp(StandardContext context) throws Exception {
        try {
            Container container = context.getParent();
            container.removeChild(context);
            context.stop();
        } catch (LifecycleException e) {
            throw MESSAGES.stopContextPhaseFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            throw MESSAGES.destroyContextPhaseFailed(e);
        }
    }

    private static class LocalInstanceManager implements InstanceManager {
        LocalInstanceManager() {
        }
        @Override
        public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            return Class.forName(className).newInstance();
        }

        @Override
        public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            return Class.forName(fqcn, false, classLoader).newInstance();
        }

        @Override
        public Object newInstance(Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
            return c.newInstance();
        }

        @Override
        public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
            throw new IllegalStateException();
        }

        @Override
        public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {
        }
    }
}

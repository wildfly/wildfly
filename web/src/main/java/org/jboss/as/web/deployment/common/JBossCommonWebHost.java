package org.jboss.as.web.deployment.common;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.jboss.as.web.host.CommonServletBuilder;
import org.jboss.as.web.host.CommonWebDeployment;
import org.jboss.as.web.host.CommonWebDeploymentBuilder;
import org.jboss.as.web.host.CommonWebHost;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class JBossCommonWebHost implements CommonWebHost, Service<CommonWebHost> {

    private final InjectedValue<VirtualHost> injectedHost = new InjectedValue<>();


    @Override
    public CommonWebDeployment addWebDeployment(final CommonWebDeploymentBuilder commonWebDeploymentBuilder) throws Exception {
        final Host host = injectedHost.getValue().getHost();
        final StandardContext context = new StandardContext();


        context.setPath(commonWebDeploymentBuilder.getContextRoot());
        context.addLifecycleListener(new ContextConfig());
        File docBase = commonWebDeploymentBuilder.getDocumentRoot();
        if (!docBase.exists()) {
            docBase.mkdirs();
        }
        context.setDocBase(docBase.getPath());

        final Loader loader = new WebCtxLoader(commonWebDeploymentBuilder.getClassLoader());
        loader.setContainer(host);
        context.setLoader(loader);
        context.setInstanceManager(new LocalInstanceManager());


        for (CommonServletBuilder servlet : commonWebDeploymentBuilder.getServlets()) {
            final String servletName = servlet.getServletName();
            Map<String, String> params = servlet.getInitParams();
            List<String> urlPatterns = new ArrayList<>(servlet.getUrlMappings());

            Wrapper wsfsWrapper = context.createWrapper();
            wsfsWrapper.setName(servletName);
            wsfsWrapper.setServlet(servlet.getServlet());
            wsfsWrapper.setServletClass(servlet.getServletClass().getName());
            for (Map.Entry<String, String> param : params.entrySet()) {
                wsfsWrapper.addInitParameter(param.getKey(), param.getValue());
            }
            context.addChild(wsfsWrapper);
            for (String urlPattern : urlPatterns) {
                context.addServletMapping(urlPattern, servletName);
            }
        }
        return new WebDeploymentImpl(context, host);
    }

    public InjectedValue<VirtualHost> getInjectedHost() {
        return injectedHost;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public CommonWebHost getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
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


    private static class WebDeploymentImpl implements CommonWebDeployment {

        private final StandardContext context;
        private final Host host;

        private WebDeploymentImpl(final StandardContext context, final Host host) {
            this.context = context;
            this.host = host;
        }

        @Override
        public void create() throws Exception {
            host.addChild(context);
            context.create();
        }

        @Override
        public void start() throws Exception {
            context.start();
        }

        @Override
        public void stop() throws Exception {
            context.stop();
        }

        @Override
        public void destroy() throws Exception {
            context.getParent().removeChild(context);
            context.destroy();
        }
    }
}


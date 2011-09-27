/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.remote;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.EJBReceiverContext;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Stuart Douglas
 */
public class LocalEjbReceiver extends EJBReceiver<Void> implements Service<LocalEjbReceiver> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localEjbReceiver");

    private final List<EJBReceiverContext> contexts = new CopyOnWriteArrayList<EJBReceiverContext>();
    private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();

    @Override
    protected void associate(final EJBReceiverContext context) {
        this.contexts.add(context);
    }

    @Override
    protected Future<?> processInvocation(final EJBClientInvocationContext<Void> invocation) throws Exception {
        final EjbDeploymentInformation ejb = findBean(invocation.getAppName(), invocation.getModuleName(), invocation.getDistinctName(), invocation.getBeanName());
        //TODO: we need a better way to get the correct view
        final Class<?> viewClass = invocation.getViewClass();
        final ComponentView view = ejb.getView(viewClass.getName());
        if (view == null) {
            throw new RuntimeException("Could not find view " + viewClass + " for ejb " + ejb.getEjbName());
        }

        //TODO: this needs to be pass by value
        //we really need to setup client interceptors to handle this somehow

        final InterceptorContext context = new InterceptorContext();
        context.setParameters(invocation.getParameters());
        context.setMethod(invocation.getInvokedMethod());
        context.setTarget(invocation.getInvokedProxy());
        context.setContextData(new HashMap<String, Object>());
        final Object result = view.invoke(context);
        return new Future<Object>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                return result;
            }

            @Override
            public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return result;
            }
        };
    }

    @Override
    protected byte[] openSession(final String appName, final String moduleName, final String distinctName, final String beanName) throws Exception {
        final EjbDeploymentInformation ejbInfo = findBean(appName, moduleName, distinctName, beanName);
        final EJBComponent component = ejbInfo.getEjbComponent();
        if (component instanceof StatefulSessionComponent) {
            final StatefulSessionComponent stateful = (StatefulSessionComponent) component;
            return stateful.createSession();
        } else {
            throw new IllegalArgumentException("EJB " + beanName + " is not a Stateful Session bean in app: " + appName + " module: " + moduleName + " distinct name:" + distinctName);
        }
    }

    @Override
    protected void verify(final String appName, final String moduleName, final String distinctName, final String beanName) throws Exception {
        findBean(appName, moduleName, distinctName, beanName);
    }

    private EjbDeploymentInformation findBean(final String appName, final String moduleName, final String distinctName, final String beanName) {
        final ModuleDeployment module = deploymentRepository.getValue().getModules().get(new DeploymentModuleIdentifier(appName, moduleName, distinctName));
        if (module == null) {
            throw new IllegalArgumentException("Could not find module app: " + appName + " module: " + moduleName + " distinct name:" + distinctName);
        }
        final EjbDeploymentInformation ejbInfo = module.getEjbs().get(beanName);
        if (ejbInfo == null) {
            throw new IllegalArgumentException("Could not find ejb " + beanName + " in app: " + appName + " module: " + moduleName + " distinct name:" + distinctName);
        }
        return ejbInfo;
    }

    @Override
    protected Void createReceiverSpecific() {
        return null;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {
        for (EJBReceiverContext ctx : contexts) {
            ctx.close();
        }
        this.contexts.clear();
    }

    @Override
    public LocalEjbReceiver getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepository() {
        return deploymentRepository;
    }
}

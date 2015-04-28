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

package org.jboss.as.pojo.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.descriptor.BeanMetaDataConfig;
import org.jboss.as.pojo.descriptor.CallbackConfig;
import org.jboss.as.pojo.descriptor.ConfigVisitor;
import org.jboss.as.pojo.descriptor.DefaultConfigVisitor;
import org.jboss.as.pojo.descriptor.InstallConfig;
import org.jboss.as.pojo.descriptor.ValueConfig;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Abstract pojo phase; it handles install/uninstall
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractPojoPhase implements Service<Object> {

    private Module module;
    private BeanMetaDataConfig beanConfig;
    private DeploymentReflectionIndex index;
    private BeanInfo beanInfo;
    private Object bean;

    protected abstract BeanState getLifecycleState();

    protected abstract AbstractPojoPhase createNextPhase();

    public void start(StartContext context) throws StartException {
        if (module != null) {
            final ClassLoader previous = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            try {
                startInternal(context);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previous);
            }
        } else {
            startInternal(context);
        }
    }

    protected void startInternal(StartContext context) throws StartException {
        try {
            executeInstalls();

            // only after describe do we have a bean
            if (getLifecycleState().isAfter(BeanState.DESCRIBED)) {
                addCallbacks(true);
                addCallbacks(false);

                ServiceRegistry registry = context.getController().getServiceContainer();
                InstancesService.addInstance(registry, context.getChildTarget(), getLifecycleState(), getBean());
            }

            final AbstractPojoPhase nextPhase = createNextPhase(); // do we have a next phase
            if (nextPhase != null) {
                final BeanState state = getLifecycleState();
                final BeanState next = state.next();
                final BeanMetaDataConfig beanConfig = getBeanConfig();
                final ServiceName name = BeanMetaDataConfig.toBeanName(beanConfig.getName(), next);
                final ServiceTarget serviceTarget = context.getChildTarget();
                final ServiceBuilder serviceBuilder = serviceTarget.addService(name, nextPhase);
                registerAliases(serviceBuilder, next);
                final ConfigVisitor visitor = new DefaultConfigVisitor(serviceBuilder, state, getModule(), getIndex(), getBeanInfo());
                beanConfig.visit(visitor);
                nextPhase.setModule(getModule());
                nextPhase.setBeanConfig(getBeanConfig());
                nextPhase.setIndex(getIndex());
                nextPhase.setBeanInfo(getBeanInfo());
                nextPhase.setBean(getBean());
                serviceBuilder.install();
            }
        } catch (Throwable t) {
            throw new StartException(t);
        }
    }

    protected void registerAliases(ServiceBuilder serviceBuilder, BeanState next) {
        final Set<String> aliases = beanConfig.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                ServiceName asn = BeanMetaDataConfig.toBeanName(alias, next);
                serviceBuilder.addAliases(asn);
            }
        }
    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return getBean();
    }

    public void stop(StopContext context) {
        if (module != null) {
            final ClassLoader previous = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            try {
                stopInternal(context);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(previous);
            }
        } else {
            stopInternal(context);
        }
    }

    protected void stopInternal(StopContext context) {
        if (getLifecycleState().isAfter(BeanState.DESCRIBED)) {
            InstancesService.removeInstance(context.getController().getServiceContainer(), getLifecycleState(), getBean());

            removeCallbacks(true);
            removeCallbacks(false);
        }
        executeUninstalls();
    }

    private List<Joinpoint> getInstalls() {
        List<InstallConfig> installs = getBeanConfig().getInstalls();
        return (installs != null) ? toJoinpoints(installs) : Collections.<Joinpoint>emptyList();
    }

    private List<Joinpoint> getUninstalls() {
        List<InstallConfig> uninstalls = getBeanConfig().getUninstalls();
        return (uninstalls != null) ? toJoinpoints(uninstalls) : Collections.<Joinpoint>emptyList();
    }

    private List<Joinpoint> toJoinpoints(List<InstallConfig> installs) {
        List<Joinpoint> joinpoints = new ArrayList<Joinpoint>();
        for (InstallConfig ic : installs) {
            if (ic.getWhenRequired() == getLifecycleState())
                joinpoints.add(createJoinpoint(ic));
        }
        return joinpoints;
    }

    protected Joinpoint createJoinpoint(InstallConfig config) {
        String methodName = config.getMethodName();
        if (methodName == null)
            throw PojoLogger.ROOT_LOGGER.nullMethodName();

        ValueConfig[] parameters = config.getParameters();
        String[] types = Configurator.getTypes(parameters);
        String dependency = config.getDependency();
        Value<Object> target = (dependency != null) ? config.getBean() : new ImmediateValue<Object>(getBean());
        BeanInfo beanInfo = (dependency != null) ? config.getBeanInfo().getValue() : getBeanInfo();
        Method method = beanInfo.findMethod(methodName, types);
        MethodJoinpoint joinpoint = new MethodJoinpoint(method);
        joinpoint.setTarget(target);
        joinpoint.setParameters(parameters);
        return joinpoint;
    }

    protected void executeInstalls() throws StartException {
        List<Joinpoint> installs = getInstalls();
        if (installs.isEmpty())
            return;

        int i = 0;
        try {
            for (i = 0; i < installs.size(); i++)
                installs.get(i).dispatch();
        } catch (Throwable t) {
            considerUninstalls(getUninstalls(), i);
            throw new StartException(t);
        }
    }

    /**
     * Consider the uninstalls.
     * <p/>
     * This method is here to be able to override
     * the behavior after installs failed.
     * e.g. perhaps only running uninstalls from the index.
     * <p/>
     * By default we run all uninstalls in the case
     * at least one install failed.
     *
     * @param uninstalls the uninstalls
     * @param index      current installs index
     */
    protected void considerUninstalls(List<Joinpoint> uninstalls, int index) {
        if (uninstalls == null)
            return;

        for (int j = Math.min(index, uninstalls.size() - 1); j >= 0; j--) {
            try {
                uninstalls.get(j).dispatch();
            } catch (Throwable t) {
                PojoLogger.ROOT_LOGGER.ignoreUninstallError(uninstalls.get(j), t);
            }
        }
    }

    protected void executeUninstalls() {
        considerUninstalls(getUninstalls(), Integer.MAX_VALUE);
    }

    protected void addCallbacks(boolean install) {
        List<CallbackConfig> configs = (install ? getBeanConfig().getIncallbacks() : getBeanConfig().getUncallbacks());
        if (configs != null) {
            for (CallbackConfig cc : configs) {
                if (cc.getWhenRequired() == getLifecycleState()) {
                    Callback callback = new Callback(getBeanInfo(), getBean(), cc);
                    if (install)
                        InstancesService.addIncallback(callback);
                    else
                        InstancesService.addUncallback(callback);
                }
            }
        }
    }

    protected void removeCallbacks(boolean install) {
        List<CallbackConfig> configs = (install ? getBeanConfig().getIncallbacks() : getBeanConfig().getUncallbacks());
        if (configs != null) {
            for (CallbackConfig cc : configs) {
                if (cc.getWhenRequired() == getLifecycleState()) {
                    Callback callback = new Callback(getBeanInfo(), getBean(), cc);
                    if (install)
                        InstancesService.removeIncallback(callback);
                    else
                        InstancesService.removeUncallback(callback);
                }
            }
        }
    }

    protected Module getModule() {
        return module;
    }

    protected void setModule(Module module) {
        this.module = module;
    }

    protected BeanMetaDataConfig getBeanConfig() {
        return beanConfig;
    }

    protected DeploymentReflectionIndex getIndex() {
        return index;
    }

    protected void setBeanConfig(BeanMetaDataConfig beanConfig) {
        this.beanConfig = beanConfig;
    }

    protected void setIndex(DeploymentReflectionIndex index) {
        this.index = index;
    }

    protected BeanInfo getBeanInfo() {
        return beanInfo;
    }

    protected void setBeanInfo(BeanInfo beanInfo) {
        this.beanInfo = beanInfo;
    }

    protected Object getBean() {
        return bean;
    }

    protected void setBean(Object bean) {
        this.bean = bean;
    }
}

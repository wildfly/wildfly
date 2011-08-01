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

package org.jboss.as.mc.service;

import org.jboss.as.mc.BeanState;
import org.jboss.as.mc.descriptor.BeanMetaDataConfig;
import org.jboss.as.mc.descriptor.ConfigVisitor;
import org.jboss.as.mc.descriptor.DefaultConfigVisitor;
import org.jboss.as.mc.descriptor.InstallConfig;
import org.jboss.as.mc.descriptor.LifecycleConfig;
import org.jboss.as.mc.descriptor.ValueConfig;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;

/**
 * Abstract MC pojo phase; it handles install/uninstall
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractPojoPhase implements Service {
    protected final Logger log = Logger.getLogger(getClass());

    private final InjectedValue<Module> module = new InjectedValue<Module>();
    private final InjectedValue<BeanMetaDataConfig> beanConfig = new InjectedValue<BeanMetaDataConfig>();
    private final InjectedValue<BeanInfo> beanInfo = new InjectedValue<BeanInfo>();
    private final InjectedValue<Object> bean = new InjectedValue<Object>();

    private InjectedValue<Joinpoint>[] installs;
    private InjectedValue<Joinpoint>[] uninstalls;

    protected abstract BeanState getLifecycleState();
    protected abstract AbstractPojoPhase createNextPhase();

    public void start(StartContext context) throws StartException {
        try {
            executeInstalls();

            final AbstractPojoPhase nextPhase = createNextPhase(); // do we have a next phase
            if (nextPhase != null) {
                final BeanState state = getLifecycleState();
                final BeanMetaDataConfig beanConfig = getBeanConfig().getValue();
                final ServiceName name = BeanMetaDataConfig.JBOSS_MC_POJO.append(beanConfig.getName()).append(state.next().name());
                final ServiceTarget serviceTarget = context.getChildTarget();
                final ServiceBuilder serviceBuilder = serviceTarget.addService(name, nextPhase);
                final ConfigVisitor visitor = new DefaultConfigVisitor(serviceBuilder, state, module.getValue().getClassLoader());
                beanConfig.visit(visitor);
                nextPhase.getModule().setValue(new ImmediateValue<Module>(getModule().getValue()));
                nextPhase.getBeanConfig().setValue(new ImmediateValue<BeanMetaDataConfig>(beanConfig));
                nextPhase.getBeanInfo().setValue(new ImmediateValue<BeanInfo>(getBeanInfo().getValue()));
                nextPhase.getBean().setValue(new ImmediateValue<Object>(getBean().getValue()));
                serviceBuilder.install();
            }

        } catch (Throwable t) {
            throw new StartException(t);
        }
    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return getBean().getValue();
    }

    public void stop(StopContext context) {
        executeUninstalls();
    }

    protected Joinpoint createJoinpoint(InstallConfig config) {
        String methodName = config.getMethodName();
        if (methodName == null)
            throw new IllegalArgumentException("Null method name");

        ValueConfig[] parameters = config.getParameters();
        String[] types = Configurator.getTypes(parameters);
        String dependency = config.getDependency();
        InjectedValue<Object> target = (dependency != null) ? config.getBean() : getBean();
        BeanInfo beanInfo = (dependency != null) ? config.getBeanInfo().getValue() : getBeanInfo().getValue();
        Method method = beanInfo.findMethod(methodName, types);
        InjectedValue<Object>[] params = Configurator.getValues(parameters);
        MethodJoinpoint joinpoint = new MethodJoinpoint(method);
        joinpoint.setTarget(target);
        joinpoint.setParameters(params);
        return joinpoint;
    }

    protected void executeInstalls() throws StartException {
        if (installs == null || installs.length == 0)
            return;

        int i = 0;
        try {
            for (i = 0; i < installs.length; i++)
                installs[i].getValue().dispatch();
        } catch (Throwable t) {
            considerUninstalls(uninstalls, i);
            throw new StartException(t);
        }
    }

    /**
     * Consider the uninstalls.
     *
     * This method is here to be able to override
     * the behavior after installs failed.
     * e.g. perhaps only running uninstalls from the index.
     *
     * By default we run all uninstalls in the case
     * at least one install failed.
     *
     * @param uninstalls the uninstalls
     * @param index current installs index
     */
    protected void considerUninstalls(InjectedValue<Joinpoint>[] uninstalls, int index) {
        if (uninstalls == null)
            return;

        for (int j = Math.min(index, uninstalls.length - 1); j >= 0; j--) {
            try {
                uninstalls[j].getValue().dispatch();
            } catch (Throwable t) {
                log.warn("Ignoring uninstall action on target: " + uninstalls[j], t);
            }
        }
    }

    protected void executeUninstalls() {
        considerUninstalls(uninstalls, Integer.MAX_VALUE);
    }

    protected InjectedValue<Module> getModule() {
        return module;
    }

    protected InjectedValue<BeanMetaDataConfig> getBeanConfig() {
        return beanConfig;
    }

    protected InjectedValue<BeanInfo> getBeanInfo() {
        return beanInfo;
    }

    protected InjectedValue<Object> getBean() {
        return bean;
    }

    public void setInstalls(InjectedValue<Joinpoint>[] installs) {
        this.installs = installs;
    }

    public void setUninstalls(InjectedValue<Joinpoint>[] uninstalls) {
        this.uninstalls = uninstalls;
    }
}

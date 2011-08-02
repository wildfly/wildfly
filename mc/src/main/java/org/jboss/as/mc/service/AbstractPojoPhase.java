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
import org.jboss.msc.value.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Abstract MC pojo phase; it handles install/uninstall
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractPojoPhase implements Service<Object> {
    protected final Logger log = Logger.getLogger(getClass());

    private Module module;
    private BeanMetaDataConfig beanConfig;
    private BeanInfo beanInfo;
    private Object bean;

    protected abstract BeanState getLifecycleState();
    protected abstract AbstractPojoPhase createNextPhase();

    public void start(StartContext context) throws StartException {
        try {
            executeInstalls();

            final AbstractPojoPhase nextPhase = createNextPhase(); // do we have a next phase
            if (nextPhase != null) {
                final BeanState state = getLifecycleState();
                final BeanState next = state.next();
                final BeanMetaDataConfig beanConfig = getBeanConfig();
                final ServiceName name = BeanMetaDataConfig.toBeanName(beanConfig.getName(), next);
                final ServiceTarget serviceTarget = context.getChildTarget();
                final ServiceBuilder serviceBuilder = serviceTarget.addService(name, nextPhase);
                final Set<String> aliases = beanConfig.getAliases();
                if (aliases != null) {
                    for (String alias : aliases) {
                        ServiceName asn = BeanMetaDataConfig.toBeanName(alias, next);
                        serviceBuilder.addAliases(asn);
                    }
                }
                final ConfigVisitor visitor = new DefaultConfigVisitor(serviceBuilder, state, module);
                beanConfig.visit(visitor);
                nextPhase.setModule(getModule());
                nextPhase.setBeanConfig(getBeanConfig());
                nextPhase.setBeanInfo(getBeanInfo());
                nextPhase.setBean(getBean());
                serviceBuilder.install();
            }

        } catch (Throwable t) {
            throw new StartException(t);
        }
    }

    @Override
    public Object getValue() throws IllegalStateException, IllegalArgumentException {
        return getBean();
    }

    public void stop(StopContext context) {
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
            throw new IllegalArgumentException("Null method name");

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
    protected void considerUninstalls(List<Joinpoint> uninstalls, int index) {
        if (uninstalls == null)
            return;

        for (int j = Math.min(index, uninstalls.size() - 1); j >= 0; j--) {
            try {
                uninstalls.get(j).dispatch();
            } catch (Throwable t) {
                log.warn("Ignoring uninstall action on target: " + uninstalls.get(j), t);
            }
        }
    }

    protected void executeUninstalls() {
        considerUninstalls(getUninstalls(), Integer.MAX_VALUE);
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

    protected void setBeanConfig(BeanMetaDataConfig beanConfig) {
        this.beanConfig = beanConfig;
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

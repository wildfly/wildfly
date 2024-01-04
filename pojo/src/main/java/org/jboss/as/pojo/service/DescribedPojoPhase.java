/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.descriptor.BeanMetaDataConfig;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * POJO described phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DescribedPojoPhase extends AbstractPojoPhase {
    public DescribedPojoPhase(DeploymentReflectionIndex index, BeanMetaDataConfig beanConfig) {
        setIndex(index);
        setBeanConfig(beanConfig);
    }

    /**
     * Expose alias registration against service builder.
     *
     * @param serviceBuilder the service builder
     */
    public void registerAliases(ServiceBuilder serviceBuilder) {
        registerAliases(serviceBuilder, getLifecycleState());
    }

    @Override
    protected BeanState getLifecycleState() {
        return BeanState.DESCRIBED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new InstantiatedPojoPhase(this);
    }

    @SuppressWarnings("unchecked")
    protected void startInternal(StartContext context) throws StartException {
        try {
            setModule(getBeanConfig().getModule().getInjectedModule().getValue());
            String beanClass = getBeanConfig().getBeanClass();
            if (beanClass != null) {
                Class clazz = Class.forName(beanClass, false, getModule().getClassLoader());
                setBeanInfo(new DefaultBeanInfo(getIndex(), clazz));
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
        super.startInternal(context);
    }

    public BeanInfo getValue() throws IllegalStateException, IllegalArgumentException {
        BeanInfo beanInfo = getBeanInfo();
        if (beanInfo == null)
            throw new IllegalStateException(PojoLogger.ROOT_LOGGER.missingBeanInfo(getBeanConfig()));
        return beanInfo;
    }
}

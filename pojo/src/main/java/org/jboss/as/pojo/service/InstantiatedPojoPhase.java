/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * POJO instantiated phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InstantiatedPojoPhase extends AbstractPojoPhase {
    private final DescribedPojoPhase describedPojoPhase;

    public InstantiatedPojoPhase(DescribedPojoPhase describedPojoPhase) {
        this.describedPojoPhase = describedPojoPhase;
    }

    @Override
    protected BeanState getLifecycleState() {
        return BeanState.INSTANTIATED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new ConfiguredPojoPhase();
    }

    @Override
    protected void startInternal(StartContext context) throws StartException {
        try {
            BeanInfo beanInfo = getBeanInfo();
            setBean(BeanUtils.instantiateBean(getBeanConfig(), beanInfo, getIndex(), getModule()));
            if (beanInfo == null) {
                //noinspection unchecked
                beanInfo = new DefaultBeanInfo(getIndex(), getBean().getClass());
                setBeanInfo(beanInfo);
                // set so describe service has its value
                describedPojoPhase.setBeanInfo(beanInfo);
            }
        } catch (StartException t) {
            throw t;
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.startInternal(context);
    }
}

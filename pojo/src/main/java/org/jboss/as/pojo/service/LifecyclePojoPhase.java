/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.descriptor.LifecycleConfig;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * POJO lifecycle phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class LifecyclePojoPhase extends AbstractPojoPhase {
    protected abstract LifecycleConfig getUpConfig();
    protected abstract LifecycleConfig getDownConfig();
    protected abstract String defaultUp();
    protected abstract String defaultDown();

    protected void dispatchJoinpoint(LifecycleConfig config, String defaultMethod) throws Throwable {
        BeanUtils.dispatchLifecycleJoinpoint(getBeanInfo(), getBean(), config, defaultMethod);
    }

    @Override
    protected void startInternal(StartContext context) throws StartException {
        try {
            dispatchJoinpoint(getUpConfig(), defaultUp());
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.startInternal(context);
    }

    @Override
    protected void stopInternal(StopContext context) {
        super.stopInternal(context);
        try {
            dispatchJoinpoint(getDownConfig(), defaultDown());
        } catch (Throwable t) {
            PojoLogger.ROOT_LOGGER.debugf(t, "Exception at %s phase.", defaultDown());
        }
    }
}

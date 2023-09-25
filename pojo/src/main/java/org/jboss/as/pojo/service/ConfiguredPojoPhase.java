/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * POJO configured phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConfiguredPojoPhase extends AbstractPojoPhase {
    @Override
    protected BeanState getLifecycleState() {
        return BeanState.CONFIGURED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new CreateDestroyPojoPhase();
    }

    protected void configure(boolean nullify) throws Throwable {
        BeanUtils.configure(getBeanConfig(), getBeanInfo(), getModule(), getBean(), nullify);
    }

    @Override
    protected void startInternal(StartContext context) throws StartException {
        try {
            configure(false);
        } catch (StartException t) {
            throw t;
        } catch (Throwable t) {
            throw new StartException(t);
        }
        super.startInternal(context);
    }

    @Override
    protected void stopInternal(StopContext context) {
        super.stopInternal(context);
        try {
            configure(true);
        } catch (Throwable ignored) {
        }
    }
}

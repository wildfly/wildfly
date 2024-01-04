/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.descriptor.LifecycleConfig;

/**
 * POJO create/destroy phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CreateDestroyPojoPhase extends LifecyclePojoPhase {
    @Override
    protected BeanState getLifecycleState() {
        return BeanState.CREATE;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new StartStopPojoPhase();
    }

    @Override
    protected LifecycleConfig getUpConfig() {
        return getBeanConfig().getCreate();
    }

    @Override
    protected LifecycleConfig getDownConfig() {
        return getBeanConfig().getDestroy();
    }

    @Override
    protected String defaultUp() {
        return "create";
    }

    @Override
    protected String defaultDown() {
        return "destroy";
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;
import org.jboss.as.pojo.descriptor.LifecycleConfig;

/**
 * POJO start/stop phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class StartStopPojoPhase extends LifecyclePojoPhase {
    @Override
    protected BeanState getLifecycleState() {
        return BeanState.START;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return new InstalledPojoPhase();
    }
    @Override
    protected LifecycleConfig getUpConfig() {
        return getBeanConfig().getStart();
    }

    @Override
    protected LifecycleConfig getDownConfig() {
        return getBeanConfig().getStop();
    }

    @Override
    protected String defaultUp() {
        return "start";
    }

    @Override
    protected String defaultDown() {
        return "stop";
    }
}

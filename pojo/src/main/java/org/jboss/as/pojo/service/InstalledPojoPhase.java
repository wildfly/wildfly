/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.service;

import org.jboss.as.pojo.BeanState;

/**
 * POJO installed phase.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class InstalledPojoPhase extends AbstractPojoPhase {
    @Override
    protected BeanState getLifecycleState() {
        return BeanState.INSTALLED;
    }

    @Override
    protected AbstractPojoPhase createNextPhase() {
        return null;
    }
}

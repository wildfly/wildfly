/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

import jakarta.ejb.Local;
import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Local(Synchronizations.class)
@Stateful
public class SynchronizationsBean implements LocalSynchronizations {
    @Override
    public void destroy() {

    }

    @Override
    public void register() {
    }
}

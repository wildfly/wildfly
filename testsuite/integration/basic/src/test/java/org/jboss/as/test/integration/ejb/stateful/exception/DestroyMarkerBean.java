/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.exception;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

@Singleton
@Remote
@LocalBean
public class DestroyMarkerBean implements DestroyMarkerBeanInterface {
    private boolean preDestroy = false;

    public boolean is() {
        return preDestroy;
    }

    public void set(boolean preDestroy) {
        this.preDestroy = preDestroy;
    }
}

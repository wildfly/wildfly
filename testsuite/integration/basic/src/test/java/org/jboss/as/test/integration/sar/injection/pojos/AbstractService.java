/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.pojos;

/**
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
abstract class AbstractService {

    private A a;
    private int injectedCount;

    public final void setA(final A a) {
        this.a = a;
    }

    public final int getCount() {
        return a.getCount();
    }

    public final int getInjectedCount() {
        return injectedCount;
    }

    public void setInjectedCount(int injectedCount) {
        this.injectedCount = injectedCount;
    }

    public final boolean getStartCalled() {
        return a.getStartCalled();
    }

    public final boolean getStopCalled() {
        return a.getStopCalled();
    }

    public final boolean getCreateCalled() {
        return a.getCreateCalled();
    }

    public final boolean getDestroyCalled() {
        return a.getDestroyCalled();
    }

}

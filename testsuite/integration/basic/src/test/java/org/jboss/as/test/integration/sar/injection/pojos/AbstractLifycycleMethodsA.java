/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.pojos;

/**
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
abstract class AbstractLifycycleMethodsA {

    private boolean startCalled;
    private boolean stopCalled;
    private boolean createCalled;
    private boolean destroyCalled;

    public final void start() {
        startCalled = true;
    }

    public final void stop() {
        stopCalled = true;
    }

    public final void create() {
        createCalled = true;
    }

    public final void destroy() {
        destroyCalled = true;
    }

    public final boolean getStartCalled() {
        return startCalled;
    }

    public final boolean getStopCalled() {
        return stopCalled;
    }

    public final boolean getCreateCalled() {
        return createCalled;
    }

    public final boolean getDestroyCalled() {
        return destroyCalled;
    }

}

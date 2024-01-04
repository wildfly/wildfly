/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PrePersist;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.Bravo;

public class MyListener {

    private static boolean injectionPerformed = false;

    private static volatile int invocationCount = 0;

    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @PersistenceContext(unitName = "onephasePU")
    EntityManager onephaseEm;

    @Inject
    private Alpha alpha;

    private Bravo bravo;

    public static int getInvocationCount() {
        return invocationCount;
    }

    public static void setInvocationCount(int invocationCount) {
        MyListener.invocationCount = invocationCount;
    }

    @Inject
    public void setBravo(Bravo bravo) {
        this.bravo = bravo;
    }

    @PostConstruct
    public void checkInjectionPerformed() {
        injectionPerformed = alpha != null && bravo != null && em != null && onephaseEm!=null;
    }

    public static boolean isIjectionPerformed() {
        return injectionPerformed;
    }

    @PrePersist
    @Interceptors(MyListenerInterceptor.class)
    public void onEntityCallback(Object entity) {
        invocationCount++;
    }
}

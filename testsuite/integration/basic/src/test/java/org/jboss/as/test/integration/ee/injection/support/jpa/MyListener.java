/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ee.injection.support.jpa;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;

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

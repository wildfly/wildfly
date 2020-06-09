/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.as.test.shared.TimeoutUtil;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author emmartins
 */
@Startup
@Singleton
public class TestResultsBean implements TestResults {

    private CountDownLatch latch;

    private boolean cdiBeanInitialized;
    private boolean cdiBeanBeforeDestroyed;
    private boolean cdiBeanDestroyed;

    private boolean ejbBeanInitialized;
    private boolean ejbBeanBeforeDestroyed;
    private boolean ejbBeanDestroyed;

    private boolean servletInitialized;
    private boolean servletBeforeDestroyed;
    private boolean servletDestroyed;

    public boolean isCdiBeanInitialized() {
        return cdiBeanInitialized;
    }

    public void setCdiBeanInitialized(boolean cdiBeanInitialized) {
        this.cdiBeanInitialized = cdiBeanInitialized;
        latch.countDown();
    }

    public boolean isEjbBeanInitialized() {
        return ejbBeanInitialized;
    }

    public void setEjbBeanInitialized(boolean ejbBeanInitialized) {
        this.ejbBeanInitialized = ejbBeanInitialized;
        latch.countDown();
    }

    public boolean isServletInitialized() {
        return servletInitialized;
    }

    public void setServletInitialized(boolean servletInitialized) {
        this.servletInitialized = servletInitialized;
        latch.countDown();
    }

    // ---

    public boolean isCdiBeanBeforeDestroyed() {
        return cdiBeanBeforeDestroyed;
    }

    public void setCdiBeanBeforeDestroyed(boolean cdiBeanBeforeDestroyed) {
        this.cdiBeanBeforeDestroyed = cdiBeanBeforeDestroyed;
        latch.countDown();
    }

    public boolean isEjbBeanBeforeDestroyed() {
        return ejbBeanBeforeDestroyed;
    }

    public void setEjbBeanBeforeDestroyed(boolean ejbBeanBeforeDestroyed) {
        this.ejbBeanBeforeDestroyed = ejbBeanBeforeDestroyed;
        latch.countDown();
    }

    public boolean isServletBeforeDestroyed() {
        return servletBeforeDestroyed;
    }

    public void setServletBeforeDestroyed(boolean servletBeforeDestroyed) {
        this.servletBeforeDestroyed = servletBeforeDestroyed;
        latch.countDown();
    }

    // ---

    public boolean isCdiBeanDestroyed() {
        return cdiBeanDestroyed;
    }

    public void setCdiBeanDestroyed(boolean cdiBeanDestroyed) {
        this.cdiBeanDestroyed = cdiBeanDestroyed;
        latch.countDown();
    }

    public boolean isEjbBeanDestroyed() {
        return ejbBeanDestroyed;
    }

    public void setEjbBeanDestroyed(boolean ejbBeanDestroyed) {
        this.ejbBeanDestroyed = ejbBeanDestroyed;
        latch.countDown();
    }

    public boolean isServletDestroyed() {
        return servletDestroyed;
    }

    public void setServletDestroyed(boolean servletDestroyed) {
        this.servletDestroyed = servletDestroyed;
        latch.countDown();
    }

    // --

    public void setup(int latchCount) {
        if (latch != null) {
            throw new IllegalStateException();
        }
        latch = new CountDownLatch(latchCount);
    }

    public void await(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (latch == null) {
            throw new IllegalStateException();
        }
        try {
            latch.await(TimeoutUtil.adjust(Long.valueOf(timeout).intValue()), timeUnit);
        } finally {
            latch = null;
        }
    }
}

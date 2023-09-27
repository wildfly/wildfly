/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import jakarta.ejb.Remote;
import java.util.concurrent.TimeUnit;

/**
 * @author emmartins
 */
@Remote
public interface TestResults {

    boolean isCdiBeanInitialized();

    void setCdiBeanInitialized(boolean cdiBeanInitialized);

    boolean isEjbBeanInitialized();

    void setEjbBeanInitialized(boolean ejbBeanInitialized);

    boolean isServletInitialized();

    void setServletInitialized(boolean servletInitialized);


    boolean isCdiBeanBeforeDestroyed();

    void setCdiBeanBeforeDestroyed(boolean cdiBeanBeforeDestroyed);

    boolean isEjbBeanBeforeDestroyed();

    void setEjbBeanBeforeDestroyed(boolean ejbBeanBeforeDestroyed);

    boolean isServletBeforeDestroyed();

    void setServletBeforeDestroyed(boolean servletBeforeDestroyed);


    boolean isCdiBeanDestroyed();

    void setCdiBeanDestroyed(boolean cdiBeanDestroyed);

    boolean isEjbBeanDestroyed();

    void setEjbBeanDestroyed(boolean ejbBeanDestroyed);

    boolean isServletDestroyed();

    void setServletDestroyed(boolean servletDestroyed);

    void setup(int latchCount);

    void await(long timeout, TimeUnit timeUnit) throws InterruptedException;
}

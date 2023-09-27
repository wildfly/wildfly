/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.dependencies.ear;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.logging.Logger;

/**
 * A Singleton EJB - implementation of {@link LogAccess} interface.
 *
 * @author Josef Cacek
 */
@Singleton
@Startup
@Remote(LogAccess.class)
public class LogAccessBean implements LogAccess {

    private static Logger LOGGER = Logger.getLogger(LogAccessBean.class);

    // Public methods --------------------------------------------------------

    /**
     * Lifecycle hook.
     */
    @PostConstruct
    @PreDestroy
    public void logLifecycleAction() {
        LOGGER.trace("logLifecycleAction");
        Log.SB.append(getClass().getSimpleName());
    }

    /**
     * Returns {@link Log#SB} content.
     *
     * @return
     * @see org.jboss.as.test.integration.deployment.dependencies.ear.LogAccess#getLog()
     */
    public String getLog() {
        return Log.SB.toString();
    }

}

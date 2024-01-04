/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.incorrect;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import org.jboss.logging.Logger;

/**
 * A SimpleBean.
 *
 * @author Josef Cacek
 */
@Stateless
@LocalBean
public class SimpleBean {

    private static Logger LOGGER = Logger.getLogger(SimpleBean.class);

    // Public methods --------------------------------------------------------

    /**
     * Simply returns given String.
     *
     * @param message
     * @return
     * @see org.jboss.as.test.integration.ejb.container.interceptor.FlowTracker#echo(java.lang.String)
     */
    public String echo(final String message) {
        LOGGER.trace("echo() called");
        return message;
    }
}

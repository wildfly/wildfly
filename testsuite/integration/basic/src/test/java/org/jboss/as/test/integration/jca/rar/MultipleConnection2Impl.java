/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;

/**
 * MultipleConnection2Impl
 *
 * @version $Revision: $
 */
public class MultipleConnection2Impl implements MultipleConnection2 {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("MultipleConnection2Impl");

    /**
     * ManagedConnection
     */
    private MultipleManagedConnection2 mc;

    /**
     * ManagedConnectionFactory
     */
    private MultipleManagedConnectionFactory2 mcf;

    /**
     * Default constructor
     *
     * @param mc  MultipleManagedConnection2
     * @param mcf MultipleManagedConnectionFactory2
     */
    public MultipleConnection2Impl(MultipleManagedConnection2 mc, MultipleManagedConnectionFactory2 mcf) {
        this.mc = mc;
        this.mcf = mcf;
    }

    /**
     * Call test
     *
     * @param s String
     * @return String
     */
    public String test(String s) {
        log.trace("test()");
        return null;

    }

    /**
     * Close
     */
    public void close() {
        mc.closeHandle(this);
    }

}

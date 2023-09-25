/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import org.jboss.logging.Logger;

/**
 * AnnoConnectionImpl
 *
 * @version $Revision: $
 */
public class AnnoConnectionImpl1 implements AnnoConnection1 {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("AnnoConnectionImpl");

    /**
     * ManagedConnection
     */
    private AnnoManagedConnection1 mc;

    /**
     * ManagedConnectionFactory
     */
    private AnnoManagedConnectionFactory1 mcf;

    /**
     * Default constructor
     *
     * @param mc  AnnoManagedConnection
     * @param mcf AnnoManagedConnectionFactory
     */
    public AnnoConnectionImpl1(AnnoManagedConnection1 mc,
                               AnnoManagedConnectionFactory1 mcf) {
        this.mc = mc;
        this.mcf = mcf;
    }

    /**
     * Call me
     */
    public void callMe() {
        mc.callMe();
    }

    /**
     * Close
     */
    public void close() {
        mc.closeHandle(this);
    }

    /**
     * Returns AnnoManagedConnectionFactory
     *
     * @return mcf
     */
    public AnnoManagedConnectionFactory1 getMCF() {
        return mcf;
    }

}

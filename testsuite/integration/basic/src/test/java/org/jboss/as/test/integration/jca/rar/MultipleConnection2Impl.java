/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

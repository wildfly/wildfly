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
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

/**
 * MultipleConnection1Impl
 *
 * @version $Revision: $
 */
public class MultipleConnection1Impl implements MultipleConnection1 {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("MultipleConnection1Impl");

    /**
     * ManagedConnection
     */
    private MultipleManagedConnection1 mc;

    /**
     * ManagedConnectionFactory
     */
    private MultipleManagedConnectionFactory1 mcf;

    /**
     * Default constructor
     *
     * @param mc  MultipleManagedConnection1
     * @param mcf MultipleManagedConnectionFactory1
     */
    public MultipleConnection1Impl(MultipleManagedConnection1 mc,
                                   MultipleManagedConnectionFactory1 mcf) {
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

    @Override
    public Interaction createInteraction() throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

}

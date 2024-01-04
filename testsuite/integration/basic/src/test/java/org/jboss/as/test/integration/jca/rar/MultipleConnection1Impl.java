/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;

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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionMetaData;
import jakarta.resource.cci.Interaction;
import jakarta.resource.cci.LocalTransaction;
import jakarta.resource.cci.ResultSetInfo;

/**
 * BaseCciConnection
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>.
 * @version $Revision: $
 */
public class BaseCciConnection implements Connection {

    /*
     * close
     *
     * @see jakarta.resource.cci.Connection#close()
     */
    @Override
    public void close() throws ResourceException {

    }

    /*
     * createInteraction
     *
     * @see jakarta.resource.cci.Connection#createInteraction()
     */
    @Override
    public Interaction createInteraction() throws ResourceException {
        return null;
    }

    /*
     * getLocalTransaction
     *
     * @see jakarta.resource.cci.Connection#getLocalTransaction()
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    /*
     * getMetaData
     *
     * @see jakarta.resource.cci.Connection#getMetaData()
     */
    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    /*
     * getResultSetInfo
     *
     * @see jakarta.resource.cci.Connection#getResultSetInfo()
     */
    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        return null;
    }

}

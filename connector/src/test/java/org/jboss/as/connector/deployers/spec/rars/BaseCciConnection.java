/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2009, Red Hat Inc, and individual contributors
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
package org.jboss.as.connector.deployers.spec.rars;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

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
     * @see javax.resource.cci.Connection#close()
     */
    @Override
    public void close() throws ResourceException {

    }

    /*
     * createInteraction
     *
     * @see javax.resource.cci.Connection#createInteraction()
     */
    @Override
    public Interaction createInteraction() throws ResourceException {
        return null;
    }

    /*
     * getLocalTransaction
     *
     * @see javax.resource.cci.Connection#getLocalTransaction()
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    /*
     * getMetaData
     *
     * @see javax.resource.cci.Connection#getMetaData()
     */
    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    /*
     * getResultSetInfo
     *
     * @see javax.resource.cci.Connection#getResultSetInfo()
     */
    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        return null;
    }

}

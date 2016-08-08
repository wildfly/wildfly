/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsrm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.jboss.wsf.spi.WSFException;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class RMStoreFeature extends RMFeature {

    private static final Logger LOGGER = Logger.getLogger(RMStoreFeature.class.getName());

    public static final String dataSourceName = "java:jboss/datasources/ExampleDS";
    private InitialContext ctx;

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        RMTxStore rmStore = new RMTxStore();
        DataSource dataSource;
        try {
            if (ctx == null) {
                ctx = new InitialContext();
            }
            dataSource = (DataSource) ctx.lookup(dataSourceName);
            rmStore.setDataSource(dataSource);
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Can't create datasource with " + dataSourceName, e);
            throw new WSFException(e);
        }

        rmStore.init();
        this.setStore(rmStore);
        this.setRMNamespace(RM11Constants.NAMESPACE_URI);

        super.initializeProvider(provider, bus);
    }

}

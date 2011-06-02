/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.ds.mbean;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Test for datasources
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class Test implements TestMBean {

    private static final String JNDI_NAME = "java:/H2DS";

    @Override
    public String test() throws Exception {
        InitialContext context = new InitialContext();
        DataSource ds = (DataSource) context.lookup(JNDI_NAME);
        Connection conn = ds.getConnection();
        ResultSet rs = conn.prepareStatement("select 1").executeQuery();
        return rs.next() ? "Success" : "Failure";
    }
}

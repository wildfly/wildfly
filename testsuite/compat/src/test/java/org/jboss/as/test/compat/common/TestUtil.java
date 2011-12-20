/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.common;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Scott Marlow
 */
public class TestUtil {

    public static void testSimpleCreateAndLoadEntities(final EmployeeBean employeeBean) throws Exception {
        employeeBean.createEmployee(1, "Kelly Smith", "Watford, England");
        employeeBean.createEmployee(2, "Alex Scott", "London, England");
        employeeBean.getEmployee(1);
        employeeBean.getEmployee(2);
    }

    public static void testSecondLevelCache(final InitialContext initialContext, final String datasource, final EmployeeBean employeeBean) throws Exception {
        testSimpleCreateAndLoadEntities(employeeBean);

        final DataSource ds = JndiUtil.rawLookup(initialContext, datasource, DataSource.class);
        final Connection conn = ds.getConnection();
        try {
            final int deleted = conn.prepareStatement("delete from Employee").executeUpdate();
            assertTrue("was able to delete added rows.  delete count=" + deleted, deleted > 1);
        } finally {
            conn.close();
        }

        // read deleted data from second level cache
        final Employee emp = employeeBean.getEmployee(1);

        assertTrue("was able to read deleted database row from second level cache", emp != null);
    }

    public static void testServletSubDeploymentRead(final String deployment, final String param) throws Exception {
        final String first = performCall(deployment, param);
        assertEquals("0", first);

        final String second = performCall(deployment, param);
        assertEquals("0", second);
    }

    private static String performCall(final String deployment, final String param) throws Exception {
        return HttpRequest.get("http://localhost:8080/"+ deployment + "/simple?input=" + param, 10, SECONDS);
    }
}

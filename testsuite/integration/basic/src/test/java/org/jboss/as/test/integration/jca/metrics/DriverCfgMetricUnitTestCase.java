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

package org.jboss.as.test.integration.jca.metrics;


import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * XA datasource configuration and metrics unit test.
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DriverCfgMetricUnitTestCase extends JCAMetrictsTestBase {

    @BeforeClass
    public static void before() {
        setBaseAddress("jdbc-driver", "name");
    }

    @Test
    public void testDriverAttributes() throws Exception {
        setModel("complex-driver.xml");
        assertEquals("name", readAttribute(baseAddress, "driver-name").asString());
        assertEquals("org.h2.jdbcx.JdbcDataSource", readAttribute(baseAddress, "driver-xa-datasource-class-name").asString());
        removeDs();
    }

    @Test
    public void testEmptyDriver() throws Exception {
        setModel("empty-driver.xml");
        assertEquals("name", readAttribute(baseAddress, "driver-name").asString());
        removeDs();
    }

    @Test(expected = Exception.class)
    public void testDriverWoName() throws Exception {
        setBadModel("wrong-wo-name-driver.xml");
    }

    @Test(expected = Exception.class)
    public void testDriverWithNoName() throws Exception {
        setBadModel("wrong-empty-name-driver.xml");
    }

    @Test(expected = Exception.class)
    public void test2DriverClasses() throws Exception {
        setBadModel("wrong-2-driver-classes.xml");
    }

    @Test(expected = Exception.class)
    public void test2DSClasses() throws Exception {
        setBadModel("wrong-2-ds-classes-driver.xml");
    }

    @Test(expected = Exception.class)
    public void test2XADSClasses() throws Exception {
        setBadModel("wrong-2-xa-ds-classes-driver.xml");
    }
}

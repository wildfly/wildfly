/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.manualmode.jca;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the consistency of data-source enable/disable operations.
 *
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceDisableTestCase extends AbstractCliTestBase {

    private static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private static ContainerController container;

    @Before
    public void before() throws Exception {
        container.start(CONTAINER);
        initCLI();
    }

    @After
    public void after() throws Exception {
        closeCLI();
        container.stop(CONTAINER);
    }

    @Test
    public void testDisableXaAndNonXaDataSources() throws IOException, MgmtOperationException {
        try {
            // add a non-xa data source
            cli.sendLine("data-source add --name=DisableTestDS --jndi-name=java:jboss/datasources/DisableTestDS --driver-name=h2 --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            cli.sendLine("data-source enable --name=DisableTestDS");

            // add a xa data source
            cli.sendLine("xa-data-source add --name=XADisableTestDS --jndi-name=java:jboss/datasources/XADisableTestDS --driver-name=h2 --xa-datasource-class=org.h2.jdbcx.JdbcDataSource --xa-datasource-properties=URL=jdbc:h2:mem:test2");
            cli.sendLine("/subsystem=datasources/xa-data-source=XADisableTestDS/xa-datasource-properties=URL:add(value=jdbc:h2:mem:test2)");
            cli.sendLine("xa-data-source enable --name=XADisableTestDS");

            cli.sendLine("/subsystem=datasources/data-source=DisableTestDS:read-attribute(name=enabled)");
            CLIOpResult ret = cli.readAllAsOpResult();
            assertEquals("true", (String) ret.getResult());
            cli.sendLine("/subsystem=datasources/xa-data-source=XADisableTestDS:read-attribute(name=enabled)");
            ret = cli.readAllAsOpResult();
            assertEquals("true", (String) ret.getResult());

            cli.sendLine("data-source disable --name=DisableTestDS");
            String output = cli.readOutput();
            assertTrue(output.contains("reload-required"));

            cli.sendLine("reload");

            cli.sendLine("/subsystem=datasources/data-source=DisableTestDS:read-attribute(name=enabled)");
            ret = cli.readAllAsOpResult();
            assertEquals("false", (String) ret.getResult());
            cli.sendLine("/subsystem=datasources/xa-data-source=XADisableTestDS:read-attribute(name=enabled)");
            ret = cli.readAllAsOpResult();
            assertEquals("true", (String) ret.getResult());

            cli.sendLine("xa-data-source disable --name=XADisableTestDS");
            output = cli.readOutput();
            assertTrue(output.contains("reload-required"));

            cli.sendLine("reload");

            cli.sendLine("/subsystem=datasources/data-source=DisableTestDS:read-attribute(name=enabled)");
            ret = cli.readAllAsOpResult();
            assertEquals("false", (String) ret.getResult());
            cli.sendLine("/subsystem=datasources/xa-data-source=XADisableTestDS:read-attribute(name=enabled)");
            ret = cli.readAllAsOpResult();
            assertEquals("false", (String) ret.getResult());

        } finally {
            // remove the data-sources
            try {
                cli.sendLine("data-source remove --name=DisableTestDS");
            } finally {
                cli.sendLine("xa-data-source remove --name=XADisableTestDS");
            }
        }
    }

}

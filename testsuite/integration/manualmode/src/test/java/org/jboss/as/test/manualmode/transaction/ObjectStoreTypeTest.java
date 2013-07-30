/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.manualmode.transaction;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ObjectStoreTypeTest extends AbstractCliTestBase {
    private static Logger log = Logger.getLogger(ObjectStoreTypeTest.class);

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
    public void testHornetQObjectStore() throws IOException, MgmtOperationException {
        try {
            String objectStoreType = readObjectStoreType();
            assertEquals("default", objectStoreType);

            cli.sendLine("/subsystem=transactions:write-attribute(name=use-hornetq-store, value=true)");
            final CLIOpResult ret = cli.readAllAsOpResult();
            assertEquals("reload-required", (String) ((Map) ret.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));

            cli.sendLine("reload");

            objectStoreType = readObjectStoreType();
            assertEquals("hornetq", objectStoreType);

        } finally {
            setDefaultObjectStore();
        }

    }

    @Test
    public void testJdbcObjectStore() throws IOException, MgmtOperationException {

        try {
            String objectStoreType = readObjectStoreType();
            assertEquals("default", objectStoreType);

            // add data source
            cli.sendLine("data-source add --name=ObjectStoreTestDS --jndi-name=java:jboss/datasources/ObjectStoreTestDS --driver-name=h2 --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1 --jta=false");

            cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=java:jboss/datasources/ObjectStoreTestDS)");
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
            final CLIOpResult ret = cli.readAllAsOpResult();
            assertEquals("reload-required", (String) ((Map) ret.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));

            cli.sendLine("reload");

            objectStoreType = readObjectStoreType();
            assertEquals("jdbc", objectStoreType);

        } finally {
            try {
                setDefaultObjectStore(true);
            } finally {
                // remove data source
                cli.sendLine("data-source remove --name=ObjectStoreTestDS");
//                cli.sendLine("/subsystem=datasources/data-source=ObjectStoreTestDS:remove{allow-resource-service-restart=true}");
            }
        }
    }

    private String readObjectStoreType() throws IOException, MgmtOperationException {
        cli.sendLine("/subsystem=transactions/log-store=log-store:read-attribute(name=type)");
        final CLIOpResult res = cli.readAllAsOpResult();
        return (String) res.getResult();
    }

    private void setDefaultObjectStore() throws IOException, MgmtOperationException {
        setDefaultObjectStore(false);
    }

    private void setDefaultObjectStore(boolean reload) throws IOException, MgmtOperationException {
        final String objectStoreType = readObjectStoreType();
        if ("default".equals(objectStoreType)) {
            return;
        }

        try {
            // reset the object store settings
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-hornetq-store, value=false)");
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=false)");
        } finally {
            if (reload) {
                cli.sendLine("reload");
            }
        }
    }

}

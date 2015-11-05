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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ObjectStoreTypeTestCase extends AbstractCliTestBase {
    private static Logger log = Logger.getLogger(ObjectStoreTypeTestCase.class);

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
            assertEquals("restart-required", (String) ((Map) ret.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));

            cli.sendLine("reload");

            objectStoreType = readObjectStoreType();

            // use-hornetq-store should have the same effect of use-journal-store even if it was deprecated
            assertEquals("journal", objectStoreType);

        } finally {
            setDefaultObjectStore();
        }

    }

    @Test
    public void testJournalObjectStore() throws IOException, MgmtOperationException {
        try {
            String objectStoreType = readObjectStoreType();
            assertEquals("default", objectStoreType);

            cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=true)");
            final CLIOpResult ret = cli.readAllAsOpResult();
            assertEquals("restart-required", (String) ((Map) ret.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));

            cli.sendLine("reload");

            objectStoreType = readObjectStoreType();
            assertEquals("journal", objectStoreType);

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
            createDataSource();

            cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=java:jboss/datasources/ObjectStoreTestDS)");
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
            final CLIOpResult ret = cli.readAllAsOpResult();
            assertEquals("restart-required", ((Map) ret.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));

            cli.sendLine("reload");

            objectStoreType = readObjectStoreType();
            assertEquals("jdbc", objectStoreType);

        } finally {
            try {
                setDefaultObjectStore(true);
            } finally {
                // remove data source
                removeDataSource();
            }
        }
    }

    @Test
    public void testUseJdbcStoreWithoutDatasource() throws Exception {
        try {
            String objectStoreType = readObjectStoreType();
            assertEquals("default", objectStoreType);

            createDataSource();

            // try to undefine use-jdbc-store
            cli.sendLine("/subsystem=transactions:undefine-attribute(name=use-jdbc-store)");
            CLIOpResult result = cli.readAllAsOpResult();
            assertTrue("Failed to undefine use-jdbc-store.", result.isIsOutcomeSuccess());

            // try to set use-jdbc-store to false without defining datasource
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=false)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-jdbc-store to false.", result.isIsOutcomeSuccess());

            // try to set use-jdbc-store to true without defining datasource
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)", true);
            result = cli.readAllAsOpResult();
            assertFalse("Expected failure when jdbc-store-datasource is not set.", result.isIsOutcomeSuccess());

            // correctly set jdbc-store-datasource first and then use-jdbc-store to true
            cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=java:jboss/datasources/ObjectStoreTestDS)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set jdbc-store-datasource.", result.isIsOutcomeSuccess());

            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-jdbc-store.", result.isIsOutcomeSuccess());

            // try to undefine jdbc-store-datasource when use-jdbc-store is set to true
            cli.sendLine("/subsystem=transactions:undefine-attribute(name=jdbc-store-datasource)", true);
            result = cli.readAllAsOpResult();
            assertFalse("Expected failure when un-defining jdbc-store-datasource when use-jdbc-store is true.", result.isIsOutcomeSuccess());

            // setting use-jdbc-store to false
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=false)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-jdbc-store to false.", result.isIsOutcomeSuccess());

            // undefine jdbc-store-datasource
            cli.sendLine("/subsystem=transactions:undefine-attribute(name=jdbc-store-datasource)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to undefine jdbc-store-datasource.", result.isIsOutcomeSuccess());
        } finally {
            try {
                setDefaultObjectStore(true);
            } finally {
                removeDataSource();
            }
        }
    }

    @Test
    public void testEitherJdbcOrJournalStore() throws Exception {
        try {
            String objectStoreType = readObjectStoreType();
            assertEquals("default", objectStoreType);

            createDataSource();

            // set jdbc-store-datasource so that use-jdbc-store can be set to true
            cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=java:jboss/datasources/ObjectStoreTestDS)");
            CLIOpResult result = cli.readAllAsOpResult();
            assertTrue("Failed to set jdbc-store-datasource.", result.isIsOutcomeSuccess());

            // set use-jdbc-store to true
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-jdbc-store to true.", result.isIsOutcomeSuccess());

            // set use-journal-store to true
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=true)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-journal-store to true.", result.isIsOutcomeSuccess());

            // check that use-jdbc-store was automatically set to false
            cli.sendLine("/subsystem=transactions:read-attribute(name=use-jdbc-store)");
            result = cli.readAllAsOpResult();
            assertEquals("use-jdbc-store was not automatically deactivated.", "false", result.getResult());

            // set use-jdbc-store to true
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-journal-store to true.", result.isIsOutcomeSuccess());

            // check that use-journal-store was automatically set to false
            cli.sendLine("/subsystem=transactions:read-attribute(name=use-journal-store)");
            result = cli.readAllAsOpResult();
            assertEquals("use-journal-store was not automatically deactivated.", "false", result.getResult());

            // set use-jdbc-store to false
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=false)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to set use-journal-store to false.", result.isIsOutcomeSuccess());

            // undefine jdbc-store-datasource
            cli.sendLine("/subsystem=transactions:undefine-attribute(name=jdbc-store-datasource)");
            result = cli.readAllAsOpResult();
            assertTrue("Failed to undefine jdbc-store-datasource.", result.isIsOutcomeSuccess());
        } finally {
            try {
                setDefaultObjectStore(true);
            } finally {
                removeDataSource();
            }
        }
    }

    private void createDataSource() {
        cli.sendLine("data-source add --name=ObjectStoreTestDS --jndi-name=java:jboss/datasources/ObjectStoreTestDS --driver-name=h2 --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1 --jta=false");
    }

    private void removeDataSource() {
        cli.sendLine("data-source remove --name=ObjectStoreTestDS");
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
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=false)");
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-hornetq-store, value=false)");
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=false)");
            cli.sendLine("/subsystem=transactions:undefine-attribute(name=jdbc-store-datasource)");
        } finally {
            if (reload) {
                cli.sendLine("reload");
            }
        }
    }

}

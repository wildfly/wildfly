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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ivo Studensky - <istudensky@redhat.com>, initial test case
 * @author Romain Pelisse - <belaran@redhat.com>, rework testcase for work on JBEAP-6449
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ObjectStoreTypeTestCase extends AbstractCliTestBase {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(ObjectStoreTypeTestCase.class);

    private static final String CONTAINER = "default-jbossas";
    private static final String JDBC_STORE_DS_NAME = "ObjectStoreTestDS";

    @ArquillianResource
    private static ContainerController container;

    @Before
    public void before() throws Exception {
        container.start(CONTAINER);
        initCLI(TimeoutUtil.adjust(20 * 1000));
        String objectStoreType = readObjectStoreType();
        assertTrue("Invalid store type: " + objectStoreType,
                objectStoreType.equals("journal") || objectStoreType.equals("default"));
        setDefaultObjectStore();
    }

    @After
    public void after() throws Exception {
        closeCLI();
        container.stop(CONTAINER);
    }

    @SuppressWarnings("rawtypes")
    private void check(String objectStoreTypeExpected) throws IOException, MgmtOperationException {
        final CLIOpResult ret = cli.readAllAsOpResult();
        if (ret != null && ret.getFromResponse(RESPONSE_HEADERS) != null) {
            assertEquals("restart-required", (String) ((Map) ret.getFromResponse(RESPONSE_HEADERS)).get(PROCESS_STATE));
            cli.sendLine("reload");
        }

        String objectStoreType = readObjectStoreType();
        assertEquals(objectStoreTypeExpected, objectStoreType);
    }

    @Test
    public void testHornetQObjectStore() throws IOException, MgmtOperationException {
        try {
            useJournalStore();
        } finally {
            setDefaultObjectStore();
        }
    }

    @Test
    public void testJournalObjectStore() throws IOException, MgmtOperationException {
        try {
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=true)");
            check("journal");
        } finally {
            setDefaultObjectStore();
        }
    }

    @Test
    public void testJdbcObjectStore() throws IOException, MgmtOperationException {
        try {
            useJdbcStore();
            check("jdbc");
        } finally {
            cleanJdbcSettingsAndResetToObjectStore();
        }
    }

    @Test
    public void ifJournalIsTrueThenHornetQToo() throws IOException, MgmtOperationException {
        useJournalStore();
        checkThatAllUseAttributesAreConsistent("true", "false", "true");
    }

    private void useJdbcStore() throws IOException, MgmtOperationException {
        useJdbcStore(true);
    }

    private void useJdbcStore(boolean expectedResults) throws IOException, MgmtOperationException {
        setDefaultObjectStore();
        // 1 - Create DS - required for the JDBC store
        createDataSource();
        // 2 - Set the value for 'jdbc-store-datasource'
        cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=java:jboss/datasources/"
                + JDBC_STORE_DS_NAME + ")");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue("Failed to set jdbc-store-datasource.", result.isIsOutcomeSuccess());
        // 3 - set 'use-jdbc-store' to true
        cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
        result = cli.readAllAsOpResult();
        assertEquals("Failed to set use-jdbc-store to expected value", expectedResults, result.isIsOutcomeSuccess());
    }

    @Test
    public void testUseJdbcStoreWithoutDatasource() throws Exception {
        try {
            // try to set use-jdbc-store to true without defining datasource
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)", true);
            CLIOpResult result = cli.readAllAsOpResult();
            assertFalse("Expected failure when jdbc-store-datasource is not set.", result.isIsOutcomeSuccess());
        } finally {
            setDefaultObjectStore();
        }
    }

    @Test
    public void testUndefinedJdbcStoreDSWhenJDBCisUsed() throws Exception {
        try {
            // Use JDBC store
            useJdbcStore();
            // try, and fail, to undefine jdbc-store-datasource when use-jdbc-store is set to true
            cli.sendLine("/subsystem=transactions:undefine-attribute(name=jdbc-store-datasource", true);
            CLIOpResult result = cli.readAllAsOpResult();
            if (result.isIsOutcomeSuccess())
                fail("The jdbc-store-datasource attribute has been undefined, while JDBC store is in use.");
        } finally {
            cleanJdbcSettingsAndResetToObjectStore();
        }
    }

    /**
     * Test if 0 can be set for default transaction timeout
     * @throws Exception
     */
    @Test
    public void testSet0ToTransactionTimeout() throws Exception {
        try {
            cli.sendLine("/subsystem=transactions:write-attribute(name=default-timeout,value=0)", true);
            checkAttributeIsAsExpected("default-timeout", "0");
        } finally {
            setDefaultObjectStore();
        }
    }

    private void checkAttributeIsAsExpected(String attributeName, String expectedValue) {
        try {
            cli.sendLine("/subsystem=transactions:read-attribute(name=" + attributeName + ")");
            CLIOpResult result = cli.readAllAsOpResult();
            assertEquals(attributeName + " has not the expected value", expectedValue, result.getResult());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    private void checkThatAllUseAttributesAreConsistent(String useJournalStore, String useJdbcStore, String useHornetQStore) {
        checkAttributeIsAsExpected("use-jdbc-store", useJdbcStore);
        checkAttributeIsAsExpected("use-journal-store", useJournalStore);
        checkAttributeIsAsExpected("use-hornetq-store", useHornetQStore);
    }

    @Test
    public void testEitherJdbcOrJournalStore() throws Exception {
        try {
            // Set journal store
            useJournalStore();
            // Check that attributes are consistent with setting
            checkThatAllUseAttributesAreConsistent("true", "false", "true");
            // Use jdbcStore
            useJdbcStore();
            // Check that attributes are consistent with setting
            checkThatAllUseAttributesAreConsistent("false", "true", "false");
        } finally {
            cleanJdbcSettingsAndResetToObjectStore();
        }
    }

    enum StorageMode {

        USE_JDBC_STORE("use-jdbc-store"), USE_JOURNAL_STORE("use-journal-store"), USE_HORNETQ_STORE("use-hornetq-store");

        StorageMode(String attributeName) {
            this.attributeName = attributeName;
        }

        String attributeName;

        public static StorageMode buildFromAttributeName(String attributeName) {
            for ( StorageMode mode : StorageMode.values() ) {
                if ( mode.attributeName.equals(attributeName) )
                    return mode;
            }
            throw new IllegalArgumentException("No such storage mode available:" + attributeName);
        }
    }

    /*
     * Checks that using two different storage mechanisms, within a
     * batch, make the batch fails.
     *
     * See https://issues.jboss.org/browse/WFLY-8335 for more information
     */
    @Test(expected=java.lang.AssertionError.class)
    public void testBatchCliFailsIfNoDSisDefined() throws IOException {
        createDataSource();
        cli.sendLine("batch");
        cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store,value=true)");
        cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=java:jboss/datasources/"
                + JDBC_STORE_DS_NAME + ")");
        cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store,value=true)");
        cli.sendLine("run-batch");
    }

    @Test
    public void testThatAlternatesAreProperlyDefined() throws IOException {
        cli.sendLine("/subsystem=transactions:read-resource-description");
        CLIOpResult result = cli.readAllAsOpResult();
        if ( result != null && result.getResultAsMap() != null ) {
            ModelNode atts = (ModelNode)result.getResponseNode().get("result").get("attributes");
            for ( StorageMode mode : StorageMode.values() )
                checkStorageMode(atts, mode);
        }
        else
            fail("Read resource description operation did provide any result");
    }

    private void checkStorageMode(ModelNode atts, StorageMode mode) {
        ModelNode modeNode = atts.get(mode.attributeName);
        assertTrue(modeNode != null);
        ModelNode alternatives = modeNode.get("alternatives");
        assertTrue(alternatives != null);
        assertEquals(2, alternatives.asList().size());
        for ( int nbAlternative = 0; nbAlternative < 2 ; nbAlternative++ )
            checkAlternative(alternatives.get(nbAlternative).asString(), mode);
    }

    private void checkAlternative(String alternative, StorageMode mode) {
        StorageMode alternativeStorageMode = StorageMode.buildFromAttributeName(alternative);
        assertTrue(alternativeStorageMode != mode );
    }

    private void useJournalStore() throws IOException, MgmtOperationException {
        cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=true)");
        check("journal");
    }

    private void createDataSource() {
        cli.sendLine("data-source add --name=" + JDBC_STORE_DS_NAME + " --jndi-name=java:jboss/datasources/"
                + JDBC_STORE_DS_NAME + " --driver-name=h2 --connection-url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1 --jta=false");
    }

    private void undefinedAttributeIfDefined(String attributeName) {
        try {
            cli.sendLine("/subsystem=transactions:read-attribute(name=" + attributeName + ")");
            CLIOpResult result = cli.readAllAsOpResult();
            if (result.getResponseNode().isDefined())
                cli.sendLine("/subsystem=transactions:undefine-attribute(name=" + attributeName + ")");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void cleanJdbcSettingsAndResetToObjectStore() throws IOException, MgmtOperationException {
        try {
            cleanupSettingsUsedForJDBCStore();
        } finally {
            setDefaultObjectStore();
        }
    }

    private void removeDatasource() {
        try {
            cli.sendLine("data-source remove --name=" + JDBC_STORE_DS_NAME);
        } catch (Exception e) {
            // if the DS does not exist, not need to delete it...
        }
    }

    private void cleanupSettingsUsedForJDBCStore() {
        try {
            // Undefine 'use-jdbc-store' first, if defined
            undefinedAttributeIfDefined("use-jdbc-store");
            // then undefine 'jdbc-store'
            undefinedAttributeIfDefined("jdbc-store-datasource");
            // finally delete Datasource if exists
            removeDatasource();
            // Reload configuration
            cli.sendLine("reload");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String readObjectStoreType() throws IOException, MgmtOperationException {
        cli.sendLine("/subsystem=transactions/log-store=log-store:read-attribute(name=type)");
        final CLIOpResult res = cli.readAllAsOpResult();
        return (String) res.getResult();
    }

    private void setDefaultObjectStore() throws IOException, MgmtOperationException {
        final String objectStoreType = readObjectStoreType();
        if ("default".equals(objectStoreType)) {
            return;
        }

        try {
            cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=false)");
        } finally {
            cli.sendLine("reload");
        }
    }
}

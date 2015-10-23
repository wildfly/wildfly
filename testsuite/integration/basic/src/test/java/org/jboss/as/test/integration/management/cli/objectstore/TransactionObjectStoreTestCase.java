/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.cli.objectstore;

import java.io.IOException;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import com.arjuna.ats.arjuna.tools.osb.api.proxy.RecoveryStoreProxy;
import com.arjuna.ats.arjuna.tools.osb.api.proxy.StoreManagerProxy;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivo Studensky <istudens@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TransactionObjectStoreTestCase extends AbstractCliTestBase {

    private static final String FOO_TYPE = "/StateManager/LockManager/foo";

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, TransactionObjectStoreTestCase.class.getSimpleName() + ".jar")
                .addClass(ObjectStoreBrowserService.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts\n"), "MANIFEST.MF")
                .addAsManifestResource(TransactionObjectStoreTestCase.class.getPackage(), "permissions.xml", "permissions.xml");
        return jar;
    }

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        // reset to default values in case a test failed
        cli.sendLine("/subsystem=transactions:undefine-attribute(name=use-jdbc-store)");
        cli.sendLine("/subsystem=transactions:undefine-attribute(name=use-journal-store)");
        cli.sendLine("/subsystem=transactions:undefine-attribute(name=jdbc-store-datasource)");

        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testExposeAllLogs() throws Exception {
        String serviceUrl = managementClient.getRemoteJMXURL().toString();
        try {
            RecoveryStoreProxy prs = StoreManagerProxy.getRecoveryStore(serviceUrl, null);
            // this immediately checks whether we really got a store manager proxy or not
            assertNotNull(prs.getStoreName());

            // write a log record to the object store
            // create a record that by default the tooling does not expose
            byte[] data = new byte[10240];
            OutputObjectState state = new OutputObjectState();
            Uid uid = new Uid();
            state.packBytes(data);
            assertTrue(prs.write_committed(uid, FOO_TYPE, state));

            // probe the log store
            cli.sendLine("/subsystem=transactions/log-store=log-store:write-attribute(name=expose-all-logs,value=false)");
            cli.sendLine("/subsystem=transactions/log-store=log-store:probe()");

            // check the log record is not listed
            cli.sendLine("cd /subsystem=transactions/log-store=log-store/transactions");
            cli.sendLine("ls");
            String ls = cli.readOutput();
            assertFalse("Unexpected uid in the log.", ls != null && ls.contains(uid.toString()));

            // probe the log store again for all records
            cli.sendLine("/subsystem=transactions/log-store=log-store:write-attribute(name=expose-all-logs,value=true)");
            cli.sendLine("/subsystem=transactions/log-store=log-store:probe()");

            // check the log record is listed
            cli.sendLine("cd /subsystem=transactions/log-store=log-store/transactions");
            cli.sendLine("ls");
            ls = cli.readOutput();
            assertTrue("Could not find the expected uid in the log.", ls != null && ls.contains(uid.toString()));

            // remove the log record
            cli.sendLine("/subsystem=transactions/log-store=log-store/transactions=" + escapeColons(uid.toString()) + ":delete()");

            // probe the log store again
            cli.sendLine("/subsystem=transactions/log-store=log-store:probe()");

            // check the log record is not listed
            cli.sendLine("cd /subsystem=transactions/log-store=log-store/transactions");
            cli.sendLine("ls");
            ls = cli.readOutput();
            assertFalse("Unexpected uid in the log after its remove.", ls != null && ls.contains(uid.toString()));

            // set back the expose-all-logs attribute
            cli.sendLine("/subsystem=transactions/log-store=log-store:write-attribute(name=expose-all-logs,value=false)");

        } finally {
            StoreManagerProxy.releaseProxy(serviceUrl);
        }
    }

    @Test
    public void testUseJdbcStoreWithoutDatasource() throws IOException {
        // check that use-jdbc-store is initially false
        cli.sendLine("/subsystem=transactions:read-attribute(name=use-jdbc-store)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertEquals("false", result.getResult());

        // try to undefine use-jdbc-store
        cli.sendLine("/subsystem=transactions:undefine-attribute(name=use-jdbc-store)");
        result = cli.readAllAsOpResult();
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
        cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=test)");
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
    }

    @Test
    public void testEitherJdbcOrJournalStore() throws IOException {
        // check that both attributes are undefined
        cli.sendLine("/subsystem=transactions:read-attribute(name=use-jdbc-store)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertEquals("false", result.getResult());

        cli.sendLine("/subsystem=transactions:read-attribute(name=use-journal-store)");
        result = cli.readAllAsOpResult();
        assertEquals("false", result.getResult());

        // check jdbc-store-datasource se that use-jdbc-store can be set to true
        cli.sendLine("/subsystem=transactions:write-attribute(name=jdbc-store-datasource, value=test)");
        result = cli.readAllAsOpResult();
        assertTrue("Failed to set jdbc-store-datasource.", result.isIsOutcomeSuccess());

        // try set both attributes to true
        cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)");
        result = cli.readAllAsOpResult();
        assertTrue("Failed to set use-jdbc-store to true.", result.isIsOutcomeSuccess());

        cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=true)", true);
        result = cli.readAllAsOpResult();
        assertFalse("Expected failure when setting both journal and jdbc stores.", result.isIsOutcomeSuccess());

        // undefine jdbc and set journal store
        cli.sendLine("/subsystem=transactions:undefine-attribute(name=use-jdbc-store)");
        result = cli.readAllAsOpResult();
        assertTrue("Failed to undefine use-jdbc-store.", result.isIsOutcomeSuccess());

        cli.sendLine("/subsystem=transactions:write-attribute(name=use-journal-store, value=true)");
        result = cli.readAllAsOpResult();
        assertTrue("Failed to set use-journal-store to true.", result.isIsOutcomeSuccess());

        // try to set jdbc store again
        cli.sendLine("/subsystem=transactions:write-attribute(name=use-jdbc-store, value=true)", true);
        result = cli.readAllAsOpResult();
        assertFalse("Expected failure when setting both journal and jdbc stores.", result.isIsOutcomeSuccess());

        // undefine journal store
        cli.sendLine("/subsystem=transactions:undefine-attribute(name=use-journal-store)");
        result = cli.readAllAsOpResult();
        assertTrue("Failed to undefine use-jdbc-store.", result.isIsOutcomeSuccess());
    }

    private String escapeColons(String colons) {
        return colons.replaceAll(":", "\\\\:");
    }
}

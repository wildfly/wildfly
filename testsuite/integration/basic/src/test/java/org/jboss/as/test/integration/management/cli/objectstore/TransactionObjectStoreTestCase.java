/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli.objectstore;

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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private String escapeColons(String colons) {
        return colons.replaceAll(":", "\\\\:");
    }
}

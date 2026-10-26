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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivo Studensky <istudens@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
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

    @BeforeAll
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterAll
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
            assertFalse(ls != null && ls.contains(uid.toString()), "Unexpected uid in the log.");

            // probe the log store again for all records
            cli.sendLine("/subsystem=transactions/log-store=log-store:write-attribute(name=expose-all-logs,value=true)");
            cli.sendLine("/subsystem=transactions/log-store=log-store:probe()");

            // check the log record is listed
            cli.sendLine("cd /subsystem=transactions/log-store=log-store/transactions");
            cli.sendLine("ls");
            ls = cli.readOutput();
            assertTrue(ls != null && ls.contains(uid.toString()), "Could not find the expected uid in the log.");

            // remove the log record
            cli.sendLine("/subsystem=transactions/log-store=log-store/transactions=" + escapeColons(uid.toString()) + ":delete()");

            // probe the log store again
            cli.sendLine("/subsystem=transactions/log-store=log-store:probe()");

            // check the log record is not listed
            cli.sendLine("cd /subsystem=transactions/log-store=log-store/transactions");
            cli.sendLine("ls");
            ls = cli.readOutput();
            assertFalse(ls != null && ls.contains(uid.toString()), "Unexpected uid in the log after its remove.");

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

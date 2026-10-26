/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction.inflow;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.util.Hashtable;
import java.util.PropertyPermission;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Testcase running Jakarta Connectors inflow transaction from deployed RAR.
 * Two mock XA resources are enlisted inside of MDB to proceed 2PC.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class TransactionInflowTestCase {

    private static final String EJB_MODULE_NAME = "inflow-ejb-";
    private static final String COMMIT = TransactionInflowResourceAdapter.ACTION_COMMIT;
    private static final String ROLLBACK = TransactionInflowResourceAdapter.ACTION_ROLLBACK;

    @ArquillianResource
    public Deployer deployer;

    @Deployment(name = TransactionInflowMdb.RESOURCE_ADAPTER_NAME, order = 1)
    public static ResourceAdapterArchive getResourceAdapterDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "inflow-txn-inside.jar")
            .addClasses(TransactionInflowResourceAdapter.class)
            .addClasses(TransactionInflowXid.class, TransactionInflowWork.class, TransactionInflowRaSpec.class,
                TransactionInflowTextMessage.class, TransactionInflowWorkListener.class, TimeoutUtil.class);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class,
                TransactionInflowMdb.RESOURCE_ADAPTER_NAME + ".rar")
            .addAsResource(TransactionInflowTestCase.class.getPackage(), "ra.xml", "META-INF/ra.xml")
            .addAsManifestResource(createPermissionsXmlAsset(
                    new RuntimePermission("accessDeclaredMembers"),
                    new RuntimePermission("getClassLoader"),
                    new RuntimePermission("defineClassInPackage.org.jboss.as.test.integration.transaction.inflow"),
                    new PropertyPermission("ts.timeout.factor", "read")
                ) , "jboss-permissions.xml")
            .addAsLibrary(jar);
        return rar;
    }

    /**
     * Based on action parameter particular ejb-jar*.xml file is added to archive.
     * The ejb-jar.xml defines RAR spec config property.
     */
    public static JavaArchive getEjbDeployment(String action) {
        return ShrinkWrap.create(JavaArchive.class, EJB_MODULE_NAME + action + ".jar")
            .addClasses(TransactionInflowMdb.class)
            .addClasses(TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class)
            .addClasses(TestXAResource.class)
            .addAsResource(TransactionInflowTestCase.class.getPackage(), "ejb-jar-" + action + ".xml", "META-INF/ejb-jar.xml")
            // module dependency on rar is added because we want to share class of TransactionInflowTextMessage
            // and arquillian packs this testcase class to container here and it needs to see rar classes as well
            .addAsManifestResource(new StringAsset("Dependencies: deployment."
                + TransactionInflowMdb.RESOURCE_ADAPTER_NAME+ ".rar\n"), "MANIFEST.MF")
            .addAsManifestResource(createPermissionsXmlAsset(
                    new RuntimePermission("accessDeclaredMembers"),
                    new SocketPermission("*", "resolve") // #getXid calls InetAddress#getLocalHost
                ) , "jboss-permissions.xml");
    }

    @Deployment(name = EJB_MODULE_NAME + COMMIT, managed = false, testable = false)
    public static JavaArchive getCommitDeployment() {
        return getEjbDeployment(COMMIT);
    }

    @Deployment(name = EJB_MODULE_NAME + ROLLBACK, managed = false, testable = false)
    public static JavaArchive getRollbackDeployment() {
        return getEjbDeployment(ROLLBACK);
    }

    @AfterEach
    public void cleanUp() {
        deployer.undeploy(EJB_MODULE_NAME + COMMIT);
        deployer.undeploy(EJB_MODULE_NAME + ROLLBACK);
    }

    @Test
    public void inflowTransactionCommit() throws NamingException {
        deployer.deploy(EJB_MODULE_NAME + COMMIT);

        TransactionCheckerSingletonRemote checker = getSingletonChecker(EJB_MODULE_NAME + COMMIT);

        try {
            Assertions.assertEquals(1, checker.getMessages().size(), "Expecting one message was passed from RAR to MDB");
            Assertions.assertEquals(TransactionInflowResourceAdapter.MSG,
                    checker.getMessages().iterator().next(), "Expecting message with the content was passed from RAR to MDB");
            Assertions.assertEquals(2, checker.getPrepared(), "Two XAResources were enlisted thus expected to be prepared");
            Assertions.assertEquals(2, checker.getCommitted(), "Two XAResources are expected to be committed");
            Assertions.assertEquals(0, checker.getRolledback(), "Two XAResources were were committed thus not rolled-back");
        } finally {
            checker.resetAll();
        }
    }

    @Test
    public void inflowTransactionRollback() throws NamingException {
        deployer.deploy(EJB_MODULE_NAME + ROLLBACK);

        TransactionCheckerSingletonRemote checker = getSingletonChecker(EJB_MODULE_NAME + ROLLBACK);

        try {
            Assertions.assertEquals(1, checker.getMessages().size(), "Expecting one message was passed from RAR to MDB");
            Assertions.assertEquals(TransactionInflowResourceAdapter.MSG,
                    checker.getMessages().iterator().next(), "Expecting message with the content was passed from RAR to MDB");
            Assertions.assertEquals(2, checker.getPrepared(), "Two XAResources were enlisted thus expected to be prepared");
            Assertions.assertEquals(2, checker.getRolledback(), "Two XAResources are expected to be rolled-back");
            Assertions.assertEquals(0, checker.getCommitted(), "Two XAResources were were rolled-bck thus not committed");
        } finally {
            checker.resetAll();
        }
    }


    private TransactionCheckerSingletonRemote getSingletonChecker(String moduleName) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);

        String ejbLookupString = Util.createRemoteEjbJndiContext("", moduleName, "",
                TransactionCheckerSingleton.class.getSimpleName(), TransactionCheckerSingletonRemote.class.getName(), false);
        TransactionCheckerSingletonRemote checker = (TransactionCheckerSingletonRemote) context.lookup(ejbLookupString);
        return checker;
    }
}

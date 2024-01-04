/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.txnaccess;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.context.ContextPermission;

import javax.naming.InitialContext;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
public class TransactionNamespaceAccessTestCase {

    private static final String MODULE_NAME = "transaction-namespace-access-test-case";

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(TransactionNamespaceAccessTestCase.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        new ContextPermission("org.wildfly.transaction.client.context.remote", "get")
                ), "permissions.xml");
        return jar;
    }

    @Test
    public void testUserTransactionLookupInCMT() throws Exception {
        final CMTSLSB cmtSlsb = InitialContext.doLookup("java:module/" + CMTSLSB.class.getSimpleName() + "!" + CMTSLSB.class.getName());
        cmtSlsb.checkUserTransactionAccessDenial();
    }

    @Test
    public void testUserTransactionLookupInBMT() throws Exception {
        final BMTSLSB bmtslsb = InitialContext.doLookup("java:module/" + BMTSLSB.class.getSimpleName() + "!" + BMTSLSB.class.getName());
        bmtslsb.checkUserTransactionAvailability();
    }
}

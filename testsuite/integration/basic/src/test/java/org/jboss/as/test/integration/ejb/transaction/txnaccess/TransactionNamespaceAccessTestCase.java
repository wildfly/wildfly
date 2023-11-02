/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

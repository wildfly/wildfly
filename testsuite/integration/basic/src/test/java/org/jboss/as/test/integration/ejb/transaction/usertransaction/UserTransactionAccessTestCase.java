/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.usertransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class UserTransactionAccessTestCase {

    private static final String MODULE_NAME = "user-transaction-access-test";

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(UserTransactionAccessTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testUserTransactionLookupInBMT() throws Exception {
        final CMTSLSB cmtSlsb = InitialContext.doLookup("java:module/" + CMTSLSB.class.getSimpleName() + "!" + CMTSLSB.class.getName());
        cmtSlsb.checkUserTransactionAccessDenial();

        // test with a BMT bean now
        final BMTSLSB bmtSlsb = InitialContext.doLookup("java:module/" + BMTSLSB.class.getSimpleName() + "!" + BMTSLSB.class.getName());
        bmtSlsb.checkUserTransactionAvailability();
    }
}

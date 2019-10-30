/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.cascadedelete;

import javax.ejb.ObjectNotFoundException;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A CascadeDeleteUnitTestCase.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 82920 $
 */
@RunWith(CmpTestRunner.class)
public class CascadeDeleteUnitTestCase extends AbstractCmpTest {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-cascadedelete.jar");
        jar.addPackage(AccountBean.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/cascadedelete/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/cascadedelete/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    @Test
    public void testCascade() throws Exception {
        CustomerLocalHome ch = getCustomerHome();
        ch.remove(1L);
        try {
            ch.findByPrimaryKey(1L);
            fail("Entity should not be found");
        } catch (ObjectNotFoundException expected) {
        }
        AccountLocalHome ah = getAccountHome();
        try {
            ah.findByPrimaryKey(11L);
            fail("Entity should not be found");
        } catch (ObjectNotFoundException expected) {
        }
        try {
            ah.findByPrimaryKey(22L);
            fail("Entity should not be found");
        } catch (ObjectNotFoundException expected) {
        }
        try {
            ah.findByPrimaryKey(33L);
            fail("Entity should not be found");
        } catch (ObjectNotFoundException expected) {
        }
    }

    public void setUpEjb() throws Exception {
        CustomerLocalHome ch = getCustomerHome();
        CustomerLocal customer = ch.create(1L, "customer1");

        AccountLocalHome ah = getAccountHome();
        AccountLocal acc11 = ah.create(11L, "account11");
        acc11.setCustomer(customer);

        AccountLocal acc22 = ah.create(22L, "account22");
        acc22.setCustomer(customer);
        acc11.setParentAccount(acc22);

        AccountLocal acc33 = ah.create(33L, "account33");
        acc33.setParentAccount(acc22);
        acc33.setParentAccount2(acc11);
        acc33.setCustomer(customer);
    }

    private AccountLocalHome getAccountHome() throws NamingException {
        return (AccountLocalHome) iniCtx.lookup("java:module/Account!org.jboss.as.test.integration.ejb.entity.cmp.cascadedelete.AccountLocalHome");
    }

    private CustomerLocalHome getCustomerHome() throws NamingException {
        return (CustomerLocalHome) iniCtx.lookup("java:module/Customer!org.jboss.as.test.integration.ejb.entity.cmp.cascadedelete.CustomerLocalHome");
    }
}

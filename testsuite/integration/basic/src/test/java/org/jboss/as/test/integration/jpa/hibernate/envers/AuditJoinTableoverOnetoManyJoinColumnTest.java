/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jpa.hibernate.envers;

import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test @AuditJoinTable over Uni-directional One-to-Many Relationship
 *
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class AuditJoinTableoverOnetoManyJoinColumnTest {
    private static final String ARCHIVE_NAME = "jpa_AuditMappedByoverOnetoManyJoinColumnTest";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Customer.class, Phone.class, SLSBAudit.class);
        jar.addAsManifestResource(AuditJoinTableoverOnetoManyJoinColumnTest.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testRevisionsfromAuditJoinTable() throws Exception {

        SLSBAudit slsbAudit = lookup("SLSBAudit", SLSBAudit.class);

        Customer c1 = slsbAudit.createCustomer("MADHUMITA", "SADHUKHAN", "WORK", "+420", "543789654");
        Phone p1 = c1.getPhones().get(1);
        p1.setType("Emergency");
        slsbAudit.updatePhone(p1);
        c1.setSurname("Mondal");
        slsbAudit.updateCustomer(c1);
        c1.setFirstname("Steve");
        c1.setSurname("Jobs");
        slsbAudit.updateCustomer(c1);

        // delete phone

        c1.getPhones().remove(p1);
        slsbAudit.updateCustomer(c1);
        slsbAudit.deletePhone(p1);
        Assert.assertEquals(1, c1.getPhones().size());
        testRevisionDatafromAuditJoinTable(c1, slsbAudit);
        testRevisionTypefromAuditJoinTable(c1, slsbAudit);
        testOtherFieldslikeForeignKeysfromAuditJoinTable(c1, slsbAudit);

    }

    private void testRevisionDatafromAuditJoinTable(Customer c1, SLSBAudit sb) throws Exception {

        // fetch REV
        List<Object> custHistory = sb.verifyRevision(c1.getId());

        // verify size
        Assert.assertEquals(2, custHistory.size());

        int counter = 0;

        for (Object revisionEntity : custHistory) {

            counter++;
            Assert.assertNotNull(revisionEntity);
            Customer rev = (Customer) (((List<Object>) (revisionEntity)).toArray()[0]);
            Assert.assertNotNull(rev); // check if revision obtained is not null

            Assert.assertEquals("MADHUMITA", rev.getFirstname());

            if (counter == 1) { Assert.assertEquals("SADHUKHAN", rev.getSurname()); }
            if (counter == 2) { Assert.assertEquals("Mondal", rev.getSurname()); }

        }

    }

    private void testRevisionTypefromAuditJoinTable(Customer c1, SLSBAudit sb) throws Exception {

        // fetch REVType
        List<Object> custRevision = sb.verifyRevisionType(c1.getId());

        int counter = 0;
        for (Object revisionTypeEntity : custRevision) {

            counter++;
            Assert.assertNotNull(revisionTypeEntity);
            Customer rev = (Customer) (((List<Object>) (revisionTypeEntity)).toArray()[0]);
            Assert.assertNotNull(rev); // check if revision obtained is not null
            Assert.assertNotNull(rev.getFirstname());

        }

    }

    private void testOtherFieldslikeForeignKeysfromAuditJoinTable(Customer c1, SLSBAudit sb) throws Exception {

        List<Object> phHistory = sb.verifyOtherFields(c1.getId());
        Assert.assertNotNull(phHistory);

        // just to check correct values are returned
        for (Object phoneIdEntity : phHistory) {

            Assert.assertNotNull(phoneIdEntity);
            //System.out.println("revendPhoneID::--" + phoneIdEntity.toString());

        }

    }

}

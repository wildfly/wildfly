/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.hibernate.envers.basicenverstest;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.envers.Address;
import org.jboss.as.test.integration.jpa.hibernate.envers.Person;
import org.jboss.as.test.integration.jpa.hibernate.envers.SLSBPU;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author Strong Liu
 */
@RunWith(Arquillian.class)
public class BasicEnversTestCase {
    private static final String ARCHIVE_NAME = "jpa_BasicEnversTestCase";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {

        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(
                Person.class,
                Address.class,
                SLSBPU.class
        );
        jar.addAsManifestResource(BasicEnversTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testSimpleEnversOperation() throws Exception {

        SLSBPU slsbpu = lookup("SLSBPU", SLSBPU.class);
        Person p1 = slsbpu.createPerson("Strong", "Liu", "kexueyuan source road", 307);
        Person p2 = slsbpu.createPerson("tom", "cat", "apache", 34);
        Address a1 = p1.getAddress();
        a1.setHouseNumber(5);

        p2.setAddress(a1);
        slsbpu.updateAddress(a1);
        slsbpu.updatePerson(p2);

        int size = slsbpu.retrieveOldPersonVersionFromAddress(a1.getId());
        Assert.assertEquals(1, size);
    }
}

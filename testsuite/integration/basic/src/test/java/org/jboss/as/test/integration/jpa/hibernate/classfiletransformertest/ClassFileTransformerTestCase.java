/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.classfiletransformertest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.Employee;
import org.jboss.as.test.integration.jpa.hibernate.SFSB1;
import org.jboss.as.test.integration.jpa.hibernate.SFSBHibernateSession;
import org.jboss.as.test.integration.jpa.hibernate.SFSBHibernateSessionFactory;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate "hibernate.ejb.use_class_enhancer" test that causes hibernate to add a
 * jakarta.persistence.spi.ClassTransformer to the pu.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class ClassFileTransformerTestCase {

    private static final String ARCHIVE_NAME = "jpa_ClassFileTransformerTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(ClassFileTransformerTestCase.class,
                Employee.class,
                SFSB1.class,
                SFSBHibernateSession.class,
                SFSBHibernateSessionFactory.class
        );
        jar.addAsManifestResource(ClassFileTransformerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @BeforeClass
    public static void skipSecurityManager() {
        // See WFLY-11359
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testhibernate_ejb_use_class_enhancer() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Kelly Smith", "Watford, England", 10);
        sfsb1.createEmployee("Alex Scott", "London, England", 20);
        Employee emp = sfsb1.getEmployeeNoTX(10);

        assertTrue("was able to read database row with hibernate.ejb.use_class_enhancer enabled", emp != null);
    }

    @Test
    public void testHibernateByteCodeEnhancementIsDisabledByDefault() {
        // Note: ManagedTypeHelper is an internal Hibernate ORM class, if it is removed or renamed then this test can be updated
        // accordingly.
        assertFalse("Employee class is not bytecode enhanced", org.hibernate.engine.internal.ManagedTypeHelper.isManagedType(Employee.class));
    }

}

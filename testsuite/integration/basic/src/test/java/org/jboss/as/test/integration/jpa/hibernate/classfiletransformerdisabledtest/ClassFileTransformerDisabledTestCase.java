/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.classfiletransformerdisabledtest;

import static org.junit.Assert.assertFalse;

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
 * test that entity class is not bytecode enhanced by Hibernate when persistence unit hint jboss.as.jpa.classtransformer is false
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class ClassFileTransformerDisabledTestCase {

    private static final String ARCHIVE_NAME = "jpa_ClassFileTransformerDisabledTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(ClassFileTransformerDisabledTestCase.class,
                Employee.class,
                SFSB1.class,
                SFSBHibernateSession.class,
                SFSBHibernateSessionFactory.class
        );
        jar.addAsManifestResource(ClassFileTransformerDisabledTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
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

    // NotByteCodeEnhancedPU
    @Test
    public void testHibernateByteCodeEnhancementIsDisabled() {
        // Note: ManagedTypeHelper is an internal Hibernate ORM class, if it is removed or renamed then this test can be updated
        // accordingly.
        assertFalse("Employee class is not bytecode enhanced as per persistence unit hint jboss.as.jpa.classtransformer=false", org.hibernate.engine.internal.ManagedTypeHelper.isManagedType(Employee.class));
    }

}

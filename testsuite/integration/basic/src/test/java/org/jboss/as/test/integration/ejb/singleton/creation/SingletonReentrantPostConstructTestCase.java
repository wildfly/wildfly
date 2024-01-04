/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.creation;

import org.jboss.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the fix to {@link org.jboss.as.ejb3.component.singleton.SingletonComponent#getComponentInstance())} that was made to
 * prevent stack overflows as described in <a href="https://issues.jboss.org/browse/AS7-5184">AS7-5184</a>.
 * <p>
 * The scenario itself is illegal because the business method {@link SingletonBeanOne#performSomething()} is being invoked from
 * {@link SingletonBeanTwo#initialise()} before SingletonBeanOne is "method-ready" - it's PostConstruct method is not yet
 * completed.
 *
 * @author steve.coy
 */
@RunWith(Arquillian.class)
public class SingletonReentrantPostConstructTestCase {
    private static final Logger log = Logger.getLogger(SingletonReentrantPostConstructTestCase.class.getName());

    @EJB(mappedName = "java:module/SingletonBeanOne")
    private SingletonBeanOne singletonBean;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-reentrantly-created-singleton.jar");
        jar.addPackage(SingletonReentrantPostConstructTestCase.class.getPackage());
        return jar;
    }

    /**
     * Indirectly invokes the {@link PostConstruct} annotated {@link SingletonBeanOne#initialise()} method.
     */
    @Test
    public void testReentrantPostConstruction() {
        // trigger the bean creation life-cycle
        try {
            singletonBean.start();
            Assert.fail("Expected an EJBException");
        } catch (EJBException expected) {
            final Throwable cause = expected.getCause();
            Assert.assertTrue("Expected exception cause to be an java.lang.IllegalStateException, but it was a " + cause,
                    cause instanceof IllegalStateException);
        }
    }

}

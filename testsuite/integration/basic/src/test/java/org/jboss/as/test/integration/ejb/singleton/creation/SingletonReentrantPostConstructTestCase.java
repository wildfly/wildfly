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

package org.jboss.as.test.integration.ejb.singleton.creation;

import org.jboss.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;

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

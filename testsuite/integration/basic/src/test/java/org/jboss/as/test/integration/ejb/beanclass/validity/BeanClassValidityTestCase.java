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

package org.jboss.as.test.integration.ejb.beanclass.validity;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Tests that deployments containing invalid bean classes (like a @Stateless on a *interface*) doesn't cause deployment
 * failures.
 *
 * @see https://issues.jboss.org/browse/AS7-1380
 *      User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class BeanClassValidityTestCase {

    private static final String JAR_NAME = "beanclass-validity-test";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME + ".jar");
        jar.addPackage(StatelessOnAInterface.class.getPackage());

        return jar;
    }

    /**
     * Tests a simple invocation on a correct bean contained within the same deployment as an invalid bean class.
     * This test asserts that the presence of an invalid bean class doesn't prevent the correct bean from deploying.
     *
     * @throws Exception
     */
    @Test
    public void testDeployment() throws Exception {
        ProperStatelessBean bean = InitialContext.doLookup("java:module/" + ProperStatelessBean.class.getSimpleName() + "!" + ProperStatelessBean.class.getName());
        bean.doNothing();
    }
}

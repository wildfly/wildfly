/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

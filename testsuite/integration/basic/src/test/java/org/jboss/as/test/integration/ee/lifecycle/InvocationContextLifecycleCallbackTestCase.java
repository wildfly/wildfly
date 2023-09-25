/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.lifecycle;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * User: jpai
 */
@RunWith(Arquillian.class)
public class InvocationContextLifecycleCallbackTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(SimpleInterceptor.class.getPackage());

        return jar;
    }

    @Test
    public void testInvocationContextGetMethodOnLifecycleMethod() throws Exception {
        final LifecycleCallbackBean bean = InitialContext.doLookup("java:module/" + LifecycleCallbackBean.class.getSimpleName());
        Assert.assertTrue("@PostConstruct was not invoked on bean", bean.wasPostConstructInvoked());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.lifecycle;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * User: jpai
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertTrue(bean.wasPostConstructInvoked(), "@PostConstruct was not invoked on bean");
    }
}

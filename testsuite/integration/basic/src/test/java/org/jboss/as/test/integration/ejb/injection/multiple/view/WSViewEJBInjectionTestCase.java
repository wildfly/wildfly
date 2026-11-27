/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.injection.multiple.view;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;

/**
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class WSViewEJBInjectionTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "multiple-view-ejb-injection.jar");
        jar.addPackage(WSViewEJBInjectionTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testInjection() throws Exception {
        final InjectingBean injectingBean = InitialContext.doLookup("java:module/" + InjectingBean.class.getSimpleName() + "!" + InjectingBean.class.getName());
        Assertions.assertTrue(injectingBean.isBeanInjected(), "@EJB injection did not happen in bean");
        // try an invocation on the bean
        injectingBean.invokeInjectedBean();
    }
}

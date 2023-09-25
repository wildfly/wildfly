/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.pojo.support.TOwner;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class CallbackBeansTestCase {
    @Deployment(name = "callback-beans")
    public static JavaArchive getCallbackBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "callback-beans.jar");
        archive.addPackage(TOwner.class.getPackage());
        archive.addAsManifestResource(CallbackBeansTestCase.class.getPackage(), "callback-jboss-beans.xml", "callback-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("callback-beans")
    public void testCallbackBeans() throws Exception {
        // TODO -- try to get beans?
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.pojo.support.TFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class SimpleBeansTestCase {
    @Deployment(name = "simple-beans")
    public static JavaArchive getSimpleBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "simple-beans.jar");
        archive.addPackage(TFactory.class.getPackage());
        archive.addAsManifestResource(SimpleBeansTestCase.class.getPackage(), "simple-jboss-beans.xml", "simple-jboss-beans.xml");
        archive.addAsManifestResource(SimpleBeansTestCase.class.getPackage(), "legacy-jboss-beans.xml", "legacy-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("simple-beans")
    public void testSimpleBeans() throws Exception {
        // TODO -- try to get beans?
    }
}

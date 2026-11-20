/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.pojo.support.TFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@ExtendWith(ArquillianExtension.class)
public class BeanFactoryTestCase {
    @Deployment(name = "bean-factory")
    public static JavaArchive getBeanFactoryJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bean-factory.jar");
        archive.addPackage(TFactory.class.getPackage());
        archive.addAsManifestResource(BeanFactoryTestCase.class.getPackage(), "bf-jboss-beans.xml", "bf-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("bean-factory")
    public void testTypeBeans() throws Exception {
        // TODO -- try to get beans?
    }
}

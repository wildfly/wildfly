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
public class CollectionsBeansTestCase {
    @Deployment(name = "collections-beans")
    public static JavaArchive getCycleBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "collections-beans.jar");
        archive.addPackage(TFactory.class.getPackage());
        archive.addAsManifestResource(CollectionsBeansTestCase.class.getPackage(), "collections-jboss-beans.xml", "collections-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("collections-beans")
    public void testCycleBeans() throws Exception {
        // TODO -- try to get beans?
    }
}

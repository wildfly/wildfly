/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.pojo.support.Sub;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class LifecycleBeansTestCase {
    @Deployment(name = "lifecycle-beans")
    public static JavaArchive getCycleBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "lifecycle-beans.jar");
        archive.addPackage(Sub.class.getPackage());
        archive.addAsManifestResource(LifecycleBeansTestCase.class.getPackage(), "sub-jboss-beans.xml", "sub-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("lifecycle-beans")
    public void testTypeBeans() throws Exception {
        // TODO -- try to get beans?
    }
}

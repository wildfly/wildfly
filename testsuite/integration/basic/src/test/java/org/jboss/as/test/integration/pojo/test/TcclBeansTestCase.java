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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@ExtendWith(ArquillianExtension.class)
public class TcclBeansTestCase {
    @Deployment(name = "tccl-beans")
    public static JavaArchive getTcclBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "tccl-beans.jar");
        archive.addPackage(TFactory.class.getPackage());
        archive.addAsManifestResource(TcclBeansTestCase.class.getPackage(), "tccl-jboss-beans.xml", "tccl-jboss-beans.xml");
        archive.addAsResource(new StringAsset("tccl"), "tccl.txt");
        return archive;
    }

    @Test
    @OperateOnDeployment("tccl-beans")
    public void testTcclBeans() throws Exception {
        // TODO -- try to get beans?
    }
}

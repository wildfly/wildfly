/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.util;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Non-functional test only used to validate the pom setup of this testsuite module.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TestsuiteModuleTestCase {

    @Deployment(name = "empty")
    public static JavaArchive getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, TestsuiteModuleTestCase.class.getSimpleName() + ".jar");
        // meaningless content just to put something in the jar
        archive.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getenv.*")), "permissions.xml");
        return archive;
    }

    @Test
    public void fakeTest() {
        Assume.assumeTrue("Not a real test", false);
    }
}

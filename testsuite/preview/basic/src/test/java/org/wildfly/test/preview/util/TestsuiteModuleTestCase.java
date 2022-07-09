/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.preview.util;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

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

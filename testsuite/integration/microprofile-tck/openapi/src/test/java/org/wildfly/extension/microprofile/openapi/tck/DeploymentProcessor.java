/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.openapi.tck;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            extensionsJar.addPackage(Matchers.class.getPackage());
            extensionsJar.addPackage(IsEqual.class.getPackage());

            WebArchive war = WebArchive.class.cast(archive);
            war.addAsLibraries(extensionsJar);
        }
    }
}
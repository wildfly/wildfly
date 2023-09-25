/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.health.test;

import org.apache.http.HttpRequest;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.eclipse.microprofile.health.tck.TCKBase;
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

            extensionsJar.addPackage(TCKBase.class.getPackage());
            extensionsJar.addPackage(Credentials.class.getPackage());
            extensionsJar.addPackage(CredentialsProvider.class.getPackage());
            extensionsJar.addPackage(HttpUriRequest.class.getPackage());
            extensionsJar.addPackage(HttpRequest.class.getPackage());


            WebArchive war = WebArchive.class.cast(archive);
            war.addAsLibraries(extensionsJar);
        }
    }
}
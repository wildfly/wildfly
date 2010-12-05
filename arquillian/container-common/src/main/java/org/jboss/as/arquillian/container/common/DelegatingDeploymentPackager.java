/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.common;

import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.arquillian.osgi.OSGiDeploymentPackager;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeploymentPackager;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.TestDeployment;
import org.jboss.arquillian.testenricher.osgi.OSGiTestEnricher;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

/**
 * A {@link DeploymentPackager} that delegates to deployment type specific packagers.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class DelegatingDeploymentPackager implements DeploymentPackager {

    public Archive<?> generateDeployment(Context context, TestDeployment testDeployment) {

        Archive<?> appArchive = testDeployment.getApplicationArchive();

        DeploymentPackager packager = null;
        if (isBundleArchive(context, appArchive))
            packager = new OSGiDeploymentPackager();
        else
            packager = new ModuleDeploymentPackager();

        packager.generateDeployment(context, testDeployment);

        return appArchive;
    }

    private boolean isBundleArchive(Context context, Archive<?> appArchive) {

        // Check if the test class is a valid OSGi injection target
        TestClass testCase = context.get(TestClass.class);
        if (OSGiTestEnricher.isInjectionTarget(testCase.getJavaClass()))
            return true;

        // Check if the archive contains a valid OSGi manifest
        Manifest manifest = getOrCreateManifest(appArchive);
        if (BundleInfo.isValidateBundleManifest(manifest))
            return true;

        return false;
    }

    static Manifest getOrCreateManifest(Archive<?> archive) {
        Manifest manifest;
        try {
            Node node = archive.get(JarFile.MANIFEST_NAME);
            if (node == null) {
                manifest = new Manifest();
                Attributes attributes = manifest.getMainAttributes();
                attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            } else {
                manifest = new Manifest(node.getAsset().openStream());
            }
            return manifest;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot obtain manifest", ex);
        }
    }
}

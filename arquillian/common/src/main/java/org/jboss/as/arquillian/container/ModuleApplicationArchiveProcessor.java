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
package org.jboss.as.arquillian.container;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

/**
 * An {@link ApplicationArchiveProcessor} for module test deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class ModuleApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    static final List<String> defaultDependencies = new ArrayList<String>();
    static {
        defaultDependencies.add("org.jboss.arquillian.api");
        defaultDependencies.add("org.jboss.arquillian.junit");
        defaultDependencies.add("org.jboss.arquillian.protocol.jmx");
        defaultDependencies.add("org.jboss.arquillian.spi");
        defaultDependencies.add("org.jboss.modules");
        defaultDependencies.add("org.jboss.msc");
        defaultDependencies.add("org.jboss.shrinkwrap.api");
        defaultDependencies.add("org.jboss.shrinkwrap.impl");
        defaultDependencies.add("org.junit");
    }
    static final List<String> jsfDependencies = new ArrayList<String>();
    static {
        jsfDependencies.add("org.jboss.jsfunit.arquillian");
        jsfDependencies.add("org.jboss.jsfunit.core");
        jsfDependencies.add("org.jboss.jsfunit.arquillian");
        jsfDependencies.add("net.sourceforge.htmlunit");
        jsfDependencies.add("org.apache.james.mime4j");
        jsfDependencies.add("org.apache.commons.codec");
        jsfDependencies.add("org.apache.commons.collections");
        jsfDependencies.add("org.apache.commons.io");
        jsfDependencies.add("org.apache.commons.lang");
        jsfDependencies.add("org.apache.commons.logging");
        jsfDependencies.add("org.apache.httpcomponents");
        jsfDependencies.add("org.apache.xalan");
        jsfDependencies.add("org.apache.xerces");
        jsfDependencies.add("org.w3c.css.sac");
        jsfDependencies.add("net.sourceforge.cssparser");
        jsfDependencies.add("net.sourceforge.nekohtml");
    }

    @Override
    public void process(Archive<?> appArchive, TestClass testClass) {
        if (appArchive instanceof ManifestContainer<?> == false)
            throw new IllegalArgumentException("ManifestContainer expected " + appArchive);

        final Manifest manifest = ManifestUtils.getOrCreateManifest(appArchive);
        Attributes attributes = manifest.getMainAttributes();
        String value = attributes.getValue("Dependencies");
        StringBuffer moduleDeps = new StringBuffer(value != null && value.trim().length() > 0 ? value : "org.jboss.modules");
        for (String dep : defaultDependencies) {
            if (moduleDeps.indexOf(dep) < 0)
                moduleDeps.append("," + dep);
        }
        if (Boolean.valueOf(System.getProperty("jboss.arquillian.jsfunit", "false"))) {
            for (String dep : jsfDependencies) {
                if (moduleDeps.indexOf(dep) < 0) {
                    moduleDeps.append("," + dep);
                }
            }
        }
        attributes.putValue("Dependencies", moduleDeps.toString());

        // Add the manifest to the archive
        ArchivePath path = ArchivePaths.create("META-INF", "MANIFEST.MF");
        appArchive.add(new Asset() {
                public InputStream openStream() {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        manifest.write(baos);
                        return new ByteArrayInputStream(baos.toByteArray());
                    } catch (IOException ex) {
                        throw new IllegalStateException("Cannot write manifest", ex);
                    }
                }
            }, path);
    }
}

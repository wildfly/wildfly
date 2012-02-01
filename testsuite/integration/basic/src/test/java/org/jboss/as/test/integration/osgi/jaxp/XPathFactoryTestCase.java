/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.osgi.jaxp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.InputSource;

import javax.inject.Inject;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.net.URL;

/**
 * Test XPathFactory.newInstance.
 *
 * @author thomas.diesler@jboss.com
 * @since 01-Feb-2012
 */
@RunWith(Arquillian.class)
public class XPathFactoryTestCase {

    @Inject
    public BundleContext context;
    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "xpath-test.jar");
        archive.addAsResource("osgi/jaxp/simple.xml", "simple.xml");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(XPathFactory.class, InputSource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testXPathFactory() throws Exception {
        XPathFactory factory = XPathFactory.newInstance();
        Assert.assertNotNull("XPathFactory not null", factory);
        XPath xpath = factory.newXPath();
        URL resurl = getClass().getClassLoader().getResource("simple.xml");
        InputSource inputSource = new InputSource(resurl.openStream());
        String content = xpath.evaluate("/root/child", inputSource);
        Assert.assertEquals("content", content);
    }
}
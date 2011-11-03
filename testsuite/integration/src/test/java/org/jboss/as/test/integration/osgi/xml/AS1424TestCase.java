/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.integration.osgi.xml;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;

/**
 * [AS7-1424] Cannot obtain SchemaFactory when TCCL is set
 * <p/>
 * https://issues.jboss.org/browse/AS7-1424
 *
 * @author Thomas.Diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class AS1424TestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "as1424");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(SchemaFactory.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSchemaFactoryDefaultTCCL() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        assertNotNull("SchemaFactory not null", schemaFactory);
    }

    @Test
    public void testSchemaFactoryNoTCCL() throws Exception {
        ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            assertNotNull("SchemaFactory not null", schemaFactory);
        } finally {
            Thread.currentThread().setContextClassLoader(ctxClassLoader);
        }
    }

    @Test
    public void testSchemaFactoryDeploymentTCCL() throws Exception {
        ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AS1424TestCase.class.getClassLoader());
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            assertNotNull("SchemaFactory not null", schemaFactory);
        } finally {
            Thread.currentThread().setContextClassLoader(ctxClassLoader);
        }
    }

    @Test
    public void testSchemaFactoryArqTCCL() throws Exception {
        ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Arquillian.class.getClassLoader());
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            assertNotNull("SchemaFactory not null", schemaFactory);
        } finally {
            Thread.currentThread().setContextClassLoader(ctxClassLoader);
        }
    }
}

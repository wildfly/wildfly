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
package org.jboss.as.test.integration.osgi.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.osgi.jaxb.bundle.CompanyType;
import org.jboss.as.test.integration.osgi.jaxb.bundle.ContactType;
import org.jboss.as.test.integration.osgi.jaxb.bundle.CourseBooking;
import org.jboss.as.test.integration.osgi.jaxb.bundle.ObjectFactory;
import org.jboss.as.test.integration.osgi.jaxb.bundle.StudentType;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;

/**
 * A test that uses JAXB to read an XML document.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Jul-2009
 */
@RunWith(Arquillian.class)
public class XMLBindingTestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-jaxb");
        archive.addClasses(CompanyType.class, ContactType.class, CourseBooking.class, ObjectFactory.class, StudentType.class);
        archive.addAsResource(ObjectFactory.class.getPackage(), "booking.xml", "booking.xml");
        archive.addAsResource(ObjectFactory.class.getPackage(), "booking.xsd", "booking.xsd");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages("javax.xml.bind", "javax.xml.bind.annotation");
                builder.addImportPackages("javax.xml.datatype", "javax.xml.namespace");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnmarshaller() throws Exception {

        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), ObjectFactory.class.getClassLoader());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        URL resURL = bundle.getResource("booking.xml");
        JAXBElement<CourseBooking> rootElement = (JAXBElement<CourseBooking>) unmarshaller.unmarshal(resURL.openStream());
        assertNotNull("root element not null", rootElement);

        CourseBooking booking = rootElement.getValue();
        assertNotNull("booking not null", booking);

        CompanyType company = booking.getCompany();
        assertNotNull("company not null", company);
        assertEquals("ACME Consulting", company.getName());
    }
}
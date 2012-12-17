/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.webapp;

import java.util.jar.Manifest;

import org.jboss.as.osgi.web.WebBundleURIParser;
import org.jboss.as.osgi.web.WebExtension;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test webbundle URI parsing
 *
 * @author thomas.diesler@jboss.com
 * @since 30-Nov-2012
 */
public class WebBundleParserTestCase {

    @Test
    public void testWarDeployment() throws Exception {

        Manifest manifest = WebBundleURIParser.parse("webbundle://foo?Bundle-SymbolicName=com.example");
        OSGiMetaData metadata = OSGiMetaDataBuilder.load(manifest);

        Assert.assertEquals("com.example", metadata.getBundleSymbolicName());
        Assert.assertEquals("foo", metadata.getHeader(WebExtension.WEB_CONTEXTPATH));
    }
}

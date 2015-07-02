/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
 *
 */

package org.jboss.as.test.integration.ws.schemalocations;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.helpers.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that schema locations are rewritten.
 *
 * CXF-6469
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SchemaLocationsRewriteTestCase {

    private final static String FAIL_MESSAGE = "Schema location was not rewritten.";

    @ArquillianResource
    URL baseUrl;

    private static final Logger log = Logger.getLogger(SchemaLocationsRewriteTestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "schema-location-rewrite.war");
        war.addPackage(SimpleService.class.getPackage()).
                addAsWebInfResource(SchemaLocationsRewriteTestCase.class.getPackage(), "SimpleService.wsdl", "wsdl/SimpleService.wsdl").
                addAsWebInfResource(SchemaLocationsRewriteTestCase.class.getPackage(), "imported/AnotherService.wsdl", "wsdl/imported/AnotherService.wsdl").
                addAsWebInfResource(SchemaLocationsRewriteTestCase.class.getPackage(), "imported/SimpleService.xsd", "wsdl/imported/SimpleService.xsd").
                addAsWebInfResource(SchemaLocationsRewriteTestCase.class.getPackage(), "imported/importedschema.xsd", "wsdl/imported/importedschema.xsd");
        war.addClass(SimpleService.class);
        return war;
    }

    @Test
    public void testSchemaLocationRewrittenWsdl() throws Exception {
        verifySchemaContains(new URL(baseUrl, "SimpleService?wsdl"), "?xsd=imported/SimpleService.xsd");
        verifySchemaContains(new URL(baseUrl, "SimpleService?wsdl"), "?wsdl=imported/AnotherService.wsdl");
    }

    @Test
    public void testSchemaLocationRewrittenNestedWsdl() throws Exception {
        verifySchemaContains(new URL(baseUrl, "SimpleService?wsdl=imported/AnotherService.wsdl"), "?xsd=SimpleService.xsd");
    }

    /**
     * CXF-6469
     */
    @Test
    public void testSchemaLocationRewrittenNestedXsd() throws Exception {
        verifySchemaContains(new URL(baseUrl, "SimpleService?xsd=imported/SimpleService.xsd"), "?xsd=imported/importedschema.xsd");
    }

    @Test
    public void testSchemaLocationRewrittenNestedXsdAltName() throws Exception {
        verifySchemaContains(new URL(baseUrl, "SimpleService?xsd=SimpleService.xsd"), "?xsd=importedschema.xsd");
    }

    private void verifySchemaContains(URL url, String s) throws IOException {
        String schema = IOUtils.toString(url.openStream());
        if (!schema.contains(s)) {
            log.log(Level.SEVERE, FAIL_MESSAGE + String.format(" String '%s' not found in:\n%s", s, schema));
            Assert.fail(FAIL_MESSAGE);
        }
    }

}

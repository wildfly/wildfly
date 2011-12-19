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
package org.jboss.as.test.xts.basic;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertTrue;


/**
 * XTS interop tests.
 *
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XTSInteropTestCase extends XTSTestBase {
    private static final Logger log = Logger.getLogger(XTSTestBase.class);

    private static final String PATH_SEPARATOR = System.getProperty("file.separator");

    private static final String JBOSSXTS_TESTS_PATH_PROPERTY = "org.jboss.as.test.xts.tests.path";
    private static final String JBOSSXTS_TESTS_PATH_DEFAULT  = "target" + PATH_SEPARATOR+ "jbossxts-tests" + PATH_SEPARATOR;

    private static String JBOSSXTS_TEST_PATH = System.getProperty(JBOSSXTS_TESTS_PATH_PROPERTY, JBOSSXTS_TESTS_PATH_DEFAULT);

    private static final String ARCHIVE_INTEROP11   = "interop11.war";
    private static final String ARCHIVE_SC007       = "sc007.war";

    public XTSInteropTestCase() {
        super();
        this.repeatable = false;
        this.headerRunName = "Execute";
    }


    @Deployment(name = "interop11", testable = false)
    public static Archive<?> deploymentInterop11() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_INTEROP11)
                .importFrom(new ZipFile(JBOSSXTS_TEST_PATH + PATH_SEPARATOR + ARCHIVE_INTEROP11)).as(WebArchive.class);
        return archive;
    }

/*  JBTM-914
    @Deployment(name = "sc007", testable = false)
    public static Archive<?> deploymentSC007() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_SC007)
                .importFrom(new ZipFile(JBOSSXTS_TEST_PATH + PATH_SEPARATOR + ARCHIVE_SC007)).as(WebArchive.class);
        return archive;
    }
*/


    @Test  @OperateOnDeployment("sc007")
    @Ignore("JBTM-914")
    public void testSC007(@ArquillianResource URL contextPath) throws Throwable {
        String outfile = getOutfileName("sc007");
        try {
            boolean res = doInteropTests(contextPath, "ParticipantService", "allTests", outfile);
            assertTrue("The sc007 tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The sc007 tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }

    @Test  @OperateOnDeployment("interop11")
    public void testInterop11AT(@ArquillianResource URL contextPath) throws Throwable {
        String outfile = getOutfileName("interop11-AT");
        try {
            boolean res = doInteropTests(contextPath, "ATParticipantService", "allATTests", outfile);
            assertTrue("The interop11-AT tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The interop11-AT tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }

    @Test  @OperateOnDeployment("interop11")
    public void testInterop11BA(@ArquillianResource URL contextPath) throws Throwable {
        String outfile = getOutfileName("interop11-BA");
        try {
            boolean res = doInteropTests(contextPath, "BAParticipantService", "allBATests", outfile);
            assertTrue("The interop11-BA tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The interop11-BA tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }


    private boolean doInteropTests(URL contextPath, String serviceURI, String testName, String outfile) throws Throwable {
        List<NameValuePair> params = new ArrayList<NameValuePair>(5);
        params.add(new BasicNameValuePair("serviceuri", contextPath.toString() + serviceURI));
        params.add(new BasicNameValuePair("test", testName));
        params.add(new BasicNameValuePair("testTimeout", "120000"));
        params.add(new BasicNameValuePair("resultPage", "/xmlresults"));

        return callTestServlet(contextPath.toString() + "test", params, outfile);
    }

    private String getOutfileName(String tag) {
        return JBOSSXTS_TEST_PATH + PATH_SEPARATOR + "TEST-xts.interop.tests." + tag + ".xml";
    }

}
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
package org.jboss.as.testsuite.integration.xts;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertTrue;

/**
 * XTS Unit tests.
 *
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XTSUnitTestCase extends XTSTestBase {
    private static final Logger log = Logger.getLogger(XTSUnitTestCase.class);

    private static final String ARCHIVE_WSAS    = "wsas-tests.ear";
    private static final String ARCHIVE_WSCF    = "wscf-tests.ear";
    private static final String ARCHIVE_WSC     = "ws-c-tests.ear";
    private static final String ARCHIVE_WST     = "ws-t-tests.ear";
    private static final String ARCHIVE_WSTX    = "wstx-tests.ear";
    private static final String OUTFILE_PATH    = "target/jbossxts-tests/";

    private static final String BASE_URL        = "http://localhost:8080";

    public XTSUnitTestCase() {
        super();
        this.repeatable = true;
    }

    @Deployment(name = "wsas", testable = false)
    public static Archive<?> deploymentWSAS() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSAS)
                .importFrom(new ZipFile("target/jbossxts-tests/" + ARCHIVE_WSAS)).as(EnterpriseArchive.class);
//        archive.as(ZipExporter.class).exportTo(new File("/tmp/deployment.zip"), true);
        return archive;
    }

    @Deployment(name = "wscf", testable = false)
    public static Archive<?> deploymentWSCF() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSCF)
                .importFrom(new ZipFile("target/jbossxts-tests/" + ARCHIVE_WSCF)).as(EnterpriseArchive.class);
        return archive;
    }

/*  FIXME: JBTM-900 affected deployments

    @Deployment(name = "wsc", testable = false)
    public static Archive<?> deploymentWSC() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSC)
                .importFrom(new ZipFile("target/jbossxts-tests/" + ARCHIVE_WSC)).as(EnterpriseArchive.class);
        return archive;
    }

    @Deployment(name = "wst", testable = false)
    public static Archive<?> deploymentWST() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WST)
                .importFrom(new ZipFile("target/jbossxts-tests/" + ARCHIVE_WST)).as(EnterpriseArchive.class);
        archive.as(ZipExporter.class).exportTo(new File("/tmp/wst.zip"), true);
        return archive;
    }

    @Deployment(name = "wstx", testable = false)
    public static Archive<?> deploymentWSTX() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSTX)
                .importFrom(new ZipFile("target/jbossxts-tests/" + ARCHIVE_WSTX)).as(EnterpriseArchive.class);
        return archive;
    }
*/

    @Test
    public void testWSAS() throws Throwable {
        String outfile = getOutfileName("wsas");
        boolean res = callTests("/wsas-tests/index.xml", outfile);
        assertTrue("The wsas tests failed, for more info see " + outfile, res);
    }

    @Test
    public void testWSCF() throws Throwable {
        String outfile = getOutfileName("wscf");
        boolean res = callTests("/wscf11-tests/index.xml", outfile);
        assertTrue("The wscf tests failed, for more info see " + outfile, res);
    }

    @Test
    @Ignore
    public void testWSC() throws Throwable {
        String outfile = getOutfileName("wsc");
        boolean res = callTests("/ws-c11-tests/index.xml", outfile);
        assertTrue("The wsc tests failed, for more info see " + outfile, res);
    }

    @Test
    @Ignore
    public void testWST() throws Throwable {
        String outfile = getOutfileName("wst");
        boolean res = callTests("/ws-t11-tests/index.xml", outfile);
        assertTrue("The wst tests failed, for more info see " + outfile, res);
    }

    @Test
    @Ignore
    public void testWSTX() throws Throwable {
        String outfile = getOutfileName("wstx");
        boolean res = callTests("/wstx11-tests/index.xml", outfile);
        assertTrue("The wstx tests failed, for more info see " + outfile, res);
    }

    private boolean callTests(String serviceURI, String outfile) throws Throwable {
        return callTestServlet(BASE_URL + serviceURI, null, outfile);
    }

    private String getOutfileName(String tag) {
        return OUTFILE_PATH + "TEST-xts.unit.tests." + tag + ".xml";
    }

}

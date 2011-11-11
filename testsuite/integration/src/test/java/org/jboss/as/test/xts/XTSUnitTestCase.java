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
package org.jboss.as.test.xts;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private static final String JBOSSXTS_TESTS_PATH_PROPERTY = "org.jboss.as.test.xts.tests.path";
    private static final String JBOSSXTS_TESTS_PATH_DEFAULT  = "target/jbossxts-tests/";

    private static final String ARCHIVE_WSAS    = "wsas-tests.ear";
    private static final String ARCHIVE_WSCF    = "wscf-tests.ear";
    private static final String ARCHIVE_WSC     = "ws-c-tests.ear";
    private static final String ARCHIVE_WST     = "ws-t-tests.ear";
    private static final String ARCHIVE_WSTX    = "wstx-tests.ear";

    private static final String BASE_URL        = "http://localhost:8080";

    private static String jbossxtsTestsPath;
    static {
        jbossxtsTestsPath = System.getProperty(JBOSSXTS_TESTS_PATH_PROPERTY, JBOSSXTS_TESTS_PATH_DEFAULT);
        log.info("jbossxtsTestsPath = " + jbossxtsTestsPath);
    }

    public XTSUnitTestCase() {
        super();
        this.repeatable = true;
    }

    @Deployment(name = "wsas", testable = false)
    public static Archive<?> deploymentWSAS() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSAS)
                .importFrom(new ZipFile(jbossxtsTestsPath + "/" + ARCHIVE_WSAS)).as(EnterpriseArchive.class);
//        archive.as(ZipExporter.class).exportTo(new File("/tmp/deployment.zip"), true);
        return archive;
    }

    @Deployment(name = "wscf", testable = false)
    public static Archive<?> deploymentWSCF() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSCF)
                .importFrom(new ZipFile(jbossxtsTestsPath + "/" + ARCHIVE_WSCF)).as(EnterpriseArchive.class);
        return archive;
    }

    @Deployment(name = "wsc", testable = false)
    public static Archive<?> deploymentWSC() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSC)
                .importFrom(new ZipFile(jbossxtsTestsPath + "/" + ARCHIVE_WSC)).as(EnterpriseArchive.class);
        return archive;
    }

    @Deployment(name = "wst", testable = false)
    public static Archive<?> deploymentWST() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WST)
                .importFrom(new ZipFile(jbossxtsTestsPath + "/" + ARCHIVE_WST)).as(EnterpriseArchive.class);
        return archive;
    }

    @Deployment(name = "wstx", testable = false)
    public static Archive<?> deploymentWSTX() throws Exception {
        Archive<?> archive = ShrinkWrap.create(ZipImporter.class, ARCHIVE_WSTX)
                .importFrom(new ZipFile(jbossxtsTestsPath + "/" + ARCHIVE_WSTX)).as(EnterpriseArchive.class);
        return archive;
    }


    @Test
    public void testWSAS() throws Throwable {
        String outfile = getOutfileName("wsas");
        try {
            boolean res = callTests("/wsas-tests/index.xml", outfile);
            assertTrue("The wsas tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The wsas tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }

    @Test
    public void testWSCF() throws Throwable {
        String outfile = getOutfileName("wscf");
        try {
            boolean res = callTests("/wscf11-tests/index.xml", outfile);
            assertTrue("The wscf tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The wscf tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }

    @Test
    public void testWSC() throws Throwable {
        String outfile = getOutfileName("wsc");
        try {
            boolean res = callTests("/ws-c11-tests/index.xml", outfile);
            assertTrue("The wsc tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The wsc tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }

    @Test
    public void testWST() throws Throwable {
        String outfile = getOutfileName("wst");
        try {
            boolean res = callTests("/ws-t11-tests/index.xml", outfile);
            assertTrue("The wst tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The wst tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }

    @Test
    public void testWSTX() throws Throwable {
        String outfile = getOutfileName("wstx");
        try {
            boolean res = callTests("/wstx11-tests/index.xml", outfile);
            assertTrue("The wstx tests failed, for more info see " + outfile, res);
        } catch (Throwable e) {
            throw new Throwable("The wstx tests failed with '" + e.getMessage() + "', for more info see " + outfile, e);
        }
    }


    private boolean callTests(String serviceURI, String outfile) throws Throwable {
        return callTestServlet(BASE_URL + serviceURI, null, outfile);
    }

    private String getOutfileName(String tag) {
        return jbossxtsTestsPath + "/" + "TEST-xts.unit.tests." + tag + ".xml";
    }

}

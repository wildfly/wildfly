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

package org.jboss.as.test.osgi;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.osgi.framework.Version;

/**
 * Abstract OSGi management operations
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Mar-2012
 */
public abstract class OSGiManagementTest {

    public static final long WAIT_TIMEOUT = 30000;
    public static final long WAIT_LINETIMEOUT = 1500;

    protected static CLIWrapper cli;

    @BeforeClass
    public static void beforeClass() throws Exception {
        OSGiManagementTest.initCLI();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        OSGiManagementTest.closeCLI();
    }

    public static CLIWrapper initCLI() throws IOException {
        if (cli == null) {
            cli = new CLIWrapper(true);
        }
        return cli;
    }

    public static void closeCLI() {
        try {
            if (cli == null) {
                cli.quit();
            }
        } finally {
            cli = null;
        }
    }

    public static CLIOpResult sendLine(String line) throws IOException {
        cli.sendLine(line);
        return cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
    }

    public static String getFrameworkStartLevel() throws IOException {
        cli.sendLine("/subsystem=osgi:read-attribute(name=startlevel)");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(cliresult.isIsOutcomeSuccess());
        return (String) cliresult.getResult();
    }

    @SuppressWarnings("unchecked")
    public static Long getBundleId(String symbolicName, Version version) throws IOException {
        Long result = null;
        cli.sendLine("/subsystem=osgi:read-resource(include-runtime=true,recursive=true)");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(cliresult.isIsOutcomeSuccess());
        Map<String, Object> bundlemap = cliresult.getNamedResultAsMap("bundle");
        for (Entry<String, Object> entry : bundlemap.entrySet()) {
            String auxid = entry.getKey();
            Map<String, Object> bundle = (Map<String, Object>) entry.getValue();
            if (bundle.get("symbolic-name").equals(symbolicName)) {
                Version auxver = Version.parseVersion((String) bundle.get("version"));
                if (version == null || version.equals(auxver)) {
                    result = Long.valueOf(auxid);
                    break;
                }
            }
        }
        return result;
    }

    public static boolean bundleStart(Object resId) throws IOException {
        cli.sendLine("/subsystem=osgi/bundle=" + resId + ":start");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        return cliresult.isIsOutcomeSuccess();
    }

    public static boolean bundleStop(Object resId) throws IOException {
        cli.sendLine("/subsystem=osgi/bundle=" + resId + ":stop");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        return cliresult.isIsOutcomeSuccess();
    }

    public static String getBundleState(Object resId) throws IOException {
        cli.sendLine("/subsystem=osgi/bundle=" + resId + ":read-attribute(name=state)");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        return (String) cliresult.getResult();
    }

    public static Map<String, Object> getBundleInfo(Long bundleId) throws IOException {
        cli.sendLine("/subsystem=osgi/bundle=" + bundleId + ":read-resource(include-runtime=true,recursive=true)");
        CLIOpResult cliresult = cli.readAllAsOpResult(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        return cliresult.getResultAsMap();
    }
}

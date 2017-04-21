/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.mgmt.elytron;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import static org.jboss.as.test.integration.security.common.Utils.createTemporaryFolder;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.FileSystemRealm;
import org.wildfly.test.security.common.elytron.Path;

/**
 * Test for authentication through http-interface secured by Elytron http-authentication-factory.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpMgmtInterfaceElytronAuthenticationTestCase {

    private static final String CONTAINER = "default-jbossas";

    private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";
    private static final String MANAGEMENT_FILESYSTEM_NAME = "mgmt-filesystem-name";

    private static File tempFolder;
    private static FileSystemRealm fileSystemRealm;

    private static final String USER = "user";
    private static final String CORRECT_PASSWORD = "password";

    private static String host;

    @ArquillianResource
    private static ContainerController containerController;

    public void prepareServerConfiguration() throws Exception {
        tempFolder = createTemporaryFolder("ely-" + HttpMgmtInterfaceElytronAuthenticationTestCase.class.getSimpleName());
        String fsRealmPath = tempFolder.getAbsolutePath() + File.separator + "fs-realm-users";
        try (CLIWrapper cli = new CLIWrapper(true)) {
            fileSystemRealm = FileSystemRealm.builder()
                    .withName(MANAGEMENT_FILESYSTEM_NAME)
                    .withPath(Path.builder().withPath(fsRealmPath).build())
                    .withUser(USER, CORRECT_PASSWORD)
                    .build();
            fileSystemRealm.create(cli);
            cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%1$s,role-decoder=groups-to-roles},{realm=local,role-mapper=super-user-mapper}],default-realm=%1$s,permission-mapper=default-permission-mapper)",
                    MANAGEMENT_FILESYSTEM_NAME));
            cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                    + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                    MANAGEMENT_FILESYSTEM_NAME, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
            cli.sendLine(String.format(
                    "/core-service=management/management-interface=http-interface:write-attribute(name=http-authentication-factory,value=%s)",
                    MANAGEMENT_FILESYSTEM_NAME));
            cli.sendLine("reload");
        }
    }

    public void resetServerConfiguration() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format(
                    "/core-service=management/management-interface=http-interface:undefine-attribute(name=http-authentication-factory)",
                    MANAGEMENT_FILESYSTEM_NAME));
            cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%s:remove()",
                    MANAGEMENT_FILESYSTEM_NAME, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", MANAGEMENT_FILESYSTEM_NAME));
            fileSystemRealm.remove(cli);
        }
        FileUtils.deleteDirectory(tempFolder);
    }


    @Test
    @InSequence(0)
    public void setupServer() throws Exception {
        containerController.start(CONTAINER);
        host = TestSuiteEnvironment.getServerAddress();
        prepareServerConfiguration();
    }

    /**
     * Test whether existing user with correct password has granted access through http-interface secured by Elytron
     * http-authentication-factory.
     */
    @Test
    @InSequence(1)
    public void testCorrectUser() throws Exception {
        Utils.makeCallWithBasicAuthn(createSimpleManagementOperationUrl(), USER, CORRECT_PASSWORD, SC_OK);
    }

    /**
     * Test whether existing user with wrong password has denied access through http-interface secured by Elytron
     * http-authentication-factory.
     */
    @Test
    @InSequence(1)
    public void testWrongPassword() throws Exception {
        Utils.makeCallWithBasicAuthn(createSimpleManagementOperationUrl(), USER, "wrongPassword", SC_UNAUTHORIZED);
    }

    /**
     * Test whether existing user with empty password has denied access through http-interface secured by Elytron
     * http-authentication-factory.
     */
    @Test
    @InSequence(1)
    public void testEmptyPassword() throws Exception {
        Utils.makeCallWithBasicAuthn(createSimpleManagementOperationUrl(), USER, "", SC_UNAUTHORIZED);
    }

    /**
     * Test whether non-existing user has denied access through http-interface secured by Elytron http-authentication-factory.
     */
    @Test
    @InSequence(1)
    public void testWrongUser() throws Exception {
        Utils.makeCallWithBasicAuthn(createSimpleManagementOperationUrl(), "wrongUser", CORRECT_PASSWORD, SC_UNAUTHORIZED);
    }

    /**
     * Test whether user with empty username has denied access through http-interface secured by Elytron
     * http-authentication-factory.
     */
    @Test
    @InSequence(1)
    public void testEmptyUser() throws Exception {
        Utils.makeCallWithBasicAuthn(createSimpleManagementOperationUrl(), "", CORRECT_PASSWORD, SC_UNAUTHORIZED);
    }

    @Test
    @InSequence(2)
    public void resetServer() throws Exception {
        try {
            resetServerConfiguration();
        } finally {
            containerController.stop(CONTAINER);
        }
    }

    private URL createSimpleManagementOperationUrl() throws URISyntaxException, IOException {
        return new URL("http://" + host + ":9990/management?operation=attribute&name=server-state");
    }

}

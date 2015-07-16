/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.domain.management.cli;

import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;


import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Test for logical hierarchical composition of Profiles and Socket Binding Groups.
 *
 * https://issues.jboss.org/browse/WFLY-4921
 *
 * @author Marek Kopecky <mkopecky@redhat.com>
 */
public class HierarchicalCompositionTestCase extends AbstractCliTestBase {

    private static Logger log = Logger.getLogger(HierarchicalCompositionTestCase.class);

    private static final String ORIGINAL_PROFILE = "default";

    private static final String ROOT_PROFILE = "hierarchical-composition-test-case-root";

    private static final String CHILD_PROFILE = "hierarchical-composition-test-case-default";

    private static final String ROOT_SUBSYSTEM = "mail";

    private static final String SERVER_GROUP = "main-server-group";

    private static final String SERVER = "main-one";

    private static final String HOST = "master";

    private static final String NEW_SOCKET_BINDING_GROUP = "hierarchical-composition-test-case-binding-group";

    private static final String NEW_SOCKET_BINDING = "hierarchical-composition-test-case-binding";

    private static final int NEW_SOCKET_BINDING_PORT = 1234;

    /**
     * Original profile from server group
     */
    String initProfile;

    /**
     * Original socket binding group from server group
     */
    String initSocketBindingGroup;


    @BeforeClass
    public static void before() throws Exception {
        CLITestSuite.createSupport(HierarchicalCompositionTestCase.class.getSimpleName());
        AbstractCliTestBase.initCLI(DomainTestSupport.masterAddress);
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
        CLITestSuite.stopSupport();
    }

    /**
     * Sends command line to CLI, validate and return output.
     *
     * @param line command line
     * @return CLI output
     */
    private String cliRequest(String line, boolean successRequired) {
        log.info(line);
        cli.sendLine(line);
        String output = cli.readOutput();
        if (successRequired) {
            assertTrue("CLI command \"" + line + " doesn't contain \"success\"", output.contains("success"));
        }
        return output;
    }

    /**
     * Sends command line to CLI, return only result.
     *
     * @param line command line
     * @return CLI output
     */
    private String cliGetResult(String line) {
        log.info(line);
        cli.sendLine(line);
        String output = null;
        try {
            output = (String) cli.readAllAsOpResult().getResult();
        } catch (IOException e) {
            Assert.fail("Fail to get result of CLI command: " + line);
        }
        return output;
    }

    /**
     * Reload server-one with selected profile and socket binding group
     *
     * @param profile profile for main-server-group
     * @param socketBindingGroup socket binding group for main-server-group
     */
    private void reloadServer(String profile, String socketBindingGroup) {
        cliRequest("/server-group=" + SERVER_GROUP + ":write-attribute(name=profile,value=" + profile + ")", true);
        cliRequest("/server-group=" + SERVER_GROUP + ":write-attribute(name=socket-binding-group,value=" + socketBindingGroup + ")", true);
        cliRequest("/host=" + HOST + "/server-config=" + SERVER + ":reload(blocking=true)", true);
    }

    /**
     * Get original profile and socket binding group
     */
    @Before
    public void init() {
        // get actual profile
        initProfile = cliGetResult("/server-group=" + SERVER_GROUP + ":read-attribute(name=profile)");

        // get actual socket binding group
        initSocketBindingGroup = cliGetResult("/server-group=" + SERVER_GROUP + ":read-attribute(name=socket-binding-group)");
    }

    /**
     * Reload server with original profile and socket binding group
     */
    @After
    public void cleanUp() {
        // revert profile to original value
        reloadServer(initProfile, initSocketBindingGroup);
    }

    /**
     * Basic smoke test for hierarchical composition of profiles
     */
    @Test
    public void testHierarchicalCompositionOfProfiles() throws IOException {
        // clone profile to child profile
        cliRequest("/profile=" + ORIGINAL_PROFILE + ":clone(to-profile=" + CHILD_PROFILE + ")", true);

        // remove mail subsystem from child profile
        cliRequest("/profile=" + CHILD_PROFILE + "/subsystem=" + ROOT_SUBSYSTEM + ":remove", true);

        // add root profile
        cliRequest("/profile=" + ROOT_PROFILE + ":add", true);

        // add mail subsystem to root profile
        cliRequest("/profile=" + ROOT_PROFILE + "/subsystem=" + ROOT_SUBSYSTEM + ":add", true);

        // set root profile as ancestor of child profile
        cliRequest("/profile=" + CHILD_PROFILE + ":list-add(name=includes,value=" + ROOT_PROFILE + ")", true);

        // reload server with child profile
        reloadServer(CHILD_PROFILE, initSocketBindingGroup);

        // check subsystems in server for mail subsystem
        String subsystemsInServer = cliRequest("ls /host=" + HOST + "/server=" + SERVER + "/subsystem", false);
        assertTrue("Child profile doesn't contain subsystem from root profile", subsystemsInServer.contains(ROOT_SUBSYSTEM));
    }

    /**
     * Basic smoke test for hierarchical composition of socket binding groups
     */
    @Test
    public void testHierarchicalCompositionOfSocketBindingGroups() throws IOException {
        // create new socket binding group
        cliRequest("/socket-binding-group=" + NEW_SOCKET_BINDING_GROUP + ":add(default-interface=public,includes=[" + initSocketBindingGroup + "])", true);

        // add new socket binding in new socket binding group
        cliRequest("/socket-binding-group=" + NEW_SOCKET_BINDING_GROUP + "/socket-binding=" + NEW_SOCKET_BINDING + ":add(port=" + NEW_SOCKET_BINDING_PORT + ")", true);

        // reload server with new socket binding group
        reloadServer(initProfile, NEW_SOCKET_BINDING_GROUP);

        // get all socket bindings from new socket binding group and check it
        String socketBinding = cliRequest("ls /host=" + HOST + "/server=" + SERVER + "/socket-binding-group=" + NEW_SOCKET_BINDING_GROUP + "/socket-binding", false);
        assertTrue(socketBinding.contains("http"));
        assertTrue(socketBinding.contains(NEW_SOCKET_BINDING));
    }
}

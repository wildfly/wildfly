/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
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

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class CapabilityCompletionTestCase extends AbstractCliTestBase {
    @BeforeClass
    public static void before() throws Exception {
        CLITestSuite.
                createSupport(CapabilityCompletionTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void after() throws Exception {
        CLITestSuite.stopSupport();
    }

    @Test
    public void testSocketBindingCapabilityCompletion() throws Exception {
        CLIWrapper cli = new CLIWrapper(true, DomainTestSupport.masterAddress, System.in);
        String command = "/profile=default/subsystem=remoting/connector=foo:add(socket-binding=";
        List<String> candidates = new ArrayList<>();
        cli.getCommandContext().getDefaultCommandCompleter().
                complete(cli.getCommandContext(), command, command.length(), candidates);
        assertFalse(candidates.toString(), candidates.isEmpty());
    }

}

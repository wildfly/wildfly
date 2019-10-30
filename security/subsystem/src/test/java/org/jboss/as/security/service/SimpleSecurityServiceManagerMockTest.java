/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.security.ISecurityManagement;
import org.jboss.security.RunAs;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Mock test for {@link SimpleSecurityManager}.
 *
 * Tests fix of JBEAP-11462 - run-as identity gets lost in some cases.
 *
 * @author Jiri Ondrusek (jondruse@redhat.com)
 */
public class SimpleSecurityServiceManagerMockTest {

    private SimpleSecurityManager simpleSecurityManager;

    private SecurityContext orig;

    @Before
    public void before() {
        orig = SecurityContextAssociation.getSecurityContext();

        ISecurityManagement delegate = mock(ISecurityManagement.class);

        simpleSecurityManager = new SimpleSecurityManager();//mock(SimpleSecurityManager.class, CALLS_REAL_METHODS);
        simpleSecurityManager.setSecurityManagement(delegate);
    }

    @After
    public void after() {
        SecurityContextAssociation.setSecurityContext(orig);
    }

    @Test
    public void testHandlingRunAs() {
        RunAs runAs1 = mock(RunAs.class);
        when(runAs1.toString()).thenReturn("RunAs-1");
        RunAs runAs2 = mock(RunAs.class);
        when(runAs2.toString()).thenReturn("RunAs-2");

        testHandlingRunAs(runAs1, runAs2);
        testHandlingRunAs(null, runAs2);
        testHandlingRunAs(runAs1, null);
        testHandlingRunAs(null, null);
    }

    /**
     * Even if outgoing RunAs from previous state is empty, incoming RunAs has to be used for incomingRunAs for current state.
     *
     * @param incomingRunAs Incoming RunAs for previous state.
     * @param outgoingRunAs Outgoung RunAs for previous state.
     */
    private void testHandlingRunAs(RunAs incomingRunAs, RunAs outgoingRunAs) {
        //previous security context mocks values from mehod
        SecurityContext context = mock(SecurityContext.class);
        if(outgoingRunAs != null) {
            when(context.getOutgoingRunAs()).thenReturn(outgoingRunAs);
        }
        if(incomingRunAs != null) {
            when(context.getIncomingRunAs()).thenReturn(incomingRunAs);
        }

        SecurityContextAssociation.setSecurityContext(context);
        simpleSecurityManager.push("test");
        SecurityContext result = SecurityContextAssociation.getSecurityContext();

        if(outgoingRunAs != null) {
            Assert.assertEquals("RunAs identity has to be same as previous outgoing RunAs.", outgoingRunAs, result.getIncomingRunAs());
        }
        else if(incomingRunAs != null) {
            Assert.assertEquals("RunAs identity has to be same as previous incoming RunAs.", incomingRunAs, result.getIncomingRunAs());
        }
        else {
            Assert.assertNull("RunAs identity has to be null.", result.getIncomingRunAs());
        }
    }

}

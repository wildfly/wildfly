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
package org.jboss.as.test.integration.management.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TODO add/remove-jms-topic/queue/cf commands are deprecated and shouldn't be used.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
public class JmsTestCase extends AbstractCliTestBase {

    @Test
    public void testAddRemoveJmsQueue() throws Exception {
        testAddJmsQueue();
        testRemoveJmsQueue();
    }

    @Test
    public void testAddRemoveJmsTopic() throws Exception {
        testAddJmsTopic();
        testRemoveJmsTopic();
    }

    private void testAddJmsQueue() throws Exception {

        // check the queue is not registered
        cli.sendLine("cd /subsystem=messaging/hornetq-server=default/jms-queue");
        cli.sendLine("ls");        
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("testJmsQueue"));              
        
        // create queue
        cli.sendLine("add-jms-queue --name=testJmsQueue");

        // check it is listed
        cli.sendLine("cd /subsystem=messaging/hornetq-server=default/jms-queue");
        cli.sendLine("ls");        
        ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(ls.contains("testJmsQueue"));              
    }

    private void testRemoveJmsQueue() throws Exception {

        // create queue
        cli.sendLine("remove-jms-queue --name=testJmsQueue");

        // check it is listed
        cli.sendLine("cd /subsystem=messaging/hornetq-server=default/jms-queue");
        cli.sendLine("ls");        
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("testJmsQueue"));              
    }    
    
    private void testAddJmsTopic() throws Exception {

        // check the queue is not registered
        cli.sendLine("cd /subsystem=messaging/hornetq-server=default/jms-topic");
        cli.sendLine("ls");        
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("testJmsTopic"));              
        
        // create queue
        cli.sendLine("add-jms-topic --name=testJmsTopic");

        // check it is listed
        cli.sendLine("cd /subsystem=messaging/hornetq-server=default/jms-topic");
        cli.sendLine("ls");        
        ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertTrue(ls.contains("testJmsTopic"));              
    }

    private void testRemoveJmsTopic() throws Exception {

        // create queue
        cli.sendLine("remove-jms-topic --name=testJmsTopic");

        // check it is listed
        cli.sendLine("cd /subsystem=messaging/hornetq-server=default/jms-topic");
        cli.sendLine("ls");        
        String ls = cli.readAllUnformated(WAIT_TIMEOUT, WAIT_LINETIMEOUT);
        assertFalse(ls.contains("testJmsTopic"));              
    }       
}

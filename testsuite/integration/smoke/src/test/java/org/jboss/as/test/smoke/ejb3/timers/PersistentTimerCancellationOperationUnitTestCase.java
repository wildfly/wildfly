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

package org.jboss.as.test.smoke.ejb3.timers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class PersistentTimerCancellationOperationUnitTestCase {

    public static final String NAME = "cancelled-persistent-timer.war";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, NAME);
        war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addClass(TimerStartup.class);

        return war;
    }

    @Test
    public void subsystem_query_retrieves_cancelled_and_valid_timers() throws Exception {
        final ModelNode address = new ModelNode();
        address.add("deployment", NAME);
        address.add("subsystem", "ejb3");
        address.add("singleton-bean", TimerStartup.class.getSimpleName());
        address.add("service", "timer-service");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

        ModelNode node = managementClient.getControllerClient().execute(operation);
        assertThat(node.get(ModelDescriptionConstants.OUTCOME).asString(), is("success"));

        // Let's verify timers
        ModelNode timersNode = node.get(ModelDescriptionConstants.RESULT).get("timer");

        Set<String> timerIDs = timersNode.keys();
        assertThat(timerIDs.size(), is(2));     // 2 timers expected

        List<ModelNode> timers = timerIDs.stream().map(id -> timersNode.get(id)).collect(Collectors.toList());

        List<ModelNode> nonCancelledTimers = timers.stream().filter(n -> n.hasDefined("info")).collect(Collectors.toList());
        assertThat(nonCancelledTimers.size(), is(1));   // one and only one with timer information, ie the one non cancelled

        List<ModelNode> cancelledTimers = timers.stream().filter(n -> !n.hasDefined("info")).collect(Collectors.toList());
        assertThat(cancelledTimers.size(), is(1));   // one without timer information, the one cancelled
    }
}

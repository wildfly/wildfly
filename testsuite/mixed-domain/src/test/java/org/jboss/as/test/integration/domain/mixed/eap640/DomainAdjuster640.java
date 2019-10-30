/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.domain.mixed.eap640;

import static org.jboss.as.controller.client.helpers.ClientConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.as.controller.operations.common.Util.getUndefineAttributeOperation;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.mixed.LegacySubsystemConfigurationUtil;
import org.jboss.as.test.integration.domain.mixed.eap700.DomainAdjuster700;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 6.4.0 legacy slaves.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster640 extends DomainAdjuster700 {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withMasterServers) throws Exception {
        final List<ModelNode> list = super.adjustForVersion(client, profileAddress, withMasterServers);
        switch(profileAddress.getElement(0).getValue()) {
            case "full-ha":
                list.addAll(adjustJGroups(profileAddress.append(SUBSYSTEM, "jgroups")));
                list.addAll(adjustInfinispan(true, profileAddress.append(SUBSYSTEM, "infinispan")));
                list.addAll(replaceActiveMqWithMessaging(profileAddress, profileAddress.append(SUBSYSTEM, "messaging-activemq")));
                list.addAll(replaceIiopOpenJdk(client, profileAddress.append(SUBSYSTEM, "iiop-openjdk")));
                break;
            case "full":
                list.addAll(adjustInfinispan(false, profileAddress.append(SUBSYSTEM, "infinispan")));
                list.addAll(replaceActiveMqWithMessaging(profileAddress, profileAddress.append(SUBSYSTEM, "messaging-activemq")));
                list.addAll(replaceIiopOpenJdk(client, profileAddress.append(SUBSYSTEM, "iiop-openjdk")));
                break;
            default:
                list.addAll(adjustInfinispan(false, profileAddress.append(SUBSYSTEM, "infinispan")));
                list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.messaging-activemq")));
                list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.iiop-openjdk")));
                break;
        }
        list.addAll(removeBatch(profileAddress.append(SUBSYSTEM, "batch-jberet")));
        list.addAll(removeBeanValidation(profileAddress.append(SUBSYSTEM, "bean-validation")));
        list.addAll(adjustEe(profileAddress.append(SUBSYSTEM, "ee")));
        list.addAll(adjustEjb3(profileAddress.append(SUBSYSTEM, "ejb3")));
        list.addAll(adjustRemoting(profileAddress.append(SUBSYSTEM, "remoting")));
        list.addAll(removeRequestController(profileAddress.append(SUBSYSTEM, "request-controller")));
        list.addAll(removeSecurityManager(profileAddress.append(SUBSYSTEM, "security-manager")));
        list.addAll(removeSingletonDeployer(profileAddress.append(SUBSYSTEM, "singleton")));
        list.addAll(replaceUndertowWithWeb(profileAddress.append(SUBSYSTEM, "undertow")));
        list.addAll(adjustWeld(profileAddress.append(SUBSYSTEM, "weld")));

        //io must be removed after undertow due to capabilities/requirements
        list.addAll(removeIo(profileAddress.append(SUBSYSTEM, "io")));

        return list;
    }

    private Collection<? extends ModelNode> removeBatch(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //batch and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.batch.jberet")));
        return list;
    }

    private Collection<? extends ModelNode> removeBeanValidation(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //bean-validation and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.bean-validation")));
        return list;
    }

    private List<ModelNode> adjustEe(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //Concurrency resources are not available
        list.add(createRemoveOperation(subsystem.append("context-service", "default")));
        list.add(createRemoveOperation(subsystem.append("managed-thread-factory", "default")));
        list.add(createRemoveOperation(subsystem.append("managed-executor-service", "default")));
        list.add(createRemoveOperation(subsystem.append("managed-scheduled-executor-service", "default")));
        //default-bindings are not available
        list.add(createRemoveOperation(subsystem.append("service", "default-bindings")));
        return list;
    }

    private List<ModelNode> adjustEjb3(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        list.add(
                getUndefineAttributeOperation(
                        subsystem.append("strict-max-bean-instance-pool", "slsb-strict-max-pool"), "derive-size"));
        list.add(
                getUndefineAttributeOperation(
                        subsystem.append("strict-max-bean-instance-pool", "mdb-strict-max-pool"), "derive-size"));
        return list;
    }

    private Collection<? extends ModelNode> removeIo(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //io and extension don't exist so remove them
        //We also must remove the remoting requirement for io worker capability
        list.add(createRemoveOperation(subsystem.getParent().append(SUBSYSTEM, "remoting").append("configuration", "endpoint")));
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.io")));
        return list;
    }

    private Collection<? extends ModelNode> removeRequestController(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //request controller and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.request-controller")));
        return list;
    }

    private Collection<? extends ModelNode> adjustRemoting(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //The endpoint configuration does not exist
        // BES 2015/07/15 -- this is done in removeIO now so the io worker capability and the requirement for it
        // both go in the same op
        //list.add(createRemoveOperation(subsystem.append("configuration", "endpoint")));

        //Replace the http-remoting connector with a normal remoting-connector. This needs the remoting socket binding,
        //so add that too
        list.add(createRemoveOperation(subsystem.append("http-connector", "http-remoting-connector")));
        ModelNode addSocketBinding =
                createAddOperation(
                        PathAddress.pathAddress(SOCKET_BINDING_GROUP, "full-ha-sockets")
                                .append(SOCKET_BINDING, "remoting"));
        addSocketBinding.get("port").set(4447);
        list.add(addSocketBinding);
        ModelNode addRemoting = createAddOperation(subsystem.append("connector", "remoting-connector"));
        addRemoting.get("socket-binding").set("remoting");
        addRemoting.get("security-realm").set("ApplicationRealm");
        list.add(addRemoting);
        return list;

    }


    private Collection<? extends ModelNode> removeSingletonDeployer(PathAddress subsystem) {
        List<ModelNode> list = new ArrayList<>();
        if("full-ha".equals(subsystem.getElement(0).getValue())) {
            //singleton subsystem and extension doesn't exist
            list.add(createRemoveOperation(subsystem));
        }
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.clustering.singleton")));
        return list;
    }


    private List<ModelNode> adjustInfinispan(final boolean isFullHa ,final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        if(isFullHa) {
            list.add(getWriteAttributeOperation(subsystem.append("cache-container", "server").append("transport", "jgroups"), "stack", new ModelNode("udp")));
        }
        this.adjustInfinispanStatisticsEnabled(list, subsystem, isFullHa);

        return list;
    }

    public void adjustInfinispanStatisticsEnabled(final List<ModelNode> list, final PathAddress subsystem, boolean isFullHa) {
        // No-op for 6.4.0
    }

    private List<ModelNode> adjustJGroups(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();

        list.add(getWriteAttributeOperation(subsystem, "default-stack", new ModelNode("tcp")));

        // pbcast.NAKACK2 does not exist, use pbcast.NAKACK instead
        // UNICAST3 does not exist, use UNICAST2 instead
        //All this code is to remove and re-add the protocols including the rename to preserve the order
        //TODO once we support indexed adds, use those instead

        // udp stack
        PathAddress udp = subsystem.append("stack", "udp");
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.NAKACK2")));
        list.add(createRemoveOperation(udp.append("protocol", "UNICAST3")));
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.STABLE")));
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.GMS")));
        list.add(createRemoveOperation(udp.append("protocol", "UFC")));
        list.add(createRemoveOperation(udp.append("protocol", "MFC")));
        list.add(createRemoveOperation(udp.append("protocol", "FRAG2")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.NAKACK")));
        list.add(createAddOperation(udp.append("protocol", "UNICAST2")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.STABLE")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.GMS")));
        list.add(createAddOperation(udp.append("protocol", "UFC")));
        list.add(createAddOperation(udp.append("protocol", "MFC")));
        list.add(createAddOperation(udp.append("protocol", "FRAG2")));
        list.add(createAddOperation(udp.append("protocol", "RSVP")));

        // tcp stack
        PathAddress tcp = subsystem.append("stack", "tcp");
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.NAKACK2")));
        list.add(createRemoveOperation(tcp.append("protocol", "UNICAST3")));
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.STABLE")));
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.GMS")));
        list.add(createRemoveOperation(tcp.append("protocol", "MFC")));
        list.add(createRemoveOperation(tcp.append("protocol", "FRAG2")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.NAKACK")));
        list.add(createAddOperation(tcp.append("protocol", "UNICAST2")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.STABLE")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.GMS")));
        list.add(createAddOperation(tcp.append("protocol", "UFC")));
        list.add(createAddOperation(tcp.append("protocol", "FRAG2")));
        list.add(createAddOperation(tcp.append("protocol", "RSVP")));

        return list;
    }

    private Collection<? extends ModelNode> replaceActiveMqWithMessaging(PathAddress profileAddress, PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //messaging-activemq does not exist, remove it and the extension
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.messaging-activemq")));

        //Add legacy messaging extension
        list.add(createAddOperation(PathAddress.pathAddress(EXTENSION, "org.jboss.as.messaging")));

        //Get the subsystem add operations (since the subsystem is huge, and there is a template, use the util)
        LegacySubsystemConfigurationUtil util =
                new LegacySubsystemConfigurationUtil(
                        new org.jboss.as.messaging.MessagingExtension(), profileAddress, "messaging", "ha", "subsystem-templates/messaging.xml");

        list.addAll(util.getSubsystemOperations());


        //Now adjust the things from the template which are not available in the legacy server

        //http acceptors and connectors are not available
        PathAddress messaging = profileAddress.append(SUBSYSTEM, "messaging");
        PathAddress server = messaging.append("hornetq-server", "default");
        list.add(createRemoveOperation(server.append("http-acceptor", "http-acceptor")));
        list.add(createRemoveOperation(server.append("http-acceptor", "http-acceptor-throughput")));
        list.add(createRemoveOperation(server.append("http-connector", "http-connector")));
        list.add(createRemoveOperation(server.append("http-connector", "http-connector-throughput")));
        //TODO here we should add a remote connector, for now use the in-vm one
        list.add(getWriteAttributeOperation(server.append("broadcast-group", "bg-group1"), "connectors",
                new ModelNode().add("in-vm")));

        return list;
    }

    private Collection<? extends ModelNode> removeSecurityManager(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //security manager and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.security.manager")));
        return list;
    }


    private Collection<? extends ModelNode> replaceUndertowWithWeb(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();

        //Add JBoss Web extension first so we can replace subsystems with one composite op
        //FIXME extension add needs to be outside of the batch due to https://issues.jboss.org/browse/WFCORE-323
        list.add(createAddOperation(PathAddress.pathAddress(EXTENSION, "org.jboss.as.web")));

        //Replace Undertow with Web in a single composite op preserving the fake capability satisfied
        ModelNode compositeOp = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = compositeOp.get(STEPS);

        //Undertow does not exist, remove it and the extension
        steps.add(createRemoveOperation(subsystem));

        //Add JBoss Web and subsystem
        final PathAddress web = subsystem.getParent().append(SUBSYSTEM, "web");
        final ModelNode addWeb = Util.createAddOperation(web);
        addWeb.get("default-virtual-server").set("default-host");
        addWeb.get("native").set("false");
        steps.add(addWeb);
        steps.add(createAddOperation(web.append("configuration", "container")));
        steps.add(createAddOperation(web.append("configuration", "static-resources")));
        steps.add(createAddOperation(web.append("configuration", "jsp-configuration")));
        ModelNode addHttp = Util.createAddOperation(web.append("connector", "http"));
        addHttp.get("protocol").set("HTTP/1.1");
        addHttp.get("scheme").set("http");
        addHttp.get("socket-binding").set("http");
        steps.add(addHttp);
        ModelNode addAjp = Util.createAddOperation(web.append("connector", "ajp"));
        addAjp.get("protocol").set("AJP/1.3");
        addAjp.get("scheme").set("http");
        addAjp.get("socket-binding").set("ajp");
        steps.add(addAjp);
        ModelNode addVirtualServer = Util.createAddOperation(web.append("virtual-server", "default-host"));
        addVirtualServer.get("enable-welcome-root").set(true);
        addVirtualServer.get("alias").add("localhost").add("example.com");
        steps.add(addVirtualServer);

        list.add(compositeOp);
        //FIXME extension remove needs to be outside of the batch due to NPE; see https://issues.jboss.org/browse/WFCORE-323
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.undertow")));

        return list;
    }

    private List<ModelNode> replaceIiopOpenJdk(final DomainClient client, final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //iiop-openjdk does not exist it used to be called jacorb

        //Remove the iiop-openjdk subsystem and extension
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.iiop-openjdk")));
        //Add the jacorb extension
        list.add(createAddOperation(PathAddress.pathAddress(EXTENSION, "org.jboss.as.jacorb")));

        //This should be the same as in the iiop-openjdk.xml subsystem template
        ModelNode add = createAddOperation(subsystem.getParent().append(SUBSYSTEM, "jacorb"));
        add.get("socket-binding").set("iiop");
        add.get("ssl-socket-binding").set("iiop-ssl");
        add.get("transactions").set("spec");
        add.get("security").set("identity");
        list.add(add);

        return list;
    }

    private Collection<? extends ModelNode> adjustWeld(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        list.add(getWriteAttributeOperation(subsystem, "require-bean-descriptor", true));
        list.add(getWriteAttributeOperation(subsystem, "non-portable-mode", true));
        return list;
    }
}

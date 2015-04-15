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

package org.jboss.as.test.integration.domain.mixed.eap630;

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

import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsExtension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.remoting.RemotingExtension;
import org.jboss.as.test.integration.domain.mixed.DomainAdjuster;
import org.jboss.as.weld.WeldExtension;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.batch.BatchSubsystemExtension;
import org.wildfly.extension.beanvalidation.BeanValidationExtension;
import org.wildfly.extension.io.IOExtension;
import org.wildfly.extension.requestcontroller.RequestControllerExtension;
import org.wildfly.extension.security.manager.SecurityManagerExtension;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.iiop.openjdk.IIOPExtension;

/**
 * Does adjustments to the domain model for 6.3.0 legacy slaves
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster630 extends DomainAdjuster {
    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress) throws Exception {
        final List<ModelNode> list = new ArrayList<>();

        list.addAll(removeBatch(profileAddress.append(SUBSYSTEM, BatchSubsystemExtension.SUBSYSTEM_NAME)));
        list.addAll(removeBeanValidation(profileAddress.append(SUBSYSTEM, BeanValidationExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustEe(profileAddress.append(SUBSYSTEM, EeExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustEjb3(profileAddress.append(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME)));
        list.addAll(replaceIiopOpenJdk(client, profileAddress.append(SUBSYSTEM, IIOPExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustInfinispan(profileAddress.append(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME)));
        list.addAll(removeIo(profileAddress.append(SUBSYSTEM, IOExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustJGroups(profileAddress.append(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustMessaging(profileAddress.append(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustRemoting(profileAddress.append(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME)));
        list.addAll(adjustWeld(profileAddress.append(SUBSYSTEM, WeldExtension.SUBSYSTEM_NAME)));
        list.addAll(removeRequestController(profileAddress.append(SUBSYSTEM, RequestControllerExtension.SUBSYSTEM_NAME)));
        list.addAll(removeSecurityManager(profileAddress.append(SecurityManagerExtension.SUBSYSTEM_PATH)));
        list.addAll(replaceUndertowWithWeb(profileAddress.append(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME)));


        //Temporary workaround, something weird is going on in infinispan/jgroups so let's get rid of those for now
        //TODO Reenable these subsystems, it is important to see if we boot with them configured although the tests don't use clustering
        list.add(createRemoveOperation(profileAddress.append(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME)));
        list.add(createRemoveOperation(profileAddress.append(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME)));

        return list;
    }


    private Collection<? extends ModelNode> removeBatch(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //batch and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.batch")));
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
        //passivation-disabled-cache-ref is not understood
        list.add(
                getUndefineAttributeOperation(
                        subsystem, "default-sfsb-passivation-disabled-cache"));
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

        //This shoud be the same as in the iiop-openjdk.xml subsystem template
        ModelNode add = createAddOperation(subsystem.getParent().append(SUBSYSTEM, "jacorb"));
        add.get("socket-binding").set("iiop");
        add.get("ssl-socket-binding").set("iiop-ssl");
        add.get("transactions").set("spec");
        add.get("security").set("identity");
        list.add(add);

        return list;
    }

    private List<ModelNode> adjustInfinispan(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //Statistics need to be enabled for all cache containers and caches
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "server")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "server").append("replicated-cache", "default")));
        list.add(getWriteAttributeOperation(subsystem.append("cache-container", "server").append("transport", "TRANSPORT"), "stack", new ModelNode("udp ")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "web")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "web").append("distributed-cache", "dist")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "ejb")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "ejb").append("distributed-cache", "dist")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate").append("invalidation-cache", "entity")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate").append("local-cache", "local-query")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate").append("replicated-cache", "timestamps")));
        return list;
    }

    private Collection<? extends ModelNode> removeIo(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //io and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.io")));
        return list;
    }


    private List<ModelNode> adjustJGroups(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //default-channel is not understood
        list.add(
                getUndefineAttributeOperation(
                        subsystem, "default-channel"));
        //In fact channels don't work
        list.add(createRemoveOperation(subsystem.append("channel", "ee")));

        // pbcast.NAKACK2 does not exist, use pbcast.NAKACK instead
        // UNICAST3 does not exist, use UNICAST2 instead
        //All this code is to remove and re-add the protocols including the rename to preserve the order
        //TODO once we support indexed adds, use those instead
        //udp
        PathAddress udp = subsystem.append("stack", "udp");
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.NAKACK2")));
        list.add(createRemoveOperation(udp.append("protocol", "UNICAST3")));
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.STABLE")));
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.GMS")));
        list.add(createRemoveOperation(udp.append("protocol", "UFC")));
        list.add(createRemoveOperation(udp.append("protocol", "MFC")));
        list.add(createRemoveOperation(udp.append("protocol", "FRAG2")));
        list.add(createRemoveOperation(udp.append("protocol", "RSVP")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.NAKACK")));
        list.add(createAddOperation(udp.append("protocol", "UNICAST2")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.STABLE")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.GMS")));
        list.add(createAddOperation(udp.append("protocol", "UFC")));
        list.add(createAddOperation(udp.append("protocol", "MFC")));
        list.add(createAddOperation(udp.append("protocol", "FRAG2")));
        list.add(createAddOperation(udp.append("protocol", "RSVP")));

        //udp
        PathAddress tcp = subsystem.append("stack", "tcp");
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.NAKACK2")));
        list.add(createRemoveOperation(tcp.append("protocol", "UNICAST3")));
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.STABLE")));
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.GMS")));
        list.add(createRemoveOperation(tcp.append("protocol", "MFC")));
        list.add(createRemoveOperation(tcp.append("protocol", "FRAG2")));
        list.add(createRemoveOperation(tcp.append("protocol", "RSVP")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.NAKACK")));
        list.add(createAddOperation(tcp.append("protocol", "UNICAST2")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.STABLE")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.GMS")));
        list.add(createAddOperation(tcp.append("protocol", "UFC")));
        list.add(createAddOperation(tcp.append("protocol", "FRAG2")));
        list.add(createAddOperation(tcp.append("protocol", "RSVP")));

        return list;
    }

    private List<ModelNode> adjustMessaging(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //http acceptors and connectors are not available
        PathAddress server = subsystem.append("hornetq-server", "default");
        list.add(createRemoveOperation(server.append("http-acceptor", "http-acceptor")));
        list.add(createRemoveOperation(server.append("http-acceptor", "http-acceptor-throughput")));
        list.add(createRemoveOperation(server.append("http-connector", "http-connector")));
        list.add(createRemoveOperation(server.append("http-connector", "http-connector-throughput")));

        //TODO here we should add a remote connector, for now use the in-vm one
        list.add(getWriteAttributeOperation(server.append("broadcast-group", "bg-group1"), "connectors",
                new ModelNode().add("in-vm")));

        return list;
    }

    private Collection<? extends ModelNode> adjustRemoting(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //The endpoint configuration does not exist
        list.add(createRemoveOperation(subsystem.append("configuration", "endpoint")));

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

    private Collection<? extends ModelNode> removeRequestController(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        //request controller and extension don't exist
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.request-controller")));
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
        //Undertow does not exist, remove it and the extension
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.undertow")));

        //Add JBoss Web extension and subsystem
        list.add(createAddOperation(PathAddress.pathAddress(EXTENSION, "org.jboss.as.web")));
        final PathAddress web = subsystem.getParent().append(SUBSYSTEM, "web");
        final ModelNode addWeb = Util.createAddOperation(web);
        addWeb.get("default-virtual-server").set("default-host");
        addWeb.get("native").set("false");
        list.add(addWeb);
        list.add(createAddOperation(web.append("configuration", "container")));
        list.add(createAddOperation(web.append("configuration", "static-resources")));
        list.add(createAddOperation(web.append("configuration", "jsp-configuration")));
        ModelNode addHttp = Util.createAddOperation(web.append("connector", "http"));
        addHttp.get("protocol").set("HTTP/1.1");
        addHttp.get("scheme").set("http");
        addHttp.get("socket-binding").set("http");
        list.add(addHttp);
        ModelNode addAjp = Util.createAddOperation(web.append("connector", "ajp"));
        addAjp.get("protocol").set("AJP/1.3");
        addAjp.get("scheme").set("http");
        addAjp.get("socket-binding").set("ajp");
        list.add(addAjp);
        ModelNode addVirtualServer = Util.createAddOperation(web.append("virtual-server", "default-host"));
        addVirtualServer.get("enable-welcome-root").set(true);
        addVirtualServer.get("alias").add("localhost").add("example.com");
        list.add(addVirtualServer);

        return list;
    }

    private Collection<? extends ModelNode> adjustWeld(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        list.add(getWriteAttributeOperation(subsystem, "require-bean-descriptor", true));
        list.add(getWriteAttributeOperation(subsystem, "non-portable-mode", true));
        return list;
    }

    private ModelNode setStatisticsEnabledTrue(final PathAddress addr) {
        return getWriteAttributeOperation(addr, "statistics-enabled", true);
    }

    @Override
    protected String getJaspiTestAuthModuleName() {
        return "org.jboss.as.web.security.jaspi.modules.HTTPBasicServerAuthModule";
    }
}

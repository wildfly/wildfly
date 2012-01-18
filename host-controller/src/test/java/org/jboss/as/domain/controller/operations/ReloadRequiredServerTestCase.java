/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.operations.ServerConfigGroupWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReloadRequiredServerTestCase extends AbstractOperationTestCase {

    @Test
    public void testChangeServerGroupProfile() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("old");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final Resource profileConfig = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(PROFILE, "some-profile"), profileConfig);

        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("some-profile");

        new ServerGroupProfileWriteAttributeHandler(RESOURCE_REGISTRATION, HOST_INFO).execute(operationContext, operation);

        operationContext.verify();
    }

    @Test
    public void testChangeServerGroupProfileNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("old");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final Resource profileConfig = Resource.Factory.create();
        operationContext.root.registerChild(PathElement.pathElement(PROFILE, "some-profile"), profileConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("old");

        new ServerGroupProfileWriteAttributeHandler(RESOURCE_REGISTRATION, HOST_INFO).execute(operationContext, operation);

        operationContext.verify();
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerGroupInvalidProfileNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "group-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("old");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "group-one"), serverConfig);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(NAME).set(PROFILE);
        operation.get(VALUE).set("does-not-exist");

        new ServerGroupProfileWriteAttributeHandler(RESOURCE_REGISTRATION, HOST_INFO).execute(operationContext, operation);

        operationContext.verify();
    }

    @Test
    public void testChangeServerConfigGroup() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final Resource serverConfig = Resource.Factory.create();
        serverConfig.getModel().get(PROFILE).set("whatever");
        operationContext.root.registerChild(PathElement.pathElement(SERVER_GROUP, "new-group"), serverConfig);

        operationContext.expectStep(PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER, "server-one")));

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("new-group");

        new ServerConfigGroupWriteAttributeHandler(RESOURCE_REGISTRATION, HOST_INFO).execute(operationContext, operation);

        operationContext.verify();
    }


    @Test
    public void testChangeServerConfigGroupNoChange() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("group-one");

        new ServerConfigGroupWriteAttributeHandler(RESOURCE_REGISTRATION, HOST_INFO).execute(operationContext, operation);

        operationContext.verify();
    }

    @Test(expected=OperationFailedException.class)
    public void testChangeServerConfigGroupBadGroup() throws Exception {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, "localhost"), PathElement.pathElement(SERVER_CONFIG, "server-one"));
        final MockOperationContext operationContext = getOperationContext(pa);

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(pa.toModelNode());
        operation.get(NAME).set(GROUP);
        operation.get(VALUE).set("bad-group");

        new ServerConfigGroupWriteAttributeHandler(RESOURCE_REGISTRATION, HOST_INFO).execute(operationContext, operation);
    }

}

/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.shallow;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Abstract class to Add a ShallowResource.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public abstract class ShallowResourceAdd extends AbstractAddStepHandler {


    protected ShallowResourceAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        //do nothing
    }

    @Override
    protected Resource createResource(OperationContext context) {
        final Resource toAdd = Resource.Factory.create(true);
        context.addResource(PathAddress.EMPTY_ADDRESS, toAdd);
        return toAdd;
    }
}

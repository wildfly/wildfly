/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import java.util.Collection;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * <p>
 * A handler for the "add" operation that always puts the process in "reload-required" state.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ReloadRequiredAddStepHandler extends AbstractAddStepHandler {

    public ReloadRequiredAddStepHandler(final AttributeDefinition... attributes) {
        super(attributes);
    }

    public ReloadRequiredAddStepHandler(final Collection<AttributeDefinition> attributes) {
        super(attributes);
    }

    @Override
    protected boolean requiresRuntime(final OperationContext context) {
        return !context.isBooting() && super.requiresRuntime(context);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) {
        context.reloadRequired();
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        context.revertReloadRequired();
    }
}

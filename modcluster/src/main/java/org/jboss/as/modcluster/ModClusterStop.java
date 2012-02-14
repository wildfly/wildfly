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

package org.jboss.as.modcluster;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;

// implements ModelQueryOperationHandler, DescriptionProvider
public class ModClusterStop implements OperationStepHandler, DescriptionProvider{

    static final ModClusterStop INSTANCE = new ModClusterStop();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getStopDescription(locale);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        if (context.isNormalServer() && context.getServiceRegistry(false).getService(ModClusterService.NAME)!=null) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ServiceController<?> controller = context.getServiceRegistry(false).getService(ModClusterService.NAME);
                    ModCluster modcluster = (ModCluster) controller.getValue();
                    List<Property> list = operation.asPropertyList();
                    Iterator<Property> it= list.iterator();
                    int waittime = 10;
                    while(it.hasNext()) {
                        Property prop= it.next();
                        if (prop.getName().equals("waittime")) {
                            waittime = Integer.parseInt(ContextHost.RemoveQuotes(prop.getValue().toString()));
                        }
                    }
                    modcluster.stop(waittime);
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep();
    }
}

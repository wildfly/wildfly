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

package org.jboss.as.naming.management;

import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author John Bailey
 */
public class JndiViewOperation implements OperationStepHandler {
    public static final JndiViewOperation INSTANCE = new JndiViewOperation();
    public static final String OPERATION_NAME = "jndi-view";

    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final ModelNode resultNode = context.getResult();

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);

                    final ModelNode contextsNode = resultNode.get("java: contexts");

                    final ServiceController<?> javaContextService = serviceRegistry.getService(ContextNames.JAVA_CONTEXT_SERVICE_NAME);
                    final NamingStore javaContextNamingStore = NamingStore.class.cast(javaContextService.getValue());
                    try {
                        addEntries(contextsNode.get("java:"), new NamingContext(javaContextNamingStore, null));
                    } catch (NamingException e) {
                        throw new OperationFailedException(e, new ModelNode().set("Failed to read java: context entries."));
                    }

                    final ServiceController<?> jbossContextService = serviceRegistry.getService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME);
                    final NamingStore jbossContextNamingStore = NamingStore.class.cast(jbossContextService.getValue());
                    try {
                        addEntries(contextsNode.get("java:jboss"), new NamingContext(jbossContextNamingStore, null));
                    } catch (NamingException e) {
                        throw new OperationFailedException(e, new ModelNode().set("Failed to read java:jboss context entries."));
                    }

                    final ServiceController<?> globalContextService = serviceRegistry.getService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME);
                    final NamingStore globalContextNamingStore = NamingStore.class.cast(globalContextService.getValue());
                    try {
                        addEntries(contextsNode.get("java:global"), new NamingContext(globalContextNamingStore, null));
                    } catch (NamingException e) {
                        throw new OperationFailedException(e, new ModelNode().set("Failed to read java:global context entries."));
                    }

                    final ServiceController<?> extensionRegistryController = serviceRegistry.getService(JndiViewExtensionRegistry.SERVICE_NAME);
                    if(extensionRegistryController != null) {
                        final JndiViewExtensionRegistry extensionRegistry = JndiViewExtensionRegistry.class.cast(extensionRegistryController.getValue());

                        for (JndiViewExtension extension : extensionRegistry.getExtensions()) {
                            extension.execute(new JndiViewExtensionContext() {
                                public OperationContext getOperationContext() {
                                    return context;
                                }

                                public ModelNode getResult() {
                                    return resultNode;
                                }

                                public void addEntries(ModelNode current, Context context) throws NamingException{
                                    JndiViewOperation.this.addEntries(current, context);
                                }
                            });
                        }
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        } else {
            throw new OperationFailedException(new ModelNode().set("Jndi view is only available in runtime mode."));
        }
        context.completeStep();
    }

    private void addEntries(final ModelNode current, final Context context) throws NamingException {
        final NamingEnumeration<NameClassPair> entries = context.list("");
        while (entries.hasMore()) {
            final NameClassPair pair = entries.next();

            final ModelNode node = current.get(pair.getName());
            node.get("class-name").set(pair.getClassName());
            try {
                final Object value = context.lookup(pair.getName());
                if (value instanceof Context) {
                    addEntries(node.get("children"), Context.class.cast(value));
                } else if (value instanceof Reference) {
                    //node.get("value").set(value.toString());
                } else {
                    node.get("value").set(value.toString());
                }
            } catch (NamingException e) {
                // Ignore for now..
            }
        }
    }
}

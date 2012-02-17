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
package org.jboss.as.osgi.parser;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.osgi.OSGiMessages;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author David Bosschaert
 */
public class BundleRuntimeHandler extends AbstractRuntimeOnlyHandler {

    static final BundleRuntimeHandler INSTANCE = new BundleRuntimeHandler();

    static final String [] ATTRIBUTES = { ModelConstants.ID, ModelConstants.STARTLEVEL,
        ModelConstants.STATE, ModelConstants.SYMBOLIC_NAME, ModelConstants.TYPE, ModelConstants.VERSION };

    static final String START_OPERATION = "start";
    static final String STOP_OPERATION = "stop";
    static final String [] OPERATIONS = { START_OPERATION, STOP_OPERATION };

    private BundleRuntimeHandler() {
    }

    void register(ManagementResourceRegistration registry) {
        for (String attr : ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, this, AttributeAccess.Storage.RUNTIME);
        }

        for (final String op : OPERATIONS) {
            registry.registerOperationHandler(op, this, new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ResourceBundle resourceBundle = OSGiSubsystemProviders.getResourceBundle(locale);
                    return CommonDescriptions.getDescriptionOnlyOperation(resourceBundle, op, ModelConstants.BUNDLE);
                }
            }, EnumSet.of(OperationEntry.Flag.RESTART_NONE));
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(operationName)) {
            handleReadAttribute(context, operation);
        } else if (Arrays.asList(OPERATIONS).contains(operationName)) {
            handleOperation(operationName, context, operation);
        }
    }

    private void handleReadAttribute(OperationContext context, ModelNode operation) {
        String name = operation.require(ModelDescriptionConstants.NAME).asString();
        Long id = Long.parseLong(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue());
        BundleContext bc = getBundleContext(context);
        Bundle bundle = bc.getBundle(id);

        if (ModelConstants.ID.equals(name)) {
            context.getResult().set(id);
        } else if (ModelConstants.STARTLEVEL.equals(name)) {
            Integer startLevel = getStartLevel(bc, bundle);
            if (startLevel != null) {
                context.getResult().set(startLevel);
            } else {
                context.getFailureDescription().set(OSGiMessages.MESSAGES.serviceNotAvailable());
            }
        } else if (ModelConstants.STATE.equals(name)) {
            context.getResult().set(getBundleState(bundle));
        } else if (ModelConstants.SYMBOLIC_NAME.equals(name)) {
            context.getResult().set(bundle.getSymbolicName());
        } else if (ModelConstants.TYPE.equals(name)) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                context.getResult().set(ModelConstants.FRAGMENT);
            } else {
                context.getResult().set(ModelConstants.BUNDLE);
            }
        } else if (ModelConstants.VERSION.equals(name)) {
            context.getResult().set(bundle.getVersion().toString());
        }

        context.completeStep();
    }

    private void handleOperation(String operationName, OperationContext context, ModelNode operation) {
        Long id = Long.parseLong(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue());
        BundleContext bc = getBundleContext(context);
        Bundle bundle = bc.getBundle(id);

        try {
            if (START_OPERATION.equals(operationName)) {
                bundle.start();
            } else if (STOP_OPERATION.equals(operationName)) {
                bundle.stop();
            }
        } catch (BundleException e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }

        context.completeStep();
    }

    static String getBundleState(Bundle bundle) {
        switch (bundle.getState()) {
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING";
        case Bundle.ACTIVE:
            return "ACTIVE";
        }
        return null;
    }

    private BundleContext getBundleContext(OperationContext context) {
        ServiceController<?> sbs = context.getServiceRegistry(false).getService(Services.SYSTEM_BUNDLE);
        if (sbs == null) {
            return null;
        }

        Bundle systemBundle = Bundle.class.cast(sbs.getValue());
        return systemBundle.getBundleContext();
    }

    private Integer getStartLevel(BundleContext bc, Bundle b) {
        ServiceReference sref = bc.getServiceReference(StartLevel.class.getName());
        if (sref == null)
            return null;

        try {
            Object sls = bc.getService(sref);
            if (sls instanceof StartLevel == false)
                return null;

            return ((StartLevel) sls).getBundleStartLevel(b);
        } finally {
            bc.ungetService(sref);
        }
    }
}

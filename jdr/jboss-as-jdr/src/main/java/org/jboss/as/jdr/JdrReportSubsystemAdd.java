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

package org.jboss.as.jdr;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import java.util.Locale;

/**
 * Adds the JDR subsystem.
 *
 * @author Brian Stansberry
 */
public class JdrReportSubsystemAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    public static final JdrReportSubsystemAdd INSTANCE = new JdrReportSubsystemAdd();

    private final ParametersValidator validator = new ParametersValidator();

    private JdrReportSubsystemAdd() {
        // Example of registering validators
        // validator.registerValidator("some-string", new StringLengthValidator(1, Integer.MAX_VALUE, false, false));
        // validator.registerValidator("some-int", new IntRangeValidator(0, 10, true, false));
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        validator.validate(operation);
        model.setEmptyObject();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        // TODO if there is any configuration data, use it to configure runtime services
        newControllers.add(JdrReportService.addService(context.getServiceTarget(), verificationHandler));

        // TODO add any other runtime services
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return JdrReportDescriptions.getSubsystemAdd(locale);
    }
}

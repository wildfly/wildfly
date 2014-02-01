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

package com.redhat.gss.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.dmr.ModelNode;
import com.redhat.gss.extension.requesthandler.AddCommentRequestHandler;
import com.redhat.gss.extension.requesthandler.DiagnoseFileRequestHandler;
import com.redhat.gss.extension.requesthandler.DiagnoseStringRequestHandler;
import com.redhat.gss.extension.requesthandler.GetCaseRequestHandler;
import com.redhat.gss.extension.requesthandler.GetCommentsRequestHandler;
import com.redhat.gss.extension.requesthandler.GetSolutionRequestHandler;
import com.redhat.gss.extension.requesthandler.GetVersionsRequestHandler;
import com.redhat.gss.extension.requesthandler.ListCasesRequestHandler;
import com.redhat.gss.extension.requesthandler.ListProductsRequestHandler;
import com.redhat.gss.extension.requesthandler.ListSeveritiesRequestHandler;
import com.redhat.gss.extension.requesthandler.ModifyCaseRequestHandler;
import com.redhat.gss.extension.requesthandler.OpenCaseRequestHandler;
import com.redhat.gss.extension.requesthandler.SearchSolutionsRequestHandler;
import com.redhat.gss.extension.requesthandler.SymptomsFileRequestHandler;

public class RedhatAccessPluginEapExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "redhat-access-plugin-eap";
    public static final String RESOURCE_NAME = RedhatAccessPluginEapExtension.class
            .getPackage().getName() + ".LocalDescriptions";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(
            SUBSYSTEM, SUBSYSTEM_NAME);

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(
            final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(),
                RESOURCE_NAME,
                RedhatAccessPluginEapExtension.class.getClassLoader(), true,
                false);
    }

    public void initialize(ExtensionContext context) {
        // TODO:FIX ALL THESE
        SubsystemRegistration subsystemRegistration = context
                .registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        ManagementResourceRegistration root = subsystemRegistration
                .registerSubsystemModel(RedhatAccessPluginEapSubsystemDefinition.INSTANCE);
        root.registerOperationHandler(
                GenericSubsystemDescribeHandler.DEFINITION,
                GenericSubsystemDescribeHandler.INSTANCE);

        if (context.isRuntimeOnlyRegistrationValid()) {
            root.registerOperationHandler(
                    SearchSolutionsRequestHandler.DEFINITION,
                    SearchSolutionsRequestHandler.INSTANCE);
            root.registerOperationHandler(GetSolutionRequestHandler.DEFINITION,
                    GetSolutionRequestHandler.INSTANCE);
            root.registerOperationHandler(
                    DiagnoseStringRequestHandler.DEFINITION,
                    DiagnoseStringRequestHandler.INSTANCE);
            root.registerOperationHandler(
                    DiagnoseFileRequestHandler.DEFINITION,
                    DiagnoseFileRequestHandler.INSTANCE);
            root.registerOperationHandler(
                    SymptomsFileRequestHandler.DEFINITION,
                    SymptomsFileRequestHandler.INSTANCE);
            root.registerOperationHandler(OpenCaseRequestHandler.DEFINITION,
                    OpenCaseRequestHandler.INSTANCE);
            root.registerOperationHandler(ModifyCaseRequestHandler.DEFINITION,
                    ModifyCaseRequestHandler.INSTANCE);
            root.registerOperationHandler(
                    ListProductsRequestHandler.DEFINITION,
                    ListProductsRequestHandler.INSTANCE);
            root.registerOperationHandler(GetVersionsRequestHandler.DEFINITION,
                    GetVersionsRequestHandler.INSTANCE);
            root.registerOperationHandler(
                    ListSeveritiesRequestHandler.DEFINITION,
                    ListSeveritiesRequestHandler.INSTANCE);
            root.registerOperationHandler(ListCasesRequestHandler.DEFINITION,
                    ListCasesRequestHandler.INSTANCE);
            root.registerOperationHandler(GetCaseRequestHandler.DEFINITION,
                    GetCaseRequestHandler.INSTANCE);
            root.registerOperationHandler(GetCommentsRequestHandler.DEFINITION,
                    GetCommentsRequestHandler.INSTANCE);
            root.registerOperationHandler(AddCommentRequestHandler.DEFINITION,
                    AddCommentRequestHandler.INSTANCE);
        }
        subsystemRegistration
                .registerXMLElementWriter(RedhatAccessPluginEapSubsystemParser.INSTANCE);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,
                Namespace.CURRENT.getUriString(),
                RedhatAccessPluginEapSubsystemParser.INSTANCE);
    }

    private static ModelNode pathAddress(PathElement... elements) {
        return PathAddress.pathAddress(elements).toModelNode();
    }
}

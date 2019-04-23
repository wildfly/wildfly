/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.jaxrs;

import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.handlers.ServletHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.wildfly.extension.jaxrs.Constants;
import org.wildfly.extension.jaxrs.ResetStatisticsOperation;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

import javax.servlet.Servlet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class JaxrsSubsystemDefinition extends PersistentResourceDefinition {
    public static final ModuleIdentifier RESTEASY_ATOM = ModuleIdentifier.create("org.jboss.resteasy.resteasy-atom-provider");
    public static final ModuleIdentifier RESTEASY_CDI = ModuleIdentifier.create("org.jboss.resteasy.resteasy-cdi");
    public static final ModuleIdentifier RESTEASY_CRYPTO = ModuleIdentifier.create("org.jboss.resteasy.resteasy-crypto");
    public static final ModuleIdentifier RESTEASY_VALIDATOR_11 = ModuleIdentifier.create("org.jboss.resteasy.resteasy-validator-provider-11");
    public static final ModuleIdentifier RESTEASY_VALIDATOR = ModuleIdentifier.create("org.jboss.resteasy.resteasy-validator-provider");
    public static final ModuleIdentifier RESTEASY_JAXRS = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxrs");
    public static final ModuleIdentifier RESTEASY_JAXB = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jaxb-provider");
    public static final ModuleIdentifier RESTEASY_JACKSON2 = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jackson2-provider");
    public static final ModuleIdentifier RESTEASY_JSON_P_PROVIDER = ModuleIdentifier.create("org.jboss.resteasy.resteasy-json-p-provider");
    public static final ModuleIdentifier RESTEASY_JSON_B_PROVIDER = ModuleIdentifier.create("org.jboss.resteasy.resteasy-json-binding-provider");
    public static final ModuleIdentifier RESTEASY_JSAPI = ModuleIdentifier.create("org.jboss.resteasy.resteasy-jsapi");
    public static final ModuleIdentifier RESTEASY_MULTIPART = ModuleIdentifier.create("org.jboss.resteasy.resteasy-multipart-provider");
    public static final ModuleIdentifier RESTEASY_YAML = ModuleIdentifier.create("org.jboss.resteasy.resteasy-yaml-provider");

    public static final ModuleIdentifier JACKSON_DATATYPE_JDK8 = ModuleIdentifier.create("com.fasterxml.jackson.datatype.jackson-datatype-jdk8");
    public static final ModuleIdentifier JACKSON_DATATYPE_JSR310 = ModuleIdentifier.create("com.fasterxml.jackson.datatype.jackson-datatype-jsr310");

    public static final ModuleIdentifier JAXB_API = ModuleIdentifier.create("javax.xml.bind.api");
    public static final ModuleIdentifier JSON_API = ModuleIdentifier.create("javax.json.api");
    public static final ModuleIdentifier JAXRS_API = ModuleIdentifier.create("javax.ws.rs.api");

    /**
     * We include this so that jackson annotations will be available, otherwise they will be ignored which leads
     * to confusing behaviour.
     *
     */
    public static final ModuleIdentifier JACKSON_CORE_ASL = ModuleIdentifier.create("org.codehaus.jackson.jackson-core-asl");

    public static final SimpleAttributeDefinition STATISTICS_ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.STATISTICS_ENABLED,
                    ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {STATISTICS_ENABLED};

    public static final JaxrsSubsystemDefinition INSTANCE = new JaxrsSubsystemDefinition();

    private JaxrsSubsystemDefinition() {
         super(new Parameters(JaxrsExtension.SUBSYSTEM_PATH, JaxrsExtension.getResolver())
                 .setAddHandler(JaxrsSubsystemAdd.INSTANCE)
                 .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }


    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ResetStatisticsOperation.DEFINITION,
                ResetStatisticsOperation.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        resourceRegistration.registerReadWriteAttribute(STATISTICS_ENABLED,
                null,  new AbstractWriteAttributeHandler<Void>(STATISTICS_ENABLED) {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {

                        setValue(context, resolvedValue.asBoolean());
                        return false;
                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {

                        setValue(context, valueToRestore.asBoolean());
                    }

                    private void setValue(OperationContext context, boolean value) {

                        ServiceController<?> controller = context.getServiceRegistry(false)
                                .getService(UndertowService.UNDERTOW);

                        if (controller != null) {
                            UndertowService service = (UndertowService) controller.getService();

                            if (service != null) {

                                for (Server server : service.getServers()) {
                                    ServletContainer servletContainer = server.getServletContainer()
                                            .getValue().getValue().getServletContainer();

                                    for (String name : servletContainer.listDeployments()) {
                                        for (Map.Entry<String, ServletHandler> entry : servletContainer
                                                .getDeployment(name).getDeployment().getServlets().getServletHandlers().entrySet()) {

                                            ManagedServlet managedServlet = entry.getValue().getManagedServlet();

                                            if (HttpServletDispatcher.class.isAssignableFrom(
                                                    managedServlet.getServletInfo().getServletClass())) {

                                                try {
                                                    Servlet resteasyServlet = managedServlet.getServlet().getInstance();
                                                    ((HttpServletDispatcher) resteasyServlet).getDispatcher()
                                                            .getProviderFactory().getStatisticsController().setEnabled(value);
                                                } catch (Exception e) {
                                                    // no-op
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
    }



    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.passive(RESTEASY_CDI.getName()),
                    RuntimePackageDependency.passive(RESTEASY_VALIDATOR_11.getName()),
                    RuntimePackageDependency.passive(RESTEASY_VALIDATOR.getName()),
                    RuntimePackageDependency.required(JAXRS_API.getName()),
                    RuntimePackageDependency.required(JAXB_API.getName()),
                    RuntimePackageDependency.required(JSON_API.getName()),
                    RuntimePackageDependency.optional(RESTEASY_ATOM.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JAXRS.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JAXB.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JACKSON2.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JSON_P_PROVIDER.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JSON_B_PROVIDER.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JSAPI.getName()),
                    RuntimePackageDependency.optional(RESTEASY_MULTIPART.getName()),
                    RuntimePackageDependency.optional(RESTEASY_YAML.getName()),
                    RuntimePackageDependency.optional(JACKSON_CORE_ASL.getName()),
                    RuntimePackageDependency.optional(RESTEASY_CRYPTO.getName()),
                    RuntimePackageDependency.optional(JACKSON_DATATYPE_JDK8.getName()),
                    RuntimePackageDependency.optional(JACKSON_DATATYPE_JSR310.getName()),
                    // The following ones are optional dependencies located in org.jboss.as.jaxrs module.xml
                    // To be provisioned, they need to be explicitly added as optional packages.
                    RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-jettison-provider"),
                    RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-jackson-provider"),
                    RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-spring"));
    }
}

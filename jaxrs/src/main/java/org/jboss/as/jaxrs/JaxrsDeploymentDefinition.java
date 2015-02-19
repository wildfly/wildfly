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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.undertow.DeploymentDefinition.CONTEXT_ROOT;
import static org.wildfly.extension.undertow.DeploymentDefinition.SERVER;
import static org.wildfly.extension.undertow.DeploymentDefinition.VIRTUAL_HOST;

import io.undertow.servlet.api.ThreadSetupAction.Handle;
import io.undertow.servlet.handlers.ServletHandler;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentService;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class JaxrsDeploymentDefinition extends SimpleResourceDefinition {

    public static final JaxrsDeploymentDefinition DEPLOYMENT_INSTANCE = new JaxrsDeploymentDefinition(true);
    public static final JaxrsDeploymentDefinition SUBSYSTEM_INSTANCE = new JaxrsDeploymentDefinition(false);

    public static final String SHOW_RESOURCES = "show-resources";
    public static final AttributeDefinition CLASSNAME
            = new SimpleAttributeDefinitionBuilder("resource-class", ModelType.STRING, true).setStorageRuntime().build();
    public static final AttributeDefinition PATH
            = new SimpleAttributeDefinitionBuilder("resource-path", ModelType.STRING, true).setStorageRuntime().build();
    public static final AttributeDefinition METHOD
            = new SimpleAttributeDefinitionBuilder("jaxrs-resource-method", ModelType.STRING, false).setStorageRuntime().build();
    public static final AttributeDefinition METHODS
            = new SimpleListAttributeDefinition.Builder("resource-methods", METHOD).setStorageRuntime().build();
    public static final ObjectTypeAttributeDefinition JAXRS_RESOURCE
            = new ObjectTypeAttributeDefinition.Builder("jaxrs-resource", CLASSNAME, PATH, METHODS).setStorageRuntime().build();

    private boolean showResources;
    private JaxrsDeploymentDefinition(boolean showResources) {
         super(JaxrsExtension.SUBSYSTEM_PATH, JaxrsExtension.getResolver(), JaxrsSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
         this.showResources = showResources;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if(showResources) {
            resourceRegistration.registerOperationHandler(ShowJaxrsResourcesHandler.DEFINITION, new ShowJaxrsResourcesHandler());
        }
    }

    static class ShowJaxrsResourcesHandler implements OperationStepHandler {
        public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(SHOW_RESOURCES,
                JaxrsExtension.getResolver("deployment"))
                .setReadOnly()
                .setRuntimeOnly()
                .setReplyType(ModelType.LIST)
                .setReplyParameters(JAXRS_RESOURCE).build();


        void handle(ModelNode response, String contextRootPath, Collection<String> servletMappings, String mapping, List<ResourceInvoker> resources) {
            for (ResourceInvoker resourceInvoker : resources) {
                ResourceMethodInvoker resource = (ResourceMethodInvoker) resourceInvoker;
                final ModelNode node = new ModelNode();
                node.get(CLASSNAME.getName()).set(resource.getResourceClass().getCanonicalName());
                node.get(PATH.getName()).set(mapping);
                for (String servletMapping : servletMappings) {
                    String method = formatMethod(resource, servletMapping, mapping, contextRootPath);
                    for (final String httpMethod : resource.getHttpMethods()) {
                        node.get(METHODS.getName()).add(String.format(method, httpMethod));
                    }
                }
                response.add(node);
            }
        }

        private String formatMethod(ResourceMethodInvoker resource, String servletMapping, String path, String contextRootPath) {
            StringBuilder builder = new StringBuilder();
            builder.append("%1$s ");
            builder.append(contextRootPath).append('/').append(servletMapping.replaceAll("\\*", "")).append(path);
            builder.append(" - ").append(resource.getResourceClass().getCanonicalName()).append('.').append(resource.getMethod().getName()).append('(');
            if (resource.getMethod().getParameterTypes().length > 0) {
                builder.append("...");
            }
            builder.append(')');
            return builder.toString().replaceAll("//", "/");
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
            //Getting Undertow deployment Model to access Servlet informations.
            final ModelNode subModel = context.readResourceFromRoot(address.subAddress(0, address.size() - 1).append(
                    SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), false).getModel();
            final String host = VIRTUAL_HOST.resolveModelAttribute(context, subModel).asString();
            final String contextPath = CONTEXT_ROOT.resolveModelAttribute(context, subModel).asString();
            final String server = SERVER.resolveModelAttribute(context, subModel).asString();

            final ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(server, host, contextPath));
            final UndertowDeploymentService deploymentService = (UndertowDeploymentService) controller.getService();
            Servlet resteasyServlet = null;
            Handle handle = deploymentService.getDeployment().getThreadSetupAction().setup(null);
            try {
                for (Map.Entry<String, ServletHandler> servletHandler : deploymentService.getDeployment().getServlets().getServletHandlers().entrySet()) {
                    if (HttpServletDispatcher.class.isAssignableFrom(servletHandler.getValue().getManagedServlet().getServletInfo().getServletClass())) {
                        resteasyServlet = (Servlet) servletHandler.getValue().getManagedServlet().getServlet().getInstance();
                        break;
                    }
                }
                if (resteasyServlet != null) {
                    final Collection<String> servletMappings = resteasyServlet.getServletConfig().getServletContext().getServletRegistration(resteasyServlet.getServletConfig().getServletName()).getMappings();
                    final ResourceMethodRegistry registry = (ResourceMethodRegistry) ((HttpServletDispatcher) resteasyServlet).getDispatcher().getRegistry();
                    context.addStep(new OperationStepHandler() {
                        @Override
                        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                            if (registry != null) {
                                final ModelNode response = new ModelNode();
                                for (Map.Entry<String, List<ResourceInvoker>> resource : registry.getBounded().entrySet()) {
                                    handle(response, contextPath, servletMappings, resource.getKey(), resource.getValue());
                                }
                                context.getResult().set(response);
                            }
                        }
                    }, OperationContext.Stage.RUNTIME);
                }
            } catch (ServletException ex) {
                throw new RuntimeException(ex);
            } finally {
                handle.tearDown();
            }
        }
    }
}

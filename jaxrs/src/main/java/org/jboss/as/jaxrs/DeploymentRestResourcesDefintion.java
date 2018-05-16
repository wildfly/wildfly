/*
 * Copyright (C) 2016 Red Hat, inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.undertow.DeploymentDefinition.CONTEXT_ROOT;
import static org.wildfly.extension.undertow.DeploymentDefinition.SERVER;
import static org.wildfly.extension.undertow.DeploymentDefinition.VIRTUAL_HOST;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.handlers.ServletHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.Level;
import org.jboss.msc.service.ServiceController;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceLocatorInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceLocator;
import org.jboss.resteasy.spi.metadata.ResourceMethod;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentService;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@SuppressWarnings("deprecation")
public class DeploymentRestResourcesDefintion extends SimpleResourceDefinition {

    public static DeploymentRestResourcesDefintion INSTANCE = new DeploymentRestResourcesDefintion();

    public static final String REST_RESOURCE_NAME = "rest-resource";

    public static final AttributeDefinition RESOURCE_CLASS = new SimpleAttributeDefinitionBuilder("resource-class",
            ModelType.STRING, true).setStorageRuntime().build();

    public static final AttributeDefinition RESOURCE_PATH = new SimpleAttributeDefinitionBuilder("resource-path", ModelType.STRING, true)
            .setStorageRuntime().build();

    public static final AttributeDefinition RESOURCE_METHOD = new SimpleAttributeDefinitionBuilder("resource-method",
            ModelType.STRING, false).setStorageRuntime().build();

    public static final AttributeDefinition RESOURCE_METHODS = new SimpleListAttributeDefinition.Builder("resource-methods", RESOURCE_METHOD)
            .setStorageRuntime().build();

    public static final AttributeDefinition CONSUME = new SimpleAttributeDefinitionBuilder("consume", ModelType.STRING, true)
            .setStorageRuntime().build();

    public static final AttributeDefinition CONSUMES = new SimpleListAttributeDefinition.Builder("consumes", CONSUME)
            .setStorageRuntime().build();

    public static final AttributeDefinition PRODUCE = new SimpleAttributeDefinitionBuilder("produce", ModelType.STRING, true)
            .setStorageRuntime().build();

    public static final AttributeDefinition PRODUCES = new SimpleListAttributeDefinition.Builder("produces", PRODUCE)
            .setStorageRuntime().build();

    public static final AttributeDefinition JAVA_METHOD = new SimpleAttributeDefinitionBuilder("java-method", ModelType.STRING,
            true).setStorageRuntime().build();

    public static final ObjectTypeAttributeDefinition RESOURCE_PATH_GRP = new ObjectTypeAttributeDefinition.Builder(
            "rest-resource-path-group", RESOURCE_PATH, CONSUMES, PRODUCES, JAVA_METHOD, RESOURCE_METHODS).build();

    public static final ObjectListAttributeDefinition RESOURCE_PATHS = new ObjectListAttributeDefinition.Builder(
            "rest-resource-paths", RESOURCE_PATH_GRP).build();

    public static final ObjectTypeAttributeDefinition SUB_RESOURCE_LOCATOR = new ObjectTypeAttributeDefinition.Builder(
            "sub-resource-locator-group", RESOURCE_CLASS, RESOURCE_PATH, CONSUMES, PRODUCES, JAVA_METHOD, RESOURCE_METHODS).build();

    public static final ObjectListAttributeDefinition SUB_RESOURCE_LOCATORS = new ObjectListAttributeDefinition.Builder(
            "sub-resource-locators", SUB_RESOURCE_LOCATOR).build();

    private DeploymentRestResourcesDefintion() {
        super(PathElement.pathElement(REST_RESOURCE_NAME), JaxrsExtension.getResolver("deployment"));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerMetric(RESOURCE_CLASS, new AbstractRestResReadHandler() {
            @Override
            void handleAttribute(String className, List<JaxrsResourceMethodDescription> methodInvokers,
                    List<JaxrsResourceLocatorDescription> locatorIncokers, Collection<String> servletMappings,
                    ModelNode response) {
                response.set(className);
            }
        });
        resourceRegistration.registerMetric(RESOURCE_PATHS, new AbstractRestResReadHandler() {
            @Override
            void handleAttribute(String className, List<JaxrsResourceMethodDescription> methodInvokers,
                    List<JaxrsResourceLocatorDescription> locatorIncokers, Collection<String> servletMappings,
                    ModelNode response) {
                for (JaxrsResourceMethodDescription methodDesc: methodInvokers) {
                    response.add(methodDesc.toModelNode());
                }
            }
        });

        resourceRegistration.registerMetric(SUB_RESOURCE_LOCATORS, new AbstractRestResReadHandler() {
            @Override
            void handleAttribute(String className, List<JaxrsResourceMethodDescription> methodInvokers,
                    List<JaxrsResourceLocatorDescription> locatorIncokers, Collection<String> servletMappings,
                    ModelNode response) {
                for (JaxrsResourceLocatorDescription methodDesc: locatorIncokers) {
                    response.add(methodDesc.toModelNode());
                }
            }
        });
    }

    abstract class AbstractRestResReadHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress();
            String clsName = address.getLastElement().getValue();
            PathAddress parentAddress = address.getParent();
            final ModelNode subModel = context.readResourceFromRoot(
                    parentAddress.subAddress(0, parentAddress.size() - 1).append(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME),
                    false).getModel();
            final String host = VIRTUAL_HOST.resolveModelAttribute(context, subModel).asString();
            final String contextPath = CONTEXT_ROOT.resolveModelAttribute(context, subModel).asString();
            final String server = SERVER.resolveModelAttribute(context, subModel).asString();

            final ServiceController<?> controller = context.getServiceRegistry(false).getService(
                    UndertowService.deploymentServiceName(server, host, contextPath));
            final UndertowDeploymentService deploymentService = (UndertowDeploymentService) controller.getService();
            try {

                deploymentService.getDeployment().createThreadSetupAction(new ThreadSetupHandler.Action<Object, Object>() {
                    @Override
                    public Object call(HttpServerExchange exchange, Object ctxObject) throws Exception {
                        Servlet resteasyServlet = null;
                        for (Map.Entry<String, ServletHandler> servletHandler : deploymentService.getDeployment().getServlets()
                                .getServletHandlers().entrySet()) {
                            if (HttpServletDispatcher.class.isAssignableFrom(servletHandler.getValue().getManagedServlet()
                                    .getServletInfo().getServletClass())) {
                                resteasyServlet = servletHandler.getValue().getManagedServlet().getServlet().getInstance();
                                break;
                            }
                        }
                        if (resteasyServlet != null) {
                            final Collection<String> servletMappings = resteasyServlet.getServletConfig().getServletContext()
                                    .getServletRegistration(resteasyServlet.getServletConfig().getServletName()).getMappings();
                            final ResourceMethodRegistry registry = (ResourceMethodRegistry) ((HttpServletDispatcher) resteasyServlet)
                                    .getDispatcher().getRegistry();
                            context.addStep(new OperationStepHandler() {
                                @Override
                                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                    final ModelNode response = new ModelNode();
                                    List<JaxrsResourceMethodDescription> resMethodInvokers = new ArrayList<>();
                                    List<JaxrsResourceLocatorDescription> resLocatorInvokers = new ArrayList<>();
                                    for (Map.Entry<String, List<ResourceInvoker>> resource : registry.getBounded().entrySet()) {
                                        String mapping = resource.getKey();
                                        List<ResourceInvoker> resouceInvokers = resource.getValue();
                                        for (ResourceInvoker resourceInvoker : resouceInvokers) {
                                            if (ResourceMethodInvoker.class.isAssignableFrom(resourceInvoker.getClass())) {
                                                ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) resourceInvoker;
                                                if (methodInvoker.getResourceClass().getCanonicalName().equals(clsName)) {
                                                    JaxrsResourceMethodDescription resMethodDesc = resMethodDescription(methodInvoker, contextPath, mapping, servletMappings);
                                                    resMethodInvokers.add(resMethodDesc);
                                                }
                                            } else if (ResourceLocatorInvoker.class.isAssignableFrom(resourceInvoker.getClass())) {
                                                ResourceLocatorInvoker locatorInvoker = (ResourceLocatorInvoker) resourceInvoker;
                                                if (clsName.equals(locatorInvoker.getMethod().getDeclaringClass().getCanonicalName())) {
                                                    ResourceClass resClass = ResourceBuilder.locatorFromAnnotations(locatorInvoker.getMethod().getReturnType());
                                                    JaxrsResourceLocatorDescription resLocatorDesc = resLocatorDescription(resClass, contextPath, mapping, servletMappings, new ArrayList<Class<?>>());
                                                    resLocatorInvokers.add(resLocatorDesc);
                                                }
                                            }
                                        }
                                    }
                                    Collections.sort(resMethodInvokers);
                                    Collections.sort(resLocatorInvokers);
                                    handleAttribute(clsName, resMethodInvokers, resLocatorInvokers, servletMappings, response);
                                    context.getResult().set(response);
                                }
                            }, OperationContext.Stage.RUNTIME);
                        }

                        return null;
                    }
                }).call(null, null);

            } catch (Exception ex) {
                //WFLY-10222 we don't want a failure to read the attribute to break everything
                JaxrsLogger.JAXRS_LOGGER.failedToReadAttribute(ex, address, operation.get(NAME));
                context.addResponseWarning(Level.WARN, ex.getMessage());
            }
        }

        abstract void handleAttribute(String className, List<JaxrsResourceMethodDescription> methodInvokers,
                List<JaxrsResourceLocatorDescription> locatorIncokers, Collection<String> servletMappings, ModelNode response);
    }

    private JaxrsResourceLocatorDescription resLocatorDescription(ResourceClass resClass, String contextPath, String mapping,
            Collection<String> servletMappings, List<Class<?>> resolvedCls) {
        JaxrsResourceLocatorDescription locatorRes = new JaxrsResourceLocatorDescription();
        locatorRes.resourceClass = resClass.getClazz();
        resolvedCls.add(resClass.getClazz());
        for (ResourceMethod resMethod : resClass.getResourceMethods()) {
            JaxrsResourceMethodDescription jaxrsRes = new JaxrsResourceMethodDescription();
            jaxrsRes.consumeTypes = resMethod.getConsumes();
            jaxrsRes.contextPath = contextPath;
            jaxrsRes.httpMethods = resMethod.getHttpMethods();
            jaxrsRes.method = resMethod.getMethod();
            jaxrsRes.produceTypes = resMethod.getProduces();
            jaxrsRes.resourceClass = resClass.getClazz();
            String resPath = new StringBuilder(mapping).append("/").append(resMethod.getFullpath()).toString().replace("//", "/");
            jaxrsRes.resourcePath = resPath;
            jaxrsRes.servletMappings = servletMappings;
            addMethodParameters(jaxrsRes, resMethod.getMethod());
            locatorRes.methodsDescriptions.add(jaxrsRes);
        }
        for (ResourceLocator resLocator: resClass.getResourceLocators()) {
            Class<?> clz = resLocator.getReturnType();
            if (clz.equals(resClass.getClazz())) {
                break;
            }
            if (resolvedCls.contains(clz)) {
                break;
            } else {
                resolvedCls.add(clz);
            }
            ResourceClass subResClass = ResourceBuilder.locatorFromAnnotations(clz);
            String subMapping = new StringBuilder(mapping).append("/").append(resLocator.getFullpath()).toString().replace("//", "/");
            JaxrsResourceLocatorDescription inner = resLocatorDescription(subResClass, contextPath, subMapping, servletMappings, resolvedCls);
            if (inner.containsMethodResources()) {
                locatorRes.subLocatorDescriptions.add(inner);
            }
        }
        return locatorRes;
    }

    private JaxrsResourceMethodDescription resMethodDescription(ResourceMethodInvoker methodInvoker, String contextPath,
            String mapping, Collection<String> servletMappings) {
        JaxrsResourceMethodDescription jaxrsRes = new JaxrsResourceMethodDescription();
        jaxrsRes.consumeTypes = methodInvoker.getConsumes();
        jaxrsRes.contextPath = contextPath;
        jaxrsRes.httpMethods = methodInvoker.getHttpMethods();
        jaxrsRes.method = methodInvoker.getMethod();
        jaxrsRes.produceTypes = methodInvoker.getProduces();
        jaxrsRes.resourceClass = methodInvoker.getResourceClass();
        jaxrsRes.resourcePath = mapping;
        jaxrsRes.servletMappings = servletMappings;
        addMethodParameters(jaxrsRes, methodInvoker.getMethod());
        return jaxrsRes;
    }

    private void addMethodParameters(JaxrsResourceMethodDescription jaxrsRes, Method method) {
        for (Parameter param : method.getParameters()) {
            ParamInfo paramInfo = new ParamInfo();
            paramInfo.cls = param.getType();
            paramInfo.defaultValue = null;
            paramInfo.name = null;
            paramInfo.type = null;
            Annotation annotation;
            if ((annotation = param.getAnnotation(PathParam.class)) != null) {
                PathParam pathParam = (PathParam) annotation;
                paramInfo.name = pathParam.value();
                paramInfo.type = "@" + PathParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(QueryParam.class)) != null) {
                QueryParam queryParam = (QueryParam) annotation;
                paramInfo.name = queryParam.value();
                paramInfo.type = "@" + QueryParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(HeaderParam.class)) != null) {
                HeaderParam headerParam = (HeaderParam) annotation;
                paramInfo.name = headerParam.value();
                paramInfo.type = "@" + HeaderParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(CookieParam.class)) != null) {
                CookieParam cookieParam = (CookieParam) annotation;
                paramInfo.name = cookieParam.value();
                paramInfo.type = "@" + CookieParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(MatrixParam.class)) != null) {
                MatrixParam matrixParam = (MatrixParam) annotation;
                paramInfo.name = matrixParam.value();
                paramInfo.type = "@" + MatrixParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(FormParam.class)) != null) {
                FormParam formParam = (FormParam) annotation;
                paramInfo.name = formParam.value();
                paramInfo.type = "@" + FormParam.class.getSimpleName();
            }
            if (paramInfo.name == null) {
                paramInfo.name = param.getName();
            }
            if ((annotation = param.getAnnotation(DefaultValue.class)) != null) {
                DefaultValue defaultValue = (DefaultValue) annotation;
                paramInfo.defaultValue = defaultValue.value();
            }
            jaxrsRes.parameters.add(paramInfo);
        }
    }

    private static class JaxrsResourceLocatorDescription implements Comparable<JaxrsResourceLocatorDescription> {

        private Class<?> resourceClass;
        private List<JaxrsResourceMethodDescription> methodsDescriptions = new ArrayList<>();
        private List<JaxrsResourceLocatorDescription> subLocatorDescriptions = new ArrayList<>();

        @Override
        public int compareTo(JaxrsResourceLocatorDescription o) {
            return resourceClass.getCanonicalName().compareTo(o.resourceClass.getCanonicalName());
        }

        public ModelNode toModelNode() {
            ModelNode node = new ModelNode();
            node.get(RESOURCE_CLASS.getName()).set(resourceClass.getCanonicalName());
            ModelNode resPathNode = node.get(RESOURCE_PATHS.getName());
            Collections.sort(methodsDescriptions);
            for (JaxrsResourceMethodDescription methodRes : methodsDescriptions) {
                resPathNode.add(methodRes.toModelNode());
            }
            ModelNode subResNode = node.get(SUB_RESOURCE_LOCATORS.getName());
            Collections.sort(subLocatorDescriptions);
            for (JaxrsResourceLocatorDescription subLocator : subLocatorDescriptions) {
                subResNode.add(subLocator.toModelNode());
            }
            return node;
        }

        private boolean containsMethodResources() {
            if (this.methodsDescriptions.size() > 0) {
                return true;
            }
            for (JaxrsResourceLocatorDescription p : this.subLocatorDescriptions) {
                if (p.containsMethodResources()) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class JaxrsResourceMethodDescription implements Comparable<JaxrsResourceMethodDescription> {

        private Class<?> resourceClass;
        private String resourcePath;
        private Method method;
        private List<ParamInfo> parameters = new ArrayList<>();
        private Set<String> httpMethods = Collections.emptySet();
        private MediaType[] consumeTypes;
        private MediaType[] produceTypes;

        private Collection<String> servletMappings = Collections.emptyList();
        private String contextPath;

        @Override
        public int compareTo(JaxrsResourceMethodDescription other) {
            int result = this.resourcePath.compareTo(other.resourcePath);
            if (result == 0) {
                result = this.resourceClass.getCanonicalName().compareTo(other.resourceClass.getCanonicalName());
            }
            if (result == 0) {
                result = this.method.getName().compareTo(other.method.getName());
            }
            return result;
        }

        ModelNode toModelNode() {
            ModelNode node = new ModelNode();
            node.get(RESOURCE_PATH.getName()).set(resourcePath);
            ModelNode consumeNode = node.get(CONSUMES.getName());
            if (consumeTypes != null && consumeTypes.length > 0) {
                for (MediaType consume : consumeTypes) {
                    consumeNode.add(consume.toString());
                }
            }
            ModelNode produceNode = node.get(PRODUCES.getName());
            if (produceTypes != null && produceTypes.length > 0) {
                for (MediaType produce : produceTypes) {
                    produceNode.add(produce.toString());
                }
            }
            node.get(JAVA_METHOD.getName()).set(formatJavaMethod());
            for (final String servletMapping : servletMappings) {
                for (final String httpMethod : httpMethods) {
                    node.get(RESOURCE_METHODS.getName()).add(httpMethod + " " + formatPath(servletMapping, contextPath, resourcePath));
                }
            }
            return node;
        }

        private String formatPath(String servletMapping, String ctxPath, String resPath) {
            StringBuilder sb = new StringBuilder();
            String servletPath = servletMapping.replaceAll("\\*", "");
            if (servletPath.charAt(0) == '/') {
                servletPath = servletPath.substring(1);
            }
            sb.append(ctxPath).append('/').append(servletPath).append(resPath);
            return sb.toString().replace("//", "/");
        }

        private String formatJavaMethod() {
            StringBuilder sb = new StringBuilder();
            sb.append(method.getReturnType().getCanonicalName()).append(" ").append(resourceClass.getCanonicalName())
                    .append(".").append(method.getName()).append('(');
            int i = 0;
            for (ParamInfo param : this.parameters) {
                if (param.type != null) {
                    sb.append(param.type).append(" ");
                }
                sb.append(param.cls.getCanonicalName()).append(" ").append(param.name);
                if (param.defaultValue != null) {
                    sb.append(" = '");
                    sb.append(param.defaultValue);
                    sb.append("'");
                }
                if (++i < this.parameters.size()) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }

    private static class ParamInfo {
        private String name;
        private Class<?> cls;
        private String type; // @PathParam, or @CookieParam, or @QueryParam, etc
        private String defaultValue;
    }
}

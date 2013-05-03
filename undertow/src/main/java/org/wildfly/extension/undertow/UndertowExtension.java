package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "undertow";
    public static final PathElement PATH_ERROR_PAGE = PathElement.pathElement(Constants.ERROR_PAGE);
    public static final PathElement PATH_SIMPLE_ERROR_PAGE = PathElement.pathElement(Constants.SIMPLE_ERROR_PAGE);
    public static final PathElement PATH_ERROR_HANDLERS = PathElement.pathElement(Constants.CONFIGURATION, Constants.ERROR_HANDLER);
    public static final PathElement PATH_HANDLERS = PathElement.pathElement(Constants.CONFIGURATION, Constants.HANDLER);
    public static final PathElement PATH_FILTERS = PathElement.pathElement(Constants.CONFIGURATION, Constants.FILTER);
    protected static final PathElement PATH_JSP = PathElement.pathElement(Constants.SETTING, Constants.JSP);
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    protected static final PathElement AJP_LISTENER_PATH = PathElement.pathElement(Constants.AJP_LISTENER);
    protected static final PathElement HOST_PATH = PathElement.pathElement(Constants.HOST);
    protected static final PathElement HTTP_LISTENER_PATH = PathElement.pathElement(Constants.HTTP_LISTENER);
    protected static final PathElement HTTPS_LISTENER_PATH = PathElement.pathElement(Constants.HTTPS_LISTENER);
    protected static final PathElement PATH_SERVLET_CONTAINER = PathElement.pathElement(Constants.SERVLET_CONTAINER);
    protected static final PathElement PATH_BUFFER_CACHE = PathElement.pathElement(Constants.BUFFER_CACHE);
    protected static final PathElement PATH_LOCATION = PathElement.pathElement(Constants.LOCATION);
    protected static final PathElement SERVER_PATH = PathElement.pathElement(Constants.SERVER);
    private static final String RESOURCE_NAME = UndertowExtension.class.getPackage().getName() + ".LocalDescriptions";
    public static final PathElement PATH_FILTER_REF = PathElement.pathElement(Constants.FILTER_REF);

    public static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        /*StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, UndertowExtension.class.getClassLoader(), true, false);*/
        //todo for now we don't care about this and since model is subject to often change in this phase, no need to resolve properties
        return new NonResolvingResourceDescriptionResolver();
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.UNDERTOW_1_0.getUriString(), UndertowSubsystemParser.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(UndertowRootDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
     /*   registration.registerSubModel(BufferCacheDefinition.INSTANCE);
        registration.registerSubModel(ServerDefinition.INSTANCE);
        registration.registerSubModel(ServletContainerDefinition.INSTANCE);
        registration.registerSubModel(ErrorHandlerDefinition.INSTANCE);*/


        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(DeploymentDefinition.INSTANCE);
        deployments.registerSubModel(DeploymentServletDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(UndertowSubsystemParser.INSTANCE);
    }


}

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

package org.jboss.as.jacorb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.jacorb.deployment.JacORBDependencyProcessor;
import org.jboss.as.jacorb.deployment.JacORBMarkerProcessor;
import org.jboss.as.jacorb.naming.jndi.JBossCNCtxFactory;
import org.jboss.as.jacorb.rmi.DelegatingStubFactoryFactory;
import org.jboss.as.jacorb.security.DomainServerSocketFactory;
import org.jboss.as.jacorb.security.DomainSocketFactory;
import org.jboss.as.jacorb.service.CorbaNamingService;
import org.jboss.as.jacorb.service.CorbaORBService;
import org.jboss.as.jacorb.service.CorbaPOAService;
import org.jboss.as.naming.InitialContext;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;

/**
 * <p>
 * This class implements a {@code ModelAddOperationHandler} that installs the JacORB subsystem services:
 * <ul>
 * <li>{@code CorbaORBService}: responsible for configuring and starting the CORBA {@code ORB}.</li>
 * <li>{@code CorbaPOAService}: responsible for creating and activating CORBA {@code POA}s.</li>
 * <li>{@code CorbaNamingService}: responsible for creating and starting the CORBA naming service.</li>
 * </ul>
 * After the {@code ORB} is created, we create and activate the "RootPOA" and then use this {@code POA} to create the
 * {@code POA}s required by the other services.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JacORBSubsystemAdd extends AbstractAddStepHandler {

    private static final String JACORB_SOCKET_BINDING = "jacorb";

    private static final String JACORB_SSL_SOCKET_BINDING = "jacorb-ssl";

    static final JacORBSubsystemAdd INSTANCE = new JacORBSubsystemAdd();

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private JacORBSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // populate the submodel.
        for (AttributeDefinition attrDefinition : JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            attrDefinition.validateAndSet(operation, model);
        }
        // if generic properties have been specified, add them to the model as well.
        String properties = JacORBSubsystemConstants.PROPERTIES;
        if (operation.hasDefined(properties))
            model.get(properties).set(operation.get(properties));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> newControllers) throws OperationFailedException {

        JacORBLogger.ROOT_LOGGER.activatingSubsystem();

        // set the ORBUseDynamicStub system property.
        SecurityActions.setSystemProperty("org.jboss.com.sun.CORBA.ORBUseDynamicStub", "true");
        //we set the same stub factory to both the static and dynamic stub factory. As there is no way to dynamically change
        //the userDynamicStubs's property at runtime it is possible for the ORB class's <clinit> method to be
        //called before this property is set.
        //TODO: investigate a better way to handle this
        org.jboss.com.sun.corba.se.spi.orb.ORB.getPresentationManager().setStubFactoryFactory(true, new DelegatingStubFactoryFactory());
        org.jboss.com.sun.corba.se.spi.orb.ORB.getPresentationManager().setStubFactoryFactory(false, new DelegatingStubFactoryFactory());

        //setup naming.
        InitialContext.addUrlContextFactory("corbaloc", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("corbaname", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("IOR", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("iiopname", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("iiop", JBossCNCtxFactory.INSTANCE);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JACORB, new JacORBDependencyProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_JACORB, new JacORBMarkerProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        // get the configured ORB properties.
        Properties props = this.getConfigurationProperties(context, model);

        // setup the ORB initializers using the configured properties.
        this.setupInitializers(props);

        // setup the SSL socket factories, if necessary.
        this.setupSSLFactories(props);

        // create the service that initializes and starts the CORBA ORB.
        CorbaORBService orbService = new CorbaORBService(props);
        final ServiceBuilder<ORB> builder = context.getServiceTarget().addService(
                CorbaORBService.SERVICE_NAME, orbService);
        // inject the socket bindings that specify the JacORB IIOP and IIOP/SSL ports.
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(JACORB_SOCKET_BINDING), SocketBinding.class,
                orbService.getJacORBSocketBindingInjector());
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(JACORB_SSL_SOCKET_BINDING), SocketBinding.class,
                orbService.getJacORBSSLSocketBindingInjector());
        builder.addListener(verificationHandler);
        // set the initial mode and install the service.
        newControllers.add(builder.setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the service the initializes the Root POA.
        CorbaPOAService rootPOAService = new CorbaPOAService("RootPOA", "poa");
        newControllers.add(context.getServiceTarget().addService(CorbaPOAService.ROOT_SERVICE_NAME, rootPOAService).
                addDependency(CorbaORBService.SERVICE_NAME, ORB.class, rootPOAService.getORBInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the service the initializes the interface repository POA.
        final CorbaPOAService irPOAService = new CorbaPOAService("IRPOA", "irpoa", IdAssignmentPolicyValue.USER_ID,
                null, null, LifespanPolicyValue.PERSISTENT, null, null, null);
        newControllers.add(context.getServiceTarget().addService(CorbaPOAService.INTERFACE_REPOSITORY_SERVICE_NAME, irPOAService).
                addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class,
                        irPOAService.getParentPOAInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the service that initializes the naming service POA.
        final CorbaPOAService namingPOAService = new CorbaPOAService("Naming", null, IdAssignmentPolicyValue.USER_ID,
                null, null, LifespanPolicyValue.PERSISTENT, null, null, null);
        newControllers.add(context.getServiceTarget().addService(CorbaPOAService.SERVICE_NAME.append("namingpoa"), namingPOAService).
                addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class,
                        namingPOAService.getParentPOAInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the CORBA naming service.
        final CorbaNamingService namingService = new CorbaNamingService();
        newControllers.add(context.getServiceTarget().addService(CorbaNamingService.SERVICE_NAME, namingService).
                addDependency(CorbaORBService.SERVICE_NAME, ORB.class, namingService.getORBInjector()).
                addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class,
                        namingService.getRootPOAInjector()).
                addDependency(CorbaPOAService.SERVICE_NAME.append("namingpoa"), POA.class,
                        namingService.getNamingPOAInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());
    }

    /**
     * <p>
     * Obtains the subsystem configuration properties from the specified {@code ModelNode}, using default values for
     * undefined properties. If the property has a JacORB equivalent, it is translated into its JacORB counterpart
     * before being added to the returned {@code Properties} object.
     * </p>
     *
     * @param model the {@code ModelNode} that contains the subsystem configuration properties.
     * @return a {@code Properties} instance containing all configured subsystem properties.
     * @throws OperationFailedException if an error occurs while resolving the properties.
     */
    private Properties getConfigurationProperties(OperationContext context, ModelNode model) throws OperationFailedException {
        Properties props = new Properties();

        // get the configuration properties from the attribute definitions.
        for (AttributeDefinition attrDefinition : JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            ModelNode resolvedModelAttribute = attrDefinition.resolveModelAttribute(context, model);
            if (resolvedModelAttribute.isDefined()) {
                String name = attrDefinition.getName();
                String value = resolvedModelAttribute.asString();

                // check if the property can be mapped to a jacorb property.
                String jacorbProperty = PropertiesMap.JACORB_PROPS_MAP.get(name);
                if (jacorbProperty != null)
                    name = jacorbProperty;

                // check if the property is an SSL config property, in which case the value must be mapped to the JacORB
                // integer representation.
                if (JacORBSubsystemDefinitions.SSL_CONFIG_ATTRIBUTES.contains(attrDefinition)) {
                    SSLConfigValue sslConfigValue = SSLConfigValue.valueOf(value.toUpperCase());
                    if (sslConfigValue != null)
                        value = sslConfigValue.getJacorbValue();
                }
                props.setProperty(name, value);
            }
        }

        // check if the node contains a list of generic properties.
        if (model.hasDefined(JacORBSubsystemConstants.PROPERTIES)) {
            ModelNode propertiesNode = model.get(JacORBSubsystemConstants.PROPERTIES);

            for (Property property : propertiesNode.asPropertyList()) {
                String name = property.getName();
                ModelNode value = property.getValue();
                props.setProperty(name, value.asString());
            }
        }
        return props;
    }

    /**
     * <p>
     * Sets up the ORB initializers according to what has been configured in the subsystem.
     * </p>
     *
     * @param props the subsystem configuration properties.
     */
    private void setupInitializers(Properties props) {
        List<String> orbInitializers = new ArrayList<String>();

        // check which groups of initializers are to be installed.
        String installSecurity = (String) props.remove(JacORBSubsystemConstants.ORB_INIT_SECURITY);
        if (installSecurity.equalsIgnoreCase("on"))
            orbInitializers.addAll(Arrays.asList(ORBInitializer.SECURITY.getInitializerClasses()));

        String installTransaction = (String) props.remove(JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS);
        if (installTransaction.equalsIgnoreCase("on")) {
            orbInitializers.addAll(Arrays.asList(ORBInitializer.TRANSACTIONS.getInitializerClasses()));
        } else if(installTransaction.equalsIgnoreCase("spec")) {
            orbInitializers.addAll(Arrays.asList(ORBInitializer.SPEC_TRANSACTIONS.getInitializerClasses()));
        }

        // add the standard jacorb initializer plus all configured initializers.
        props.setProperty(JacORBSubsystemConstants.JACORB_STD_INITIALIZER_KEY,
                JacORBSubsystemConstants.JACORB_STD_INITIALIZER_VALUE);
        for (String initializerClass : orbInitializers)
            props.setProperty(JacORBSubsystemConstants.ORB_INITIALIZER_PREFIX + initializerClass, "");
    }

    /**
     * <p>
     * Sets up the SSL domain socket factories if SSL support has been enabled.
     * </p>
     *
     * @param props the subsystem configuration properties.
     * @throws OperationFailedException if the SSL setup has not been done correctly (SSL support has been turned on
     * but no security domain has been specified).
     */
    private void setupSSLFactories(Properties props) throws OperationFailedException {
        String supportSSLKey = PropertiesMap.JACORB_PROPS_MAP.get(JacORBSubsystemConstants.SECURITY_SUPPORT_SSL);
        boolean supportSSL = "on".equalsIgnoreCase(props.getProperty(supportSSLKey));

        if (supportSSL) {
            // if SSL is to be used, check if a security domain has been specified.
            String securityDomain = props.getProperty(JacORBSubsystemConstants.SECURITY_SECURITY_DOMAIN);
            if (securityDomain == null || securityDomain.isEmpty())
                throw JacORBMessages.MESSAGES.noSecurityDomainSpecified();

            // add the domain socket factories.
            props.setProperty(JacORBSubsystemConstants.JACORB_SSL_SOCKET_FACTORY, DomainSocketFactory.class.getName());
            props.setProperty(JacORBSubsystemConstants.JACORB_SSL_SERVER_SOCKET_FACTORY, DomainServerSocketFactory.class.getName());
        }
    }
}

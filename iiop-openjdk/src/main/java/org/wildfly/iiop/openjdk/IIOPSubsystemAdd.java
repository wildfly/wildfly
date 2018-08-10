/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.InitialContext;
import org.jboss.as.naming.service.DefaultNamespaceContextSelectorService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.metadata.ejb.jboss.IORASContextMetaData;
import org.jboss.metadata.ejb.jboss.IORSASContextMetaData;
import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.jboss.metadata.ejb.jboss.IORTransportConfigMetaData;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.wildfly.iiop.openjdk.csiv2.CSIV2IORToSocketInfo;
import org.wildfly.iiop.openjdk.csiv2.ElytronSASClientInterceptor;
import org.wildfly.iiop.openjdk.deployment.IIOPDependencyProcessor;
import org.wildfly.iiop.openjdk.deployment.IIOPMarkerProcessor;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.naming.jndi.JBossCNCtxFactory;
import org.wildfly.iiop.openjdk.rmi.DelegatingStubFactoryFactory;
import org.wildfly.iiop.openjdk.security.NoSSLSocketFactory;
import org.wildfly.iiop.openjdk.security.LegacySSLSocketFactory;
import org.wildfly.iiop.openjdk.security.SSLSocketFactory;
import org.wildfly.iiop.openjdk.service.CorbaNamingService;
import org.wildfly.iiop.openjdk.service.CorbaORBService;
import org.wildfly.iiop.openjdk.service.CorbaPOAService;
import org.wildfly.iiop.openjdk.service.IORSecConfigMetaDataService;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.manager.WildFlySecurityManager;

import com.sun.corba.se.impl.orbutil.ORBConstants;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;

/**
 * <p>
 * This class implements a {@code ModelAddOperationHandler} that installs the IIOP subsystem services:
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
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public IIOPSubsystemAdd(final Collection<? extends AttributeDefinition> attributes) {
        super(attributes);
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        // This needs to run after all child resources so that they can detect a fresh state
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                ModelNode node = Resource.Tools.readModel(resource);
                launchServices(context, node);
                // Rollback handled by the parent step
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();
        ConfigValidator.validateConfig(context, model);
    }

    protected void launchServices(final OperationContext context, final ModelNode model) throws OperationFailedException {

        IIOPLogger.ROOT_LOGGER.activatingSubsystem();

        // set the ORBUseDynamicStub system property.
        WildFlySecurityManager.setPropertyPrivileged("org.jboss.com.sun.CORBA.ORBUseDynamicStub", "true");
        // we set the same stub factory to both the static and dynamic stub factory. As there is no way to dynamically change
        // the userDynamicStubs's property at runtime it is possible for the ORB class's <clinit> method to be
        // called before this property is set.
        // TODO: investigate a better way to handle this
        com.sun.corba.se.spi.orb.ORB.getPresentationManager().setStubFactoryFactory(true,
                new DelegatingStubFactoryFactory());
        com.sun.corba.se.spi.orb.ORB.getPresentationManager().setStubFactoryFactory(false,
                new DelegatingStubFactoryFactory());

        // setup naming.
        InitialContext.addUrlContextFactory("corbaloc", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("corbaname", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("IOR", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("iiopname", JBossCNCtxFactory.INSTANCE);
        InitialContext.addUrlContextFactory("iiop", JBossCNCtxFactory.INSTANCE);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(IIOPExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        Phase.DEPENDENCIES_IIOP_OPENJDK, new IIOPDependencyProcessor());
                processorTarget.addDeploymentProcessor(IIOPExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_IIOP_OPENJDK,
                        new IIOPMarkerProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        // get the configured ORB properties.
        Properties props = this.getConfigurationProperties(context, model);

        // setup the ORB initializers using the configured properties.
        this.setupInitializers(props);

        // setup the SSL socket factories, if necessary.
        final boolean sslConfigured = this.setupSSLFactories(props);

        // create the service that initializes and starts the CORBA ORB.
        CorbaORBService orbService = new CorbaORBService(props);
        final ServiceBuilder<ORB> builder = context.getServiceTarget().addService(CorbaORBService.SERVICE_NAME, orbService);
        org.jboss.as.server.Services.addServerExecutorDependency(builder, orbService.getExecutorInjector());

        // if a security domain has been specified, add a dependency to the domain service.
        String securityDomain = props.getProperty(Constants.SECURITY_SECURITY_DOMAIN);
        if (securityDomain != null) {
            builder.addDependency(context.getCapabilityServiceName(Capabilities.LEGACY_SECURITY_DOMAIN_CAPABILITY, securityDomain, null));
            builder.addDependency(DefaultNamespaceContextSelectorService.SERVICE_NAME);
        }

        // add dependencies to the ssl context services if needed.
        final String serverSSLContextName = props.getProperty(Constants.SERVER_SSL_CONTEXT);
        if (serverSSLContextName != null) {
            ServiceName serverContextServiceName = context.getCapabilityServiceName(Capabilities.SSL_CONTEXT_CAPABILITY, serverSSLContextName, SSLContext.class);
            builder.addDependency(serverContextServiceName);
        }
        final String clientSSLContextName = props.getProperty(Constants.CLIENT_SSL_CONTEXT);
        if (clientSSLContextName != null) {
            ServiceName clientContextServiceName = context.getCapabilityServiceName(Capabilities.SSL_CONTEXT_CAPABILITY, clientSSLContextName, SSLContext.class);
            builder.addDependency(clientContextServiceName);
        }

        // if an authentication context has ben specified, add a dependency to its service.
        final String authContext = props.getProperty(Constants.ORB_INIT_AUTH_CONTEXT);
        if (authContext != null) {
            ServiceName authContextServiceName = context.getCapabilityServiceName(Capabilities.AUTH_CONTEXT_CAPABILITY, authContext, AuthenticationContext.class);
            builder.addDependency(authContextServiceName);
        }

        final boolean serverRequiresSsl = IIOPRootDefinition.SERVER_REQUIRES_SSL.resolveModelAttribute(context, model).asBoolean();

        // inject the socket bindings that specify IIOP and IIOP/SSL ports.
        String socketBinding = props.getProperty(Constants.ORB_SOCKET_BINDING);
        if (socketBinding != null) {
            if (!serverRequiresSsl) {
                builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBinding), SocketBinding.class,
                        orbService.getIIOPSocketBindingInjector());
            } else {
                IIOPLogger.ROOT_LOGGER.wontUseCleartextSocket();
            }
        }


        String sslSocketBinding = props.getProperty(Constants.ORB_SSL_SOCKET_BINDING);
        if(sslSocketBinding != null) {
            builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(sslSocketBinding), SocketBinding.class,
                    orbService.getIIOPSSLSocketBindingInjector());
        }

        // create the IOR security config metadata service.
        final IORSecurityConfigMetaData securityConfigMetaData = this.createIORSecurityConfigMetaData(context,
                model, sslConfigured, serverRequiresSsl);
        final IORSecConfigMetaDataService securityConfigMetaDataService = new IORSecConfigMetaDataService(securityConfigMetaData);
        context.getServiceTarget()
                .addService(IORSecConfigMetaDataService.SERVICE_NAME, securityConfigMetaDataService)
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

        builder.addDependency(IORSecConfigMetaDataService.SERVICE_NAME);

        // set the initial mode and install the service.
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        // create the service the initializes the Root POA.
        CorbaPOAService rootPOAService = new CorbaPOAService("RootPOA", "poa", serverRequiresSsl);
        context.getServiceTarget().addService(CorbaPOAService.ROOT_SERVICE_NAME, rootPOAService)
                .addDependency(CorbaORBService.SERVICE_NAME, ORB.class, rootPOAService.getORBInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

        // create the service the initializes the interface repository POA.
        final CorbaPOAService irPOAService = new CorbaPOAService("IRPOA", "irpoa", serverRequiresSsl, IdAssignmentPolicyValue.USER_ID, null, null,
                LifespanPolicyValue.PERSISTENT, null, null, null);
        context.getServiceTarget()
                .addService(CorbaPOAService.INTERFACE_REPOSITORY_SERVICE_NAME, irPOAService)
                .addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class, irPOAService.getParentPOAInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

        // create the service that initializes the naming service POA.
        final CorbaPOAService namingPOAService = new CorbaPOAService("Naming", null, serverRequiresSsl, IdAssignmentPolicyValue.USER_ID, null,
                null, LifespanPolicyValue.PERSISTENT, null, null, null);
        context.getServiceTarget()
                .addService(CorbaPOAService.SERVICE_NAME.append("namingpoa"), namingPOAService)
                .addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class, namingPOAService.getParentPOAInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

        // create the CORBA naming service.
        final CorbaNamingService namingService = new CorbaNamingService(props);
        context
                .getServiceTarget()
                .addService(CorbaNamingService.SERVICE_NAME, namingService)
                .addDependency(CorbaORBService.SERVICE_NAME, ORB.class, namingService.getORBInjector())
                .addDependency(CorbaPOAService.ROOT_SERVICE_NAME, POA.class, namingService.getRootPOAInjector())
                .addDependency(CorbaPOAService.SERVICE_NAME.append("namingpoa"), POA.class,
                        namingService.getNamingPOAInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

        configureClientSecurity(props);
    }

    /**
     * <p>
     * Obtains the subsystem configuration properties from the specified {@code ModelNode}, using default values for undefined
     * properties. If the property has a IIOP equivalent, it is translated into its IIOP counterpart before being added to
     * the returned {@code Properties} object.
     * </p>
     *
     * @param model the {@code ModelNode} that contains the subsystem configuration properties.
     * @return a {@code Properties} instance containing all configured subsystem properties.
     * @throws OperationFailedException if an error occurs while resolving the properties.
     */
    protected Properties getConfigurationProperties(OperationContext context, ModelNode model) throws OperationFailedException {
        Properties props = new Properties();

        getResourceProperties(props, IIOPRootDefinition.INSTANCE, context, model);


        // check if the node contains a list of generic properties.
        ModelNode configNode = model.get(Constants.CONFIGURATION);
        if (configNode.hasDefined(Constants.PROPERTIES)) {
            for (Property property : configNode.get(Constants.PROPERTIES).get(Constants.PROPERTY)
                    .asPropertyList()) {
                String name = property.getName();
                String value = property.getValue().get(Constants.PROPERTY_VALUE).asString();
                props.setProperty(name, value);
            }
        }
        return props;
    }

    private void getResourceProperties(final Properties properties, PersistentResourceDefinition resource,
            OperationContext context, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attrDefinition : resource.getAttributes()) {
            if(attrDefinition instanceof PropertiesAttributeDefinition){
                PropertiesAttributeDefinition pad=(PropertiesAttributeDefinition)attrDefinition;
                ModelNode resolvedModelAttribute = attrDefinition.resolveModelAttribute(context, model);
                if(resolvedModelAttribute.isDefined()) {
                    for (final Property prop : resolvedModelAttribute.asPropertyList()) {
                        properties.setProperty(prop.getName(), prop.getValue().asString());
                    }
                }
                continue;
            }
            ModelNode resolvedModelAttribute = attrDefinition.resolveModelAttribute(context, model);
            //FIXME
            if (resolvedModelAttribute.isDefined()) {
                String name = attrDefinition.getName();
                String value = resolvedModelAttribute.asString();

                String openjdkProperty = PropertiesMap.PROPS_MAP.get(name);
                if (openjdkProperty != null) {
                    name = openjdkProperty;
                }
                properties.setProperty(name, value);
            }
        }
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
        String installSecurity = (String) props.remove(Constants.ORB_INIT_SECURITY);
        if (installSecurity.equalsIgnoreCase(Constants.CLIENT)) {
            orbInitializers.addAll(Arrays.asList(IIOPInitializer.SECURITY_CLIENT.getInitializerClasses()));
        } else if (installSecurity.equalsIgnoreCase(Constants.IDENTITY)) {
            orbInitializers.addAll(Arrays.asList(IIOPInitializer.SECURITY_IDENTITY.getInitializerClasses()));
        } else if (installSecurity.equalsIgnoreCase(Constants.ELYTRON)) {
            final String authContext = props.getProperty(Constants.ORB_INIT_AUTH_CONTEXT);
            ElytronSASClientInterceptor.setAuthenticationContextName(authContext);
            orbInitializers.addAll(Arrays.asList(IIOPInitializer.SECURITY_ELYTRON.getInitializerClasses()));
        }

        String installTransaction = (String) props.remove(Constants.ORB_INIT_TRANSACTIONS);
        if (installTransaction.equalsIgnoreCase(Constants.FULL)) {
            orbInitializers.addAll(Arrays.asList(IIOPInitializer.TRANSACTIONS.getInitializerClasses()));
        } else if (installTransaction.equalsIgnoreCase(Constants.SPEC)) {
            orbInitializers.addAll(Arrays.asList(IIOPInitializer.SPEC_TRANSACTIONS.getInitializerClasses()));
        }

        // add the standard opendk initializer plus all configured initializers.
        for (String initializerClass : orbInitializers) {
            props.setProperty(Constants.ORB_INITIALIZER_PREFIX + initializerClass, "");
        }
    }

    /**
     * <p>
     * Sets up the SSL domain socket factories if SSL support has been enabled.
     * </p>
     *
     * @param props the subsystem configuration properties.
     * @return true if ssl has been configured
     * @throws OperationFailedException if the SSL setup has not been done correctly (SSL support has been turned on but no
     *         security domain has been specified).
     */
    private boolean setupSSLFactories(final Properties props) throws OperationFailedException {
        final boolean supportSSL = "true".equalsIgnoreCase(props.getProperty(Constants.SECURITY_SUPPORT_SSL));

        final boolean sslConfigured;
        if (supportSSL) {
            // if the config is using Elytron supplied SSL contexts, install the SSLSocketFactory.
            final String serverSSLContextName = props.getProperty(Constants.SERVER_SSL_CONTEXT);
            final String clientSSLContextName = props.getProperty(Constants.CLIENT_SSL_CONTEXT);
            if (serverSSLContextName != null && clientSSLContextName != null) {
                SSLSocketFactory.setServerSSLContextName(serverSSLContextName);
                SSLSocketFactory.setClientSSLContextName(clientSSLContextName);
                props.setProperty(ORBConstants.SOCKET_FACTORY_CLASS_PROPERTY, SSLSocketFactory.class.getName());
            }
            else {
                // if the config only has a legacy JSSE domain reference, install the LegacySSLSocketFactory.
                final String securityDomain = props.getProperty(Constants.SECURITY_SECURITY_DOMAIN);
                LegacySSLSocketFactory.setSecurityDomain(securityDomain);
                props.setProperty(ORBConstants.SOCKET_FACTORY_CLASS_PROPERTY, LegacySSLSocketFactory.class.getName());
            }
            sslConfigured = true;
        } else {
            props.setProperty(ORBConstants.SOCKET_FACTORY_CLASS_PROPERTY, NoSSLSocketFactory.class.getName());
            sslConfigured = false;
        }

        return sslConfigured;
    }

    private IORSecurityConfigMetaData createIORSecurityConfigMetaData(final OperationContext context, final ModelNode resourceModel, final boolean sslConfigured, final boolean serverRequiresSsl)
            throws OperationFailedException {
        final IORSecurityConfigMetaData securityConfigMetaData = new IORSecurityConfigMetaData();

        final IORSASContextMetaData sasContextMetaData = new IORSASContextMetaData();
        sasContextMetaData.setCallerPropagation(IIOPRootDefinition.CALLER_PROPAGATION.resolveModelAttribute(context, resourceModel).asString());
        securityConfigMetaData.setSasContext(sasContextMetaData);

        final IORASContextMetaData asContextMetaData = new IORASContextMetaData();
        asContextMetaData.setAuthMethod(IIOPRootDefinition.AUTH_METHOD.resolveModelAttribute(context, resourceModel).asString());
        if (resourceModel.hasDefined(IIOPRootDefinition.REALM.getName())) {
            asContextMetaData.setRealm(IIOPRootDefinition.REALM.resolveModelAttribute(context, resourceModel).asString());
        }
        asContextMetaData.setRequired(IIOPRootDefinition.REQUIRED.resolveModelAttribute(context, resourceModel).asBoolean());
        securityConfigMetaData.setAsContext(asContextMetaData);

        final IORTransportConfigMetaData transportConfigMetaData = new IORTransportConfigMetaData();
        final ModelNode integrityNode = IIOPRootDefinition.INTEGRITY.resolveModelAttribute(context, resourceModel);
        if(integrityNode.isDefined()){
            transportConfigMetaData.setIntegrity(integrityNode.asString());
        } else {
            transportConfigMetaData.setIntegrity(sslConfigured ? (serverRequiresSsl ? Constants.IOR_REQUIRED : Constants.IOR_SUPPORTED) : Constants.NONE);
        }

        final ModelNode confidentialityNode = IIOPRootDefinition.CONFIDENTIALITY.resolveModelAttribute(context, resourceModel);
        if(confidentialityNode.isDefined()){
            transportConfigMetaData.setConfidentiality(confidentialityNode.asString());
        } else {
            transportConfigMetaData.setConfidentiality(sslConfigured ? (serverRequiresSsl ? Constants.IOR_REQUIRED: Constants.IOR_SUPPORTED) : Constants.IOR_NONE);
        }

        final ModelNode establishTrustInTargetNode = IIOPRootDefinition.TRUST_IN_TARGET.resolveModelAttribute(context, resourceModel);
        if (establishTrustInTargetNode.isDefined()) {
            transportConfigMetaData.setEstablishTrustInTarget(confidentialityNode.asString());
        } else {
            transportConfigMetaData.setEstablishTrustInTarget(sslConfigured ? Constants.IOR_SUPPORTED : Constants.NONE);
        }

        final ModelNode establishTrustInClientNode = IIOPRootDefinition.TRUST_IN_CLIENT.resolveModelAttribute(context, resourceModel);
        if(establishTrustInClientNode.isDefined()){
            transportConfigMetaData.setEstablishTrustInClient(establishTrustInClientNode.asString());
        } else {
            transportConfigMetaData.setEstablishTrustInClient(sslConfigured ? (serverRequiresSsl ? Constants.IOR_REQUIRED : Constants.IOR_SUPPORTED) : Constants.NONE);
        }

        transportConfigMetaData.setDetectMisordering(Constants.IOR_SUPPORTED);
        transportConfigMetaData.setDetectReplay(Constants.IOR_SUPPORTED);

        securityConfigMetaData.setTransportConfig(transportConfigMetaData);
        return securityConfigMetaData;
    }

    private void configureClientSecurity(final Properties props) {
        final boolean clientRequiresSSL = Boolean.getBoolean(props.getProperty(Constants.SECURITY_CLIENT_REQUIRES_SSL));
        CSIV2IORToSocketInfo.setClientRequiresSSL(clientRequiresSSL);
    }
}

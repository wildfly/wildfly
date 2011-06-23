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

import static org.jboss.as.jacorb.JacORBElement.INITIALIZERS_CONFIG;
import static org.jboss.as.jacorb.JacORBElement.INTEROP_CONFIG;
import static org.jboss.as.jacorb.JacORBElement.ORB_CONFIG;
import static org.jboss.as.jacorb.JacORBElement.POA_CONFIG;
import static org.jboss.as.jacorb.JacORBElement.PROPERTY_CONFIG;
import static org.jboss.as.jacorb.JacORBElement.SECURITY_CONFIG;

import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.jacorb.service.CorbaNamingService;
import org.jboss.as.jacorb.service.CorbaORBService;
import org.jboss.as.jacorb.service.CorbaPOAService;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
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

    private static final Logger log = Logger.getLogger("org.jboss.as.jacorb");

    private static final String JACORB_SOCKET_BINDING = "jacorb";

    private static final String JACORB_SSL_SOCKET_BINDING = "jacorb-ssl";

    static final JacORBSubsystemAdd INSTANCE = new JacORBSubsystemAdd();

    private final ParametersValidator jacorbConfigValidator = new ParametersValidator();

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private JacORBSubsystemAdd() {
        ModelTypeValidator optionalObjectTypeValidator = new ModelTypeValidator(ModelType.OBJECT, true);
        this.jacorbConfigValidator.registerValidator(ORB_CONFIG.getLocalName(), optionalObjectTypeValidator);
        this.jacorbConfigValidator.registerValidator(POA_CONFIG.getLocalName(), optionalObjectTypeValidator);
        this.jacorbConfigValidator.registerValidator(INTEROP_CONFIG.getLocalName(), optionalObjectTypeValidator);
        this.jacorbConfigValidator.registerValidator(SECURITY_CONFIG.getLocalName(), optionalObjectTypeValidator);
        this.jacorbConfigValidator.registerValidator(PROPERTY_CONFIG.getLocalName(),
                new ModelTypeValidator(ModelType.LIST, true));
        this.jacorbConfigValidator.registerValidator(INITIALIZERS_CONFIG.getLocalName(),
                new ModelTypeValidator(ModelType.STRING, true, true));
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        // validate the operation.
        this.jacorbConfigValidator.validate(operation);

        // populate the submodel.
        for (JacORBElement configElement : JacORBElement.getRootElements()) {
            String configElementName = configElement.getLocalName();
            if (operation.hasDefined(configElementName)) {
                model.get(configElementName).set(operation.get(configElementName));
            }
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> newControllers) throws OperationFailedException {

        log.info("Activating JacORB Subsystem");

        // get the list of ORB initializers.
        EnumSet<ORBInitializer> initializers = getORBInitializers(operation);

        // get the configured ORB properties.
        Properties props = new Properties();
        getConfigurationProperties(operation, props);

        // create the service that initializes and starts the CORBA ORB.
        CorbaORBService orbService = new CorbaORBService(initializers, props);
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
        newControllers.add(context.getServiceTarget().addService(CorbaPOAService.SERVICE_NAME.append("rootpoa"), rootPOAService).
                addDependency(CorbaORBService.SERVICE_NAME, ORB.class, rootPOAService.getORBInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the service the initializes the interface repository POA.
        CorbaPOAService irPOAService = new CorbaPOAService("IRPOA", "irpoa", IdAssignmentPolicyValue.USER_ID,
                null, null, LifespanPolicyValue.PERSISTENT, null, null, null);
        newControllers.add(context.getServiceTarget().addService(CorbaPOAService.SERVICE_NAME.append("irpoa"), irPOAService).
                addDependency(CorbaPOAService.SERVICE_NAME.append("rootpoa"), POA.class,
                        irPOAService.getParentPOAInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the service that initializes the naming service POA.
        CorbaPOAService namingPOAService = new CorbaPOAService("NamingPOA", null, IdAssignmentPolicyValue.USER_ID,
                null, null, LifespanPolicyValue.PERSISTENT, null, null, null);
        newControllers.add(context.getServiceTarget().addService(CorbaPOAService.SERVICE_NAME.append("namingpoa"), namingPOAService).
                addDependency(CorbaPOAService.SERVICE_NAME.append("rootpoa"), POA.class,
                        namingPOAService.getParentPOAInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());

        // create the CORBA naming service.
        CorbaNamingService namingService = new CorbaNamingService();
        newControllers.add(context.getServiceTarget().addService(CorbaNamingService.SERVICE_NAME, namingService).
                addDependency(CorbaORBService.SERVICE_NAME, ORB.class, namingService.getORBInjector()).
                addDependency(CorbaPOAService.SERVICE_NAME.append("rootpoa"), POA.class,
                        namingService.getRootPOAInjector()).
                addDependency(CorbaPOAService.SERVICE_NAME.append("namingpoa"), POA.class,
                        namingService.getNamingPOAInjector()).
                addListener(verificationHandler).
                setInitialMode(ServiceController.Mode.ACTIVE).install());
    }

    /**
     * <p>
     * Obtains the set of {@code ORBInitializer}s that have been configured in the JacORB subsystem.
     * </p>
     *
     * @param jacorbConfig the {@code ModelNode} that contains the JacORB configuration.
     * @return an {@code EnumSet} containing all configured {@code ORBInitializer}s. If the {@code orbConfig} parameter
     *         is {@code null} or if no initializers have been configured, the method returns the default {@code CODEBASE},
     *         {@code CSIv2} and {@code SAS} initializers.
     * @throws org.jboss.as.controller.OperationFailedException
     *          if an unknown initializer has been specified in the configuration.
     */
    private EnumSet<ORBInitializer> getORBInitializers(ModelNode jacorbConfig) throws OperationFailedException {
        if (jacorbConfig.hasDefined(INITIALIZERS_CONFIG.getLocalName())) {
            String value = jacorbConfig.get(INITIALIZERS_CONFIG.getLocalName()).asString();
            String[] initializers = value.split(",");

            EnumSet<ORBInitializer> orbInitializers = EnumSet.noneOf(ORBInitializer.class);
            for (String initializer : initializers) {
                ORBInitializer orbInitializer = ORBInitializer.getInitializer(initializer);
                if (orbInitializer == ORBInitializer.UNKNOWN) {
                    throw new OperationFailedException("Unknown ORB initializer", jacorbConfig);
                }
                orbInitializers.add(orbInitializer);
            }
            return orbInitializers;
        } else {
            // return the default ORB initializers.
            return EnumSet.of(ORBInitializer.CODEBASE, ORBInitializer.CSIV2, ORBInitializer.SAS);
        }
    }

    /**
     * <p>
     * Fills the provided {@code Properties} instance with the properties extracted recursively from the specified
     * {@code ModelNode}. The property names are translated to JacORB properties before being added to the
     * {@code Properties} instance. If the translation fails (that is, if that property does not correspond to a JacORB
     * property), then the configuration property name itself will be used as key.
     * </p>
     *
     * @param node  the {@code ModelNode}  that contains the configuration properties.
     * @param props the {@code Properties} instance that will hold the extracted properties.
     */
    private void getConfigurationProperties(ModelNode node, Properties props) {
        if (node == null || props == null)
            return;

        // iterate through the child nodes.
        for (Property property : node.asPropertyList()) {
            String key = property.getName();
            ModelNode value = property.getValue();
            // if the value is a complex type, use recursion to get all attributes from the value.
            if (value.getType() == ModelType.OBJECT || value.getType() == ModelType.LIST) {
                this.getConfigurationProperties(value, props);
            }
            // else try to translate the config property into a jacorb property using JacORBAttribute.
            else {
                String jacorbKey = JacORBAttribute.forName(key).getJacORBName();
                if (jacorbKey == null) {
                    jacorbKey = key;
                }
                props.setProperty(jacorbKey, value.asString());
            }
        }
    }
}

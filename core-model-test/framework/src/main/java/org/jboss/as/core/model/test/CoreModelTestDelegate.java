/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_SOURCE_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.core.model.bridge.local.ScopedKernelServicesBootstrap;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.RestartMode;
import org.jboss.as.model.test.ChildFirstClassLoaderBuilder;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestBootOperationsBuilder;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator.ValidationFailure;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLMapper;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;



/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CoreModelTestDelegate {


    private final Namespace NAMESPACE = Namespace.CURRENT;

    private final Class<?> testClass;
    private final List<KernelServices> kernelServices = new ArrayList<KernelServices>();

    public CoreModelTestDelegate(Class<?> testClass) {
        this.testClass = testClass;
    }

    void initializeParser() throws Exception {
        //Initialize the parser

    }

    void cleanup() throws Exception {
        for (KernelServices kernelServices : this.kernelServices) {
            try {
                kernelServices.shutdown();
            } catch (Exception e) {
            }
        }
        kernelServices.clear();
    }


    protected KernelServicesBuilder createKernelServicesBuilder(TestModelType type) {
        return new KernelServicesBuilderImpl(type);
    }

    private void validateDescriptionProviders(TestModelType type, KernelServices kernelServices) {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(OP_ADDR).setEmptyList();
        op.get(RECURSIVE).set(true);
        op.get(INHERITED).set(false);
        op.get(OPERATIONS).set(true);
        op.get(INCLUDE_ALIASES).set(true);
        ModelNode result = kernelServices.executeOperation(op);
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).toString());
        }
        ModelNode model = result.get(RESULT);

        if (type == TestModelType.HOST) {
            //TODO (1)
            //Big big hack to get around the fact that the tests install the host description twice
            //we're only interested in the host model anyway
            //See KnownIssuesValidator.createHostPlatformMBeanAddress
            model = model.require(CHILDREN).require(HOST).require(MODEL_DESCRIPTION).require(MASTER);
        }

        //System.out.println(model);

        ValidationConfiguration config = KnownIssuesValidationConfiguration.createAndFixupModel(type, model);

        ModelTestModelDescriptionValidator validator = new ModelTestModelDescriptionValidator(PathAddress.EMPTY_ADDRESS.toModelNode(), model, config);
        List<ValidationFailure> validationMessages = validator.validateResources();
        if (validationMessages.size() > 0) {
            final StringBuilder builder = new StringBuilder("VALIDATION ERRORS IN MODEL:");
            for (ValidationFailure failure : validationMessages) {
                builder.append(failure);
                builder.append("\n");

            }
            Assert.fail("Failed due to validation errors in the model. Please fix :-) " + builder.toString());
        }
    }

    /**
     * Checks that the transformed model is the same as the model built up in the legacy subsystem controller via the transformed operations,
     * and that the transformed model is valid according to the resource definition in the legacy subsystem controller.
     *
     * @param kernelServices the main kernel services
     * @param modelVersion   the model version of the targetted legacy subsystem
     * @param legacyModelFixer use to touch up the model read from the legacy controller, use sparingly when the legacy model is just wrong. May be {@code null}
     * @return the whole model of the legacy controller
     */
    ModelNode checkCoreModelTransformation(KernelServices kernelServices, ModelVersion modelVersion, ModelFixer legacyModelFixer, ModelFixer transformedModelFixer) throws IOException {
        KernelServices legacyServices = kernelServices.getLegacyServices(modelVersion);

        //Only read the model without any defaults
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).setEmptyList();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);
        op.get(INCLUDE_DEFAULTS).set(false);
        ModelNode legacyModel;
        try {
            legacyModel = legacyServices.executeForResult(op);
        } catch (OperationFailedException e) {
            throw new RuntimeException(e);
        }

        //Work around known problem where the recursice :read-resource on legacy controllers in ModelVersion < 1.4.0
        //incorrectly does not propagate include-defaults=true when recursing
        //https://issues.jboss.org/browse/AS7-6077
        removeDefaultAttributesWronglyShowingInRecursiveReadResource(modelVersion, legacyServices, legacyModel);

        //1) Check that the transformed model is the same as the whole model read from the legacy controller.
        //The transformed model is done via the resource transformers
        //The model in the legacy controller is built up via transformed operations
        ModelNode transformed = kernelServices.readTransformedModel(modelVersion);
        if (legacyModelFixer != null) {
            legacyModel = legacyModelFixer.fixModel(legacyModel);
        }
        if (transformedModelFixer != null) {
            transformed = transformedModelFixer.fixModel(transformed);
        }

        //TODO temporary hacks
        temporaryHack(transformed, legacyModel);

        ModelTestUtils.compare(legacyModel, transformed, true);

        //2) Check that the transformed model is valid according to the resource definition in the legacy subsystem controller
        //ResourceDefinition rd = TransformerRegistry.loadSubsystemDefinition(mainSubsystemName, modelVersion);
        //ManagementResourceRegistration rr = ManagementResourceRegistration.Factory.create(rd);
        //ModelTestUtils.checkModelAgainstDefinition(transformed, rr);
        return legacyModel;
    }

    private void temporaryHack(ModelNode transformedModel, ModelNode legacyModel) {
        if (legacyModel.hasDefined(NAMESPACES) && !transformedModel.hasDefined(NAMESPACES)) {
            if (legacyModel.get(NAMESPACES).asList().isEmpty()) {
                legacyModel.get(NAMESPACES).set(new ModelNode());
            }
        }
        if (legacyModel.hasDefined(SCHEMA_LOCATIONS) && !transformedModel.hasDefined(SCHEMA_LOCATIONS)) {
            if (legacyModel.get(SCHEMA_LOCATIONS).asList().isEmpty()) {
                legacyModel.get(SCHEMA_LOCATIONS).set(new ModelNode());
            }
        }


        //We will test these in mixed-domain instead since something differs in the test setup for these attributes
        legacyModel.remove(MANAGEMENT_MAJOR_VERSION);
        legacyModel.remove(MANAGEMENT_MINOR_VERSION);
        legacyModel.remove(MANAGEMENT_MICRO_VERSION);
        legacyModel.remove(NAME);
        legacyModel.remove(RELEASE_CODENAME);
        legacyModel.remove(RELEASE_VERSION);
        transformedModel.remove(MANAGEMENT_MAJOR_VERSION);
        transformedModel.remove(MANAGEMENT_MINOR_VERSION);
        transformedModel.remove(MANAGEMENT_MICRO_VERSION);
        transformedModel.remove(NAME);
        transformedModel.remove(RELEASE_CODENAME);
        transformedModel.remove(RELEASE_VERSION);
    }

    private void removeDefaultAttributesWronglyShowingInRecursiveReadResource(ModelVersion modelVersion, KernelServices legacyServices, ModelNode legacyModel) {

        if (modelVersion.getMajor() == 1 && modelVersion.getMinor() < 4) {
            //Work around known problem where the recursice :read-resource on legacy controllers in ModelVersion < 1.4.0
            //incorrectly does not propagate include-defaults=true when recursing
            //https://issues.jboss.org/browse/AS7-6077
            checkAttributeIsActuallyDefinedAndReplaceIfNot(legacyServices, legacyModel, MANAGEMENT_SUBSYSTEM_ENDPOINT, SERVER_GROUP);
            checkAttributeIsActuallyDefinedAndReplaceIfNot(legacyServices, legacyModel, READ_ONLY, PATH);
            removeDefaultAttributesWronglyShowingInRecursiveReadResourceInSocketBindingGroup(modelVersion, legacyServices, legacyModel);
        }
    }

    private void removeDefaultAttributesWronglyShowingInRecursiveReadResourceInSocketBindingGroup(ModelVersion modelVersion, KernelServices legacyServices, ModelNode legacyModel) {
        if (legacyModel.hasDefined(SOCKET_BINDING_GROUP)) {
            for (Property prop : legacyModel.get(SOCKET_BINDING_GROUP).asPropertyList()) {
                if (prop.getValue().isDefined()) {
                    checkAttributeIsActuallyDefinedAndReplaceIfNot(legacyServices, legacyModel, FIXED_SOURCE_PORT, SOCKET_BINDING_GROUP, prop.getName(), REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING);
                    checkAttributeIsActuallyDefinedAndReplaceIfNot(legacyServices, legacyModel, FIXED_SOURCE_PORT, SOCKET_BINDING_GROUP, prop.getName(), LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING);
                    checkAttributeIsActuallyDefinedAndReplaceIfNot(legacyServices, legacyModel, FIXED_PORT, SOCKET_BINDING_GROUP, prop.getName(), SOCKET_BINDING);
                }
            }
        }
    }

    private void checkAttributeIsActuallyDefinedAndReplaceIfNot(KernelServices legacyServices, ModelNode legacyModel, String attributeName, String...parentAddress) {

        ModelNode parentNode = legacyModel;
        for (String s : parentAddress) {
            if (parentNode.hasDefined(s)) {
                parentNode = parentNode.get(s);
            } else {
                return;
            }
            if (!parentNode.isDefined()) {
                return;
            }
        }

        for (Property prop : parentNode.asPropertyList()) {
            if (prop.getValue().isDefined()) {
                ModelNode attribute = parentNode.get(prop.getName(), attributeName);
                if (attribute.isDefined()) {
                    //Attribute is defined in the legacy model - remove it if that is not the case
                    ModelNode op = new ModelNode();
                    op.get(OP).set(READ_ATTRIBUTE_OPERATION);
                    for (int i = 0 ; i < parentAddress.length ; i ++) {
                        if (i < parentAddress.length -1) {
                            op.get(OP_ADDR).add(parentAddress[i], parentAddress[++i]);
                        } else {
                            op.get(OP_ADDR).add(parentAddress[i], prop.getName());
                        }
                    }
                    op.get(NAME).set(attributeName);
                    op.get(INCLUDE_DEFAULTS).set(false);
                    try {
                        ModelNode result = legacyServices.executeForResult(op);
                        if (!result.isDefined()) {
                                attribute.set(new ModelNode());
                        }
                    } catch (OperationFailedException e) {
                        //TODO this might get thrown because the attribute does not exist in the legacy model?
                        //In which case it should perhaps be undefined
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }


    private class KernelServicesBuilderImpl implements KernelServicesBuilder, ModelTestBootOperationsBuilder.BootOperationParser {

        private final TestModelType type;
        private final ModelTestBootOperationsBuilder bootOperationBuilder = new ModelTestBootOperationsBuilder(testClass, this);
        private final TestParser testParser;
        private ProcessType processType;
        private RunningMode runningMode;
        private ModelInitializer modelInitializer;
        private boolean validateDescription;
        private boolean validateOperations = true;
        private XMLMapper xmlMapper = XMLMapper.Factory.create();
        private Map<ModelVersion, LegacyKernelServicesInitializerImpl> legacyControllerInitializers = new HashMap<ModelVersion, LegacyKernelServicesInitializerImpl>();


        public KernelServicesBuilderImpl(TestModelType type) {
            this.type = type;
            this.processType = type == TestModelType.HOST || type == TestModelType.DOMAIN ? ProcessType.HOST_CONTROLLER : ProcessType.STANDALONE_SERVER;
            runningMode = RunningMode.ADMIN_ONLY;
            testParser = TestParser.create(xmlMapper, type);
        }


        public KernelServicesBuilder validateDescription() {
            this.validateDescription = true;
            return this;
        }

        @Override
        public KernelServicesBuilder setXmlResource(String resource) throws IOException, XMLStreamException {
            bootOperationBuilder.setXmlResource(resource);
            return this;
        }

        @Override
        public KernelServicesBuilder setXml(String subsystemXml) throws XMLStreamException {
            bootOperationBuilder.setXml(subsystemXml);
            return this;
        }

        @Override
        public KernelServicesBuilder setBootOperations(List<ModelNode> bootOperations) {
            bootOperationBuilder.setBootOperations(bootOperations);
            return this;
        }

        @Override
        public KernelServicesBuilder setModelInitializer(ModelInitializer modelInitializer, ModelWriteSanitizer modelWriteSanitizer) {
            bootOperationBuilder.validateNotAlreadyBuilt();
            this.modelInitializer = modelInitializer;
            testParser.setModelWriteSanitizer(modelWriteSanitizer);
            return this;
        }


        public KernelServices build() throws Exception {
            bootOperationBuilder.validateNotAlreadyBuilt();
            List<ModelNode> bootOperations = bootOperationBuilder.build();
            RunningModeControl runningModeControl = type == TestModelType.HOST ? new HostRunningModeControl(runningMode, RestartMode.HC_ONLY) : new RunningModeControl(runningMode);
            ExtensionRegistry registry = new ExtensionRegistry(processType, runningModeControl);
            AbstractKernelServicesImpl kernelServices = AbstractKernelServicesImpl.create(processType, runningModeControl, validateOperations, bootOperations, testParser, null, type, modelInitializer, registry);
            CoreModelTestDelegate.this.kernelServices.add(kernelServices);

            if (validateDescription) {
                validateDescriptionProviders(type, kernelServices);
            }


            ModelTestUtils.validateModelDescriptions(PathAddress.EMPTY_ADDRESS, kernelServices.getRootRegistration());


            for (Map.Entry<ModelVersion, LegacyKernelServicesInitializerImpl> entry : legacyControllerInitializers.entrySet()) {
                LegacyKernelServicesInitializerImpl legacyInitializer = entry.getValue();

                List<ModelNode> transformedBootOperations = new ArrayList<ModelNode>();
                for (ModelNode op : bootOperations) {

                    ModelNode transformed = kernelServices.transformOperation(entry.getKey(), op).getTransformedOperation();
                    if (transformed != null) {
                        transformedBootOperations.add(transformed);
                    }
                }

                KernelServices legacyServices = legacyInitializer.install(transformedBootOperations);
                kernelServices.addLegacyKernelService(entry.getKey(), legacyServices);

            }


            return kernelServices;
        }
        @Override
        public List<ModelNode> parse(String xml) throws XMLStreamException {
            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            final List<ModelNode> operationList = new ArrayList<ModelNode>();
            xmlMapper.parseDocument(operationList, reader);
            return operationList;
        }

        @Override
        public LegacyKernelServicesInitializer createLegacyKernelServicesBuilder(ModelVersion modelVersion) {
            if (type != TestModelType.DOMAIN) {
                throw new IllegalStateException("Can only create legacy kernel services for DOMAIN.");
            }
            LegacyKernelServicesInitializerImpl legacyKernelServicesInitializerImpl = new LegacyKernelServicesInitializerImpl(modelVersion);
            legacyControllerInitializers.put(modelVersion, legacyKernelServicesInitializerImpl);
            return legacyKernelServicesInitializerImpl;
        }


        @Override
        public KernelServicesBuilder setDontValidateOperations() {
            validateOperations = true;
            return this;
        }
    }

    private class LegacyKernelServicesInitializerImpl implements LegacyKernelServicesInitializer {
        private final ChildFirstClassLoaderBuilder classLoaderBuilder = new ChildFirstClassLoaderBuilder();
        private final ModelVersion modelVersion;
        private final List<LegacyModelInitializerEntry> modelInitializerEntries = new ArrayList<LegacyModelInitializerEntry>();
        private boolean validateOperations = true;

        public LegacyKernelServicesInitializerImpl(ModelVersion modelVersion) {
            this.modelVersion = modelVersion;
        }

        private KernelServices install(List<ModelNode> bootOperations) throws Exception {
            classLoaderBuilder.addMavenResourceURL("org.jboss.as:jboss-as-core-model-test-framework:7.2.0.Alpha1-SNAPSHOT");
            classLoaderBuilder.addMavenResourceURL("org.jboss.as:jboss-as-model-test:7.2.0.Alpha1-SNAPSHOT");
            classLoaderBuilder.addParentFirstClassPattern("org.jboss.as.core.model.bridge.shared.*");
            ClassLoader legacyCl = classLoaderBuilder.build();



            ScopedKernelServicesBootstrap scopedBootstrap = new ScopedKernelServicesBootstrap(legacyCl);
            return scopedBootstrap.createKernelServices(bootOperations, validateOperations, modelVersion, modelInitializerEntries);
        }

        @Override
        public LegacyKernelServicesInitializer initializerCreateModelResource(PathAddress parentAddress, PathElement relativeResourceAddress, ModelNode model) {
            modelInitializerEntries.add(new LegacyModelInitializerEntry(parentAddress, relativeResourceAddress, model));
            return this;
        }

        @Override
        public LegacyKernelServicesInitializer setTestControllerVersion(TestControllerVersion version) throws MalformedURLException, DependencyCollectionException, DependencyResolutionException {
            classLoaderBuilder.addRecursiveMavenResourceURL(version.getLegacyControllerMavenGav());
            if (version.getTestControllerVersion() != null) {
                classLoaderBuilder.addMavenResourceURL("org.jboss.as:jboss-as-core-model-test-controller-" + version.getTestControllerVersion() + ":" + VERSION);
            }
            return this;
        }

        @Override
        public LegacyKernelServicesInitializer setDontValidateOperations() {
            validateOperations = false;
            return this;
        }
    }


}

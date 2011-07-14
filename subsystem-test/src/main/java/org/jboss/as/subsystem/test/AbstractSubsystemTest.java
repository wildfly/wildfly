package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Before;

/**
 * The base class for parsing tests which does the work of setting up the environment for parsing
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSubsystemTest {

    private final String TEST_NAMESPACE = "urn.org.jboss.test:1.0";

    private ExtensionParsingContextImpl parsingContext;

    private List<KernelServices> kernelServices = new ArrayList<KernelServices>();

    private final AtomicInteger counter = new AtomicInteger();

    private final String mainSubsystemName;
    private final Extension mainExtension;
    private TestParser testParser;
    private boolean addedExtraParsers;

    protected AbstractSubsystemTest(final String mainSubsystemName, final Extension mainExtension) {
        this.mainSubsystemName = mainSubsystemName;
        this.mainExtension = mainExtension;

    }

    @Before
    public void initializeParser() throws Exception {
        //Initialize the parser
        XMLMapper mapper = XMLMapper.Factory.create();
        testParser = new TestParser();
        mapper.registerRootElement(new QName(TEST_NAMESPACE, "test"), testParser);
        parsingContext = new ExtensionParsingContextImpl(mapper);
        mainExtension.initializeParsers(parsingContext);
        addedExtraParsers = false;
    }

    @After
    public void cleanup() throws Exception {
        for (KernelServices kernelServices : this.kernelServices) {
            try {
                kernelServices.shutdown();
            } catch (Exception e) {
            }
        }
        kernelServices.clear();
        parsingContext = null;
        testParser = null;
    }

    protected Extension getMainExtension() {
        return mainExtension;
    }


    /**
     * Parse the subsystem xml and create the operations that will be passed into the controller
     *
     * @param subsystemXml the subsystem xml to be parsed
     * @return the created operations
     */
    protected List<ModelNode> parse(String subsystemXml) throws XMLStreamException {
        return parse(null, subsystemXml);
    }

    /**
     * Parse the subsystem xml and create the operations that will be passed into the controller
     *
     * @param additionalParsers additional initialization that should be done to the parsers before initializing our extension. These parsers
     * will only be initialized the first time this method is called from within a test
     * @param subsystemXml the subsystem xml to be parsed
     * @return the created operations
     */
    protected List<ModelNode> parse(AdditionalParsers additionalParsers, String subsystemXml) throws XMLStreamException {
        if (additionalParsers != null && !addedExtraParsers) {
            additionalParsers.addParsers(parsingContext);
            addedExtraParsers = true;
        }

        String xml = "<test xmlns=\"" + TEST_NAMESPACE + "\">" +
                subsystemXml +
                "</test>";
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        final List<ModelNode> operationList = new ArrayList<ModelNode>();
        parsingContext.getMapper().parseDocument(operationList, reader);
        return operationList;
    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml
     *
     * @param subsystemXml the subsystem xml to be parsed
     * @return the kernel services allowing access to the controller and service container
     */
    protected KernelServices installInController(String subsystemXml) throws Exception {
        return installInController(null, subsystemXml);
    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param subsystemXml the subsystem xml to be parsed
     * @return the kernel services allowing access to the controller and service container
     */
    protected KernelServices installInController(AdditionalInitialization additionalInit, String subsystemXml) throws Exception {
        List<ModelNode> operations = parse(additionalInit, subsystemXml);
        KernelServices services = installInController(additionalInit, operations);
        return services;
    }

    /**
     * Create a new controller with the passed in operations
     * @param bootOperations the operations
     */
    protected KernelServices installInController(List<ModelNode> bootOperations) throws Exception {
        return installInController(null, bootOperations);
    }

    /**
     * Create a new controller with the passed in operations
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param bootOperations the operations
     */
    protected KernelServices installInController(AdditionalInitialization additionalInit, List<ModelNode> bootOperations) throws Exception {
        ControllerInitializer controllerInitializer = createControllerInitializer();
        if (additionalInit != null) {
            additionalInit.setupController(controllerInitializer);
        }

        //Initialize the controller
        ServiceContainer container = ServiceContainer.Factory.create("test" + counter.incrementAndGet());
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        List<ModelNode> extraOps = controllerInitializer.initializeBootOperations();
        List<ModelNode> allOps = new ArrayList<ModelNode>();
        if (extraOps != null) {
            allOps.addAll(extraOps);
        }
        allOps.addAll(bootOperations);
        StringConfigurationPersister persister = new StringConfigurationPersister(allOps, testParser);
        ModelControllerService svc = new ModelControllerService(mainExtension, controllerInitializer, additionalInit, processState, persister);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();

        if (additionalInit != null) {
            additionalInit.addExtraServices(target);
        }

        //sharedState = svc.state;
        svc.latch.await();
        ModelController controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();

        KernelServices kernelServices = new KernelServices(container, controller, persister);
        this.kernelServices.add(kernelServices);
        return kernelServices;
    }


    /**
     * Checks that the result was successful and gets the real result contents
     * @param result the result to check
     * @return the result contents
     */
    protected static ModelNode checkResultAndGetContents(ModelNode result) {
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.hasDefined(RESULT));
        return result.get(RESULT);
    }

    /**
     * Compares two models to make sure that they are the same
     * @param node1 the first model
     * @param node2 the second model
     * @throws AssertionFailedError if the models were not the same
     */
    protected void compare(ModelNode node1, ModelNode node2) {
        Assert.assertEquals(node1.getType(), node2.getType());
        if (node1.getType() == ModelType.OBJECT) {
            final Set<String> keys1 = node1.keys();
            final Set<String> keys2 = node2.keys();
            Assert.assertEquals(node1 + "\n" + node2, keys1.size(), keys2.size());

            for (String key : keys1) {
                final ModelNode child1 = node1.get(key);
                Assert.assertTrue("Missing: " + key + "\n" + node1 + "\n" + node2, node2.has(key));
                final ModelNode child2 = node2.get(key);
                if (child1.isDefined()) {
                    Assert.assertTrue(child1.toString(), child2.isDefined());
                    compare(child1, child2);
                } else {
                    Assert.assertFalse(child2.asString(), child2.isDefined());
                }
            }
        } else if (node1.getType() == ModelType.LIST) {
            List<ModelNode> list1 = node1.asList();
            List<ModelNode> list2 = node2.asList();
            Assert.assertEquals(list1 + "\n" + list2, list1.size(), list2.size());

            for (int i = 0; i < list1.size(); i++) {
                compare(list1.get(i), list2.get(i));
            }

        } else if (node1.getType() == ModelType.PROPERTY) {
            Property prop1 = node1.asProperty();
            Property prop2 = node2.asProperty();
            Assert.assertEquals(prop1 + "\n" + prop2, prop1.getName(), prop2.getName());
            compare(prop1.getValue(), prop2.getValue());

        } else {
            try {
                Assert.assertEquals("\n\"" + node1.asString() + "\"\n\"" + node2.asString() + "\"\n-----", node2.asString().trim(), node1.asString().trim());
            } catch (AssertionFailedError error) {
                throw error;
            }
        }
    }

    /**
     * Creates the controller initializer.
     * Override this method to do custom initialization.
     *
     * @return the created controller initializer
     */
    protected ControllerInitializer createControllerInitializer() {
        return new ControllerInitializer();
    }

    private final class ExtensionParsingContextImpl implements ExtensionParsingContext {
        private final XMLMapper mapper;

        public ExtensionParsingContextImpl(XMLMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void setSubsystemXmlMapping(final String namespaceUri, final XMLElementReader<List<ModelNode>> reader) {
            mapper.registerRootElement(new QName(namespaceUri, SUBSYSTEM), reader);
        }

        @Override
        public void setDeploymentXmlMapping(final String namespaceUri, final XMLElementReader<ModelNode> reader) {
        }

        private XMLMapper getMapper() {
            return mapper;
        }
    }



    private final class TestParser implements  XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelMarshallingContext> {

        private TestParser() {

        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, ModelMarshallingContext context) throws XMLStreamException {
            String defaultNamespace = writer.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
            try {
                ModelNode subsystem = context.getModelNode().get(SUBSYSTEM, mainSubsystemName);
                XMLElementWriter<SubsystemMarshallingContext> subsystemWriter = context.getSubsystemWriter(mainSubsystemName);
                if (subsystemWriter != null) {
                    subsystemWriter.writeContent(writer, new SubsystemMarshallingContext(subsystem, writer));
                }
            } finally {
                writer.setDefaultNamespace(defaultNamespace);
            }
            writer.writeEndDocument();
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

            ParseUtils.requireNoAttributes(reader);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.UNKNOWN) {
                    throw unexpectedElement(reader);
                }
                if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                    throw unexpectedElement(reader);
                }
                reader.handleAny(operations);
            }
        }
    }

    private static class ModelControllerService extends AbstractControllerService {

        final AtomicBoolean state = new AtomicBoolean(true);
        final CountDownLatch latch = new CountDownLatch(1);
        final StringConfigurationPersister persister;
        final AdditionalInitialization additionalInit;
        final ControllerInitializer controllerInitializer;
        final Extension mainExtension;

        ModelControllerService(final Extension mainExtension, final ControllerInitializer controllerInitializer, final AdditionalInitialization additionalPreStep, final ControlledProcessState processState, final StringConfigurationPersister persister) {
            super(OperationContext.Type.SERVER, persister, processState, DESC_PROVIDER, null);
            this.persister = persister;
            this.additionalInit = additionalPreStep;
            this.mainExtension = mainExtension;
            this.controllerInitializer = controllerInitializer;
        }

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            rootResource.getModel().get(SUBSYSTEM);
            rootRegistration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
            rootRegistration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

            controllerInitializer.initializeModel(rootResource, rootRegistration);

            ExtensionContext context = new ExtensionContextImpl(rootRegistration, null, persister);
            if (additionalInit != null) {
                additionalInit.initializeExtraSubystemsAndModel(context, rootResource, rootRegistration);
            }
            mainExtension.initialize(context);
        }

        @Override
        protected void boot(List<ModelNode> bootOperations) throws ConfigurationPersistenceException {
            super.boot(persister.bootOperations);
            latch.countDown();
        }

        @Override
        public void start(StartContext context) throws StartException {
            super.start(context);
        }
    }

    private static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode model = new ModelNode();
            model.get(DESCRIPTION).set("The test model controller");
            return model;
        }
    };

    static class StringConfigurationPersister extends AbstractConfigurationPersister {

        private final List<ModelNode> bootOperations;
        volatile String marshalled;

        public StringConfigurationPersister(List<ModelNode> bootOperations, XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(rootDeparser);
            this.bootOperations = bootOperations;
        }

        @Override
        public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses)
                throws ConfigurationPersistenceException {
            return new StringPersistenceResource(model, this);
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            return bootOperations;
        }

        private class StringPersistenceResource implements PersistenceResource {

            private byte[] bytes;
            private final AbstractConfigurationPersister persister;

            StringPersistenceResource(final ModelNode model, final AbstractConfigurationPersister persister) throws ConfigurationPersistenceException {
                this.persister = persister;
                ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 8);
                try {
                    try {
                        persister.marshallAsXml(model, output);
                    } finally {
                        try {
                            output.close();
                        } catch (Exception ignore) {
                        }
                        bytes = output.toByteArray();
                    }
                } catch (Exception e) {
                    throw new ConfigurationPersistenceException("Failed to marshal configuration", e);
                }
            }

            @Override
            public void commit() {
                StringConfigurationPersister.this.marshalled = new String(bytes);
            }

            @Override
            public void rollback() {
                marshalled = null;
            }
        }
    }
}

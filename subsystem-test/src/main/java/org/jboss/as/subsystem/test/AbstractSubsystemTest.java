package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.DeployerChainAddHandler;
import org.jboss.as.server.Services;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.repository.impl.ContentRepositoryImpl;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationFailure;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
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
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

/**
 * The base class for parsing tests which does the work of setting up the environment for parsing
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSubsystemTest {

    private static final ThreadLocal<Stack<String>> stack = new ThreadLocal<Stack<String>>();

    private final String TEST_NAMESPACE = "urn.org.jboss.test:1.0";

    private List<KernelServices> kernelServices = new ArrayList<KernelServices>();

    private final AtomicInteger counter = new AtomicInteger();

    protected final String mainSubsystemName;
    private final Extension mainExtension;
    /**
     * ExtensionRegistry we use just for registering parsers.
     * The ModelControllerService uses a separate registry. This is done this way to allow multiple ModelControllerService
     * instantiations in the same test without having to re-initialize the parsers.
     */
    private ExtensionRegistry extensionParsingRegistry;
    private TestParser testParser;
    private boolean addedExtraParsers;
    private XMLMapper xmlMapper;

    protected AbstractSubsystemTest(final String mainSubsystemName, final Extension mainExtension) {
        this.mainSubsystemName = mainSubsystemName;
        this.mainExtension = mainExtension;
    }

    public String getMainSubsystemName() {
        return mainSubsystemName;
    }

    @Before
    public void initializeParser() throws Exception {
        //Initialize the parser
        xmlMapper = XMLMapper.Factory.create();
        testParser = new TestParser();
        extensionParsingRegistry = new ExtensionRegistry(getProcessType(), new RunningModeControl(RunningMode.NORMAL));
        xmlMapper.registerRootElement(new QName(TEST_NAMESPACE, "test"), testParser);
        mainExtension.initializeParsers(extensionParsingRegistry.getExtensionParsingContext("Test", xmlMapper));
        addedExtraParsers = false;

        stack.set(new Stack<String>());
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
        xmlMapper = null;
        extensionParsingRegistry = null;
        testParser = null;
        stack.remove();
    }

    protected Extension getMainExtension() {
        return mainExtension;
    }

    /**
     * Read the classpath resource with the given name and return its contents as a string. Hook to
     * for reading in classpath resources for subsequent parsing.
     *
     * @param name the name of the resource
     * @return the contents of the resource as a string
     * @throws IOException
     */
    protected String readResource(final String name) throws IOException {

        URL configURL = getClass().getResource(name);
        org.junit.Assert.assertNotNull(name + " url is null", configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        StringWriter writer = new StringWriter();
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
        }
        return writer.toString();
    }


    /**
     * Parse the subsystem xml and create the operations that will be passed into the controller
     *
     * @param subsystemXml the subsystem xml to be parsed
     * @return the created operations
     * @throws XMLStreamException if there is a parsing problem
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
     * @throws XMLStreamException if there is a parsing problem
     */
    protected List<ModelNode> parse(AdditionalParsers additionalParsers, String subsystemXml) throws XMLStreamException {
        String xml = "<test xmlns=\"" + TEST_NAMESPACE + "\">" +
                subsystemXml +
                "</test>";
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        addAdditionalParsers(additionalParsers);
        final List<ModelNode> operationList = new ArrayList<ModelNode>();
        xmlMapper.parseDocument(operationList, reader);
        return operationList;
    }

    /**
     * Output the model to xml
     *
     * @param model the model to marshall
     * @return the xml
     */
    protected String outputModel(ModelNode model) throws Exception {

        StringConfigurationPersister persister = new StringConfigurationPersister(Collections.<ModelNode>emptyList(), testParser);

        ExtensionRegistry outputExtensionRegistry = new ExtensionRegistry(ProcessType.EMBEDDED_SERVER, new RunningModeControl(RunningMode.NORMAL));
        outputExtensionRegistry.setProfileResourceRegistration(MOCK_RESOURCE_REG);
        outputExtensionRegistry.setDeploymentsResourceRegistration(MOCK_RESOURCE_REG);
        outputExtensionRegistry.setWriterRegistry(persister);

        Extension extension = mainExtension.getClass().newInstance();
        extension.initialize(outputExtensionRegistry.getExtensionContext("Test"));

        ConfigurationPersister.PersistenceResource resource = persister.store(model, Collections.<PathAddress>emptySet());
        resource.commit();
        return persister.marshalled;
    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml.
     *
     * @param subsystemXml the subsystem xml to be parsed
     * @return the kernel services allowing access to the controller and service container
     */
    protected KernelServices installInController(String subsystemXml) throws Exception {
        return installInController(null, subsystemXml);
    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param subsystemXml the subsystem xml to be parsed
     * @return the kernel services allowing access to the controller and service container
     */
    protected KernelServices installInController(AdditionalInitialization additionalInit, String subsystemXml) throws Exception {
        if (additionalInit == null) {
            additionalInit = new AdditionalInitialization();
        }
        List<ModelNode> operations = parse(additionalInit, subsystemXml);
        KernelServices services = installInController(additionalInit, operations);
        return services;
    }

    /**
     * Create a new controller with the passed in operations.
     *
     * @param bootOperations the operations
     */
    protected KernelServices installInController(List<ModelNode> bootOperations) throws Exception {
        return installInController(null, bootOperations);
    }

    /**
     * Create a new controller with the passed in operations.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param bootOperations the operations
     */
    protected KernelServices installInController(AdditionalInitialization additionalInit, List<ModelNode> bootOperations) throws Exception {
        if (additionalInit == null) {
            additionalInit = new AdditionalInitialization();
        }
        ControllerInitializer controllerInitializer = additionalInit.createControllerInitializer();
        additionalInit.setupController(controllerInitializer);

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
        final ExtensionRegistry controllerExtensionRegistry = cloneExtensionRegistry();
        controllerExtensionRegistry.setWriterRegistry(persister);
        ModelControllerService svc = new ModelControllerService(mainExtension, controllerInitializer, additionalInit, controllerExtensionRegistry,
                processState, persister, additionalInit.isValidateOperations());
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.install();

        additionalInit.addExtraServices(target);

        //sharedState = svc.state;
        svc.latch.await();
        ModelController controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();

        KernelServices kernelServices = new KernelServices(container, controller, persister, new OperationValidator(svc.rootRegistration));
        this.kernelServices.add(kernelServices);
        if (svc.error != null) {
            throw svc.error;
        }

        validateDescriptionProviders(additionalInit, kernelServices);

        return kernelServices;
    }

    /**
     * Gets the ProcessType to use. Defaults to {@link ProcessType#EMBEDDED_SERVER}
     * @return the process type
     */
    protected ProcessType getProcessType() {
        return ProcessType.EMBEDDED_SERVER;
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
     * Checks that the subystem resources can be removed, i.e. that people have registered
     * working 'remove' operations for every 'add' level.
     *
     * @param kernelServices the kernel services used to access the controller
     */
    protected void assertRemoveSubsystemResources(KernelServices kernelServices) {
        assertRemoveSubsystemResources(kernelServices, null);
    }

    /**
     * Checks that the subystem resources can be removed, i.e. that people have registered
     * working 'remove' operations for every 'add' level.
     *
     * @param kernelServices the kernel services used to access the controller
     * @param ignoredChildAddresses child addresses that should not be removed, they are managed by one of the parent resources.
     * This set cannot contain the subsystem resource itself
     */
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {

        if (ignoredChildAddresses == null) {
            ignoredChildAddresses = Collections.<PathAddress>emptySet();
        } else {
            PathAddress subsystem = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, mainSubsystemName));
            Assert.assertFalse("Cannot exclude removal of subsystem itself", ignoredChildAddresses.contains(subsystem));
        }

        Resource rootResource = grabRootResource(kernelServices);

        List<PathAddress> addresses = new ArrayList<PathAddress>();
        PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, mainSubsystemName));
        Resource subsystemResource = rootResource.getChild(pathAddress.getLastElement());
        Assert.assertNotNull(subsystemResource);
        addresses.add(pathAddress);

        getAllChildAddressesForRemove(pathAddress, addresses, subsystemResource);

        ModelNode composite = new ModelNode();
        composite.get(OP).set(CompositeOperationHandler.NAME);
        composite.get(OP_ADDR).setEmptyList();
        composite.get("rollback-on-runtime-failure").set(true);


        for (ListIterator<PathAddress> iterator = addresses.listIterator(addresses.size()) ; iterator.hasPrevious() ; ) {
            PathAddress cur = iterator.previous();
            if (!ignoredChildAddresses.contains(cur)) {
                ModelNode remove = new ModelNode();
                remove.get(OP).set(REMOVE);
                remove.get(OP_ADDR).set(cur.toModelNode());
                composite.get("steps").add(remove);
            }
        }


        ModelNode result = kernelServices.executeOperation(composite);

        ModelNode model = kernelServices.readWholeModel().get(SUBSYSTEM, mainSubsystemName);
        Assert.assertFalse("Subsystem resources were not removed " + model, model.isDefined());
    }

    private void getAllChildAddressesForRemove(PathAddress address, List<PathAddress> addresses, Resource resource) {
        List<PathElement> childElements = new ArrayList<PathElement>();
        for (String type : resource.getChildTypes()) {
            for (String childName : resource.getChildrenNames(type)) {
                PathElement element = PathElement.pathElement(type, childName);
                childElements.add(element);
            }
        }

        for (PathElement childElement : childElements) {
            addresses.add(address.append(childElement));
        }

        for (PathElement childElement : childElements) {
            getAllChildAddressesForRemove(address.append(childElement), addresses, resource.getChild(childElement));
        }
    }

    /**
     * Grabs the current root resource
     *
     * @param kernelServices the kernel services used to access the controller
     */
    protected Resource grabRootResource(KernelServices kernelServices) {
        ModelNode op = new ModelNode();
        op.get(OP).set(RootResourceGrabber.NAME);
        op.get(OP_ADDR).setEmptyList();
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        Resource rootResource = RootResourceGrabber.INSTANCE.resource;
        Assert.assertNotNull(rootResource);
        return rootResource;
    }

    /**
     * Compares two models to make sure that they are the same
     * @param node1 the first model
     * @param node2 the second model
     * @throws AssertionFailedError if the models were not the same
     */
    protected void compare(ModelNode node1, ModelNode node2) {
        Assert.assertEquals(getCompareStackAsString() + " types", node1.getType(), node2.getType());
        if (node1.getType() == ModelType.OBJECT) {
            final Set<String> keys1 = node1.keys();
            final Set<String> keys2 = node2.keys();
            Assert.assertEquals(node1 + "\n" + node2, keys1.size(), keys2.size());

            for (String key : keys1) {
                final ModelNode child1 = node1.get(key);
                Assert.assertTrue("Missing: " + key + "\n" + node1 + "\n" + node2, node2.has(key));
                final ModelNode child2 = node2.get(key);
                if (child1.isDefined()) {
                    Assert.assertTrue("key="+ key + "\n with child1 \n" + child1.toString() + "\n has child2 not defined\n node2 is:\n" + node2.toString(), child2.isDefined());
                    stack.get().push(key + "/");
                    compare(child1, child2);
                    stack.get().pop();
                } else {
                    Assert.assertFalse(child2.asString(), child2.isDefined());
                }
            }
        } else if (node1.getType() == ModelType.LIST) {
            List<ModelNode> list1 = node1.asList();
            List<ModelNode> list2 = node2.asList();
            Assert.assertEquals(list1 + "\n" + list2, list1.size(), list2.size());

            for (int i = 0; i < list1.size(); i++) {
                stack.get().push(i + "/");
                compare(list1.get(i), list2.get(i));
                stack.get().pop();
            }

        } else if (node1.getType() == ModelType.PROPERTY) {
            Property prop1 = node1.asProperty();
            Property prop2 = node2.asProperty();
            Assert.assertEquals(prop1 + "\n" + prop2, prop1.getName(), prop2.getName());
            stack.get().push(prop1.getName() + "/");
            compare(prop1.getValue(), prop2.getValue());
            stack.get().pop();

        } else {
            try {
                Assert.assertEquals(getCompareStackAsString() +
                        "\n\"" + node1.asString() + "\"\n\"" + node2.asString() + "\"\n-----", node2.asString().trim(), node1.asString().trim());
            } catch (AssertionFailedError error) {
                throw error;
            }
        }
    }

    /**
     * Normalize and pretty-print XML so that it can be compared using string
     * compare. The following code does the following: - Removes comments -
     * Makes sure attributes are ordered consistently - Trims every element -
     * Pretty print the document
     *
     * @param xml
     *            The XML to be normalized
     * @return The equivalent XML, but now normalized
     */
    protected String normalizeXML(String xml) throws Exception {
        // Remove all white space adjoining tags ("trim all elements")
        xml = xml.replaceAll("\\s*<", "<");
        xml = xml.replaceAll(">\\s*", ">");

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS domLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
        LSParser lsParser = domLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        LSInput input = domLS.createLSInput();
        input.setStringData(xml);
        Document document = lsParser.parse(input);

        LSSerializer lsSerializer = domLS.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("comments", Boolean.FALSE);
        lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        return lsSerializer.writeToString(document);
    }

    private static String getCompareStackAsString() {
         String result = "";
         for (String element : stack.get()) {
            result += element;
         }
        return result;
    }

    private void addAdditionalParsers(AdditionalParsers additionalParsers) {
        if (additionalParsers != null && !addedExtraParsers) {
            additionalParsers.addParsers(extensionParsingRegistry, xmlMapper);
            addedExtraParsers = true;
        }
    }

    private ExtensionRegistry cloneExtensionRegistry() {
        final ExtensionRegistry clone = new ExtensionRegistry(extensionParsingRegistry.getProcessType(), new RunningModeControl(RunningMode.NORMAL));
        for (String extension : extensionParsingRegistry.getExtensionModuleNames()) {
            ExtensionParsingContext epc = clone.getExtensionParsingContext(extension, null);
            for (Map.Entry<String, ExtensionRegistry.SubsystemInformation> entry : extensionParsingRegistry.getAvailableSubsystems(extension).entrySet()) {
                for (String namespace : entry.getValue().getXMLNamespaces()) {
                    epc.setSubsystemXmlMapping(entry.getKey(), namespace, null);
                }
            }
            for (String namespace : extensionParsingRegistry.getUnnamedNamespaces(extension)) {
                epc.setSubsystemXmlMapping(namespace, null);
            }
        }

        return clone;
    }

    private void validateDescriptionProviders(AdditionalInitialization additionalInit, KernelServices kernelServices) {
        ValidationConfiguration arbitraryDescriptors = additionalInit.getModelValidationConfiguration();
        ModelNode address = new ModelNode();
        address.setEmptyList();
        address.add("subsystem", mainSubsystemName);

        ModelNode op = new ModelNode();
        op.get(OP).set("read-resource-description");
        //op.get(OP_ADDR).setEmptyList();
        op.get(OP_ADDR).set(address);
        op.get("recursive").set(true);
        op.get("inherited").set(false);
        op.get("operations").set(true);
        ModelNode result = kernelServices.executeOperation(op);
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).asString());
        }
        ModelNode model = result.get(RESULT);

        ModelDescriptionValidator validator = new ModelDescriptionValidator(address, model, arbitraryDescriptors);
        List<ValidationFailure> validationMessages = validator.validateResource();
        if (validationMessages.size() > 0) {
            final StringBuilder builder = new StringBuilder("VALIDATION ERRORS IN MODEL:");
            for (ValidationFailure failure :validationMessages) {
                builder.append(failure);
                builder.append("\n");

            }
            if (arbitraryDescriptors != null) {
                Assert.fail("Failed due to validation errors in the model. Please fix :-) " + builder.toString());
            }
        }
    }

    /**
     * Validate the marshalled xml without adjusting the namespaces for the original and marshalled xml.
     * @param configId the id of the xml configuration
     * @param original the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     *
     * @throws Exception
     */
    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {
        compareXml(configId, original, marshalled, false);
    }

    /**
     * Validate the marshalled xml without adjusting the namespaces for the original and marshalled xml.
     * @param configId TODO
     * @param original the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     * @param ignoreNamespace if {@code true} the subsystem's namespace is ignored, otherwise it is taken into account when comparing the normalized xml.
     *
     * @throws Exception
     */
    protected void compareXml(String configId, final String original, final String marshalled, final boolean ignoreNamespace) throws Exception {
        final String xmlOriginal;
        final String xmlMarshalled;
        if (ignoreNamespace) {
            xmlOriginal = removeNamespace(original);
            xmlMarshalled = removeNamespace(marshalled);
        } else {
            xmlOriginal = original;
            xmlMarshalled = marshalled;
        }


        Assert.assertEquals(normalizeXML(xmlOriginal), normalizeXML(xmlMarshalled));
    }

    private String removeNamespace(String xml) {
        return xml.replaceFirst(" xmlns=\".*\"", "");
    }

    private final class TestParser implements  XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelMarshallingContext> {

        private TestParser() {

        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, ModelMarshallingContext context) throws XMLStreamException {
            String defaultNamespace = writer.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
            try {
                ModelNode subsystem = context.getModelNode().get(SUBSYSTEM, mainSubsystemName);
                if (subsystem.isDefined()) {
                    //We might have been removed
                    XMLElementWriter<SubsystemMarshallingContext> subsystemWriter = context.getSubsystemWriter(mainSubsystemName);
                    if (subsystemWriter != null) {
                        subsystemWriter.writeContent(writer, new SubsystemMarshallingContext(subsystem, writer));
                    }
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

        final CountDownLatch latch = new CountDownLatch(1);
        final StringConfigurationPersister persister;
        final AdditionalInitialization additionalInit;
        final ControllerInitializer controllerInitializer;
        final ExtensionRegistry extensionRegistry;
        final Extension mainExtension;
        final boolean validateOps;
        volatile ManagementResourceRegistration rootRegistration;
        volatile Exception error;

        ModelControllerService(final Extension mainExtension, final ControllerInitializer controllerInitializer,
                               final AdditionalInitialization additionalPreStep, final ExtensionRegistry extensionRegistry,
                               final ControlledProcessState processState, final StringConfigurationPersister persister, boolean validateOps) {
            super(additionalPreStep.getProcessType(), new RunningModeControl(additionalPreStep.getRunningMode()), persister,
                    processState, DESC_PROVIDER, null, ExpressionResolver.DEFAULT);
            this.persister = persister;
            this.additionalInit = additionalPreStep;
            this.extensionRegistry = extensionRegistry;
            this.mainExtension = mainExtension;
            this.controllerInitializer = controllerInitializer;
            this.validateOps = validateOps;
        }

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            this.rootRegistration = rootRegistration;
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
            rootRegistration.registerOperationHandler(CompositeOperationHandler.NAME, CompositeOperationHandler.INSTANCE, CompositeOperationHandler.INSTANCE, false, EntryType.PRIVATE);

            //Handler to be able to get hold of the root resource
            rootRegistration.registerOperationHandler(RootResourceGrabber.NAME, RootResourceGrabber.INSTANCE, RootResourceGrabber.INSTANCE, false);

            ManagementResourceRegistration deployments = rootRegistration.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);

            //Hack to be able to access the registry for the jmx facade
            rootRegistration.registerOperationHandler(RootResourceHack.NAME, RootResourceHack.INSTANCE, RootResourceHack.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

            extensionRegistry.setProfileResourceRegistration(rootRegistration);
            extensionRegistry.setDeploymentsResourceRegistration(deployments);

            controllerInitializer.initializeModel(rootResource, rootRegistration);

            additionalInit.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration);
            mainExtension.initialize(extensionRegistry.getExtensionContext("Test"));
        }

        @Override
        protected void boot(List<ModelNode> bootOperations) throws ConfigurationPersistenceException {
            try {
                if (validateOps) {
                    new OperationValidator(rootRegistration).validateOperations(bootOperations);
                }
                super.boot(persister.bootOperations);
            } catch (Exception e) {
                error = e;
            } catch (Throwable t) {
                error = new Exception(t);
            } finally {
                DeployerChainAddHandler.INSTANCE.clearDeployerMap();
                latch.countDown();
            }
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

    private final ManagementResourceRegistration MOCK_RESOURCE_REG = new ManagementResourceRegistration() {

        @Override
        public boolean isRuntimeOnly() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public OperationEntry getOperationEntry(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public OperationStepHandler getOperationHandler(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public DescriptionProvider getOperationDescription(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public Set<Flag> getOperationFlags(PathAddress address, String operationName) {
            return null;
        }

        @Override
        public Set<String> getAttributeNames(PathAddress address) {
            return null;
        }

        @Override
        public AttributeAccess getAttributeAccess(PathAddress address, String attributeName) {
            return null;
        }

        @Override
        public Set<String> getChildNames(PathAddress address) {
            return null;
        }

        @Override
        public Set<PathElement> getChildAddresses(PathAddress address) {
            return null;
        }

        @Override
        public DescriptionProvider getModelDescription(PathAddress address) {
            return null;
        }

        @Override
        public Map<String, OperationEntry> getOperationDescriptions(PathAddress address, boolean inherited) {
            return null;
        }

        @Override
        public ProxyController getProxyController(PathAddress address) {
            return null;
        }

        @Override
        public Set<ProxyController> getProxyControllers(PathAddress address) {
            return null;
        }

        @Override
        public ManagementResourceRegistration getOverrideModel(String name) {
            return null;
        }

        @Override
        public ManagementResourceRegistration getSubModel(PathAddress address) {
            return null;
        }

        @Override
        public ManagementResourceRegistration registerSubModel(PathElement address, DescriptionProvider descriptionProvider) {
            return MOCK_RESOURCE_REG;
        }

        @Override
        public ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition) {
            return MOCK_RESOURCE_REG;
        }

        @Override
        public void unregisterSubModel(PathElement address) {
        }

        @Override
        public boolean isAllowsOverride() {
            return true;
        }

        @Override
        public void setRuntimeOnly(boolean runtimeOnly) {
        }

        @Override
        public ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
            return MOCK_RESOURCE_REG;
        }

        @Override
        public void unregisterOverrideModel(String name) {
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler,
                DescriptionProvider descriptionProvider) {
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler,
                DescriptionProvider descriptionProvider, EnumSet<Flag> flags) {
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler,
                DescriptionProvider descriptionProvider, boolean inherited) {
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler,
                DescriptionProvider descriptionProvider, boolean inherited, EntryType entryType) {
        }

        @Override
        public void registerOperationHandler(String operationName, OperationStepHandler handler,
                DescriptionProvider descriptionProvider, boolean inherited, EntryType entryType, EnumSet<Flag> flags) {
        }

        @Override
        public void unregisterOperationHandler(String operationName) {

        }

        @Override
        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler,
                OperationStepHandler writeHandler, Storage storage) {
        }

        @Override
        public void registerReadWriteAttribute(String attributeName, OperationStepHandler readHandler, OperationStepHandler writeHandler, EnumSet<AttributeAccess.Flag> flags) {
        }

        @Override
        public void registerReadWriteAttribute(AttributeDefinition definition, OperationStepHandler readHandler, OperationStepHandler writeHandler) {
        }

        @Override
        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, Storage storage) {
        }

        @Override
        public void registerReadOnlyAttribute(String attributeName, OperationStepHandler readHandler, EnumSet<AttributeAccess.Flag> flags) {
        }

        @Override
        public void registerReadOnlyAttribute(AttributeDefinition definition, OperationStepHandler readHandler) {
        }

        @Override
        public void registerMetric(String attributeName, OperationStepHandler metricHandler) {
        }

        @Override
        public void registerMetric(AttributeDefinition definition, OperationStepHandler metricHandler) {
        }

        @Override
        public void registerMetric(String attributeName, OperationStepHandler metricHandler, EnumSet<AttributeAccess.Flag> flags) {
        }

        @Override
        public void unregisterAttribute(String attributeName) {
        }

        @Override
        public void registerProxyController(PathElement address, ProxyController proxyController) {
        }

        @Override
        public void unregisterProxyController(PathElement address) {
        }

    };

    private static class TestContentRepository extends ContentRepositoryImpl {

        private TestContentRepository(File repoRoot) {
            // FIXME TestContentRepository constructor
            super(repoRoot);
        }

        public static TestContentRepository createTestContentRepository() {
            File file = new File("target/content-repository");
            if (file.exists()) {
                cleanFiles(file);
            }
            file.mkdirs();
            return new TestContentRepository(file);
        }

        private static void cleanFiles(File file) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    File current = new File(file, name);
                    if (current.isDirectory()) {
                        cleanFiles(current);
                    }
                }
            }
            file.delete();
        }
    }

    private static class RootResourceGrabber implements OperationStepHandler, DescriptionProvider {
        static String NAME = "grab-root-resource";
        static RootResourceGrabber INSTANCE = new RootResourceGrabber();
        volatile Resource resource;
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            resource = context.getRootResource();
            context.getResult().setEmptyObject();
            context.completeStep();
        }
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            node.get(OPERATION_NAME).set(NAME);
            node.get(DESCRIPTION).set("Grabs the root resource");
            node.get(REQUEST_PROPERTIES).setEmptyObject();
            node.get(REPLY_PROPERTIES).setEmptyObject();
            return node;
        }


    }
}

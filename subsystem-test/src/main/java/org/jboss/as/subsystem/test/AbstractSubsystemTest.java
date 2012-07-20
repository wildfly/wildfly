package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.SubsystemDescriptionDump;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationFailure;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.xnio.IoUtils;

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
        testParser = new TestParser(mainSubsystemName);
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
     * for reading in classpath resources for subsequent parsing. The resource is loaded using similar
     * semantics to {@link Class#getResource(String)}
     *
     * @param name the name of the resource
     * @return the contents of the resource as a string
     * @throws IOException
     */
    protected String readResource(final String name) throws IOException {

        URL configURL = getClass().getResource(name);
        Assert.assertNotNull(name + " url is null", configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        StringWriter writer = new StringWriter();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
        } finally {
            reader.close();
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

        // Use ProcessType.HOST_CONTROLLER for this ExtensionRegistry so we don't need to provide
        // a PathManager via the ExtensionContext. All we need the Extension to do here is register the xml writers
        ExtensionRegistry outputExtensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, new RunningModeControl(RunningMode.NORMAL));
        outputExtensionRegistry.setSubsystemParentResourceRegistrations(MOCK_RESOURCE_REG, MOCK_RESOURCE_REG);
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
     * @deprecated Use {@link #createKernelServicesBuilder()} instead
     */
    @Deprecated
    protected KernelServices installInController(String subsystemXml) throws Exception {
        return createKernelServicesBuilder(null)
                .setSubsystemXml(subsystemXml)
                .build();
    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param subsystemXml the subsystem xml to be parsed
     * @deprecated Use {@link #createKernelServicesBuilder()} instead
     */
    @Deprecated
    protected KernelServices installInController(AdditionalInitialization additionalInit, String subsystemXml) throws Exception {
        return createKernelServicesBuilder(additionalInit)
                .setSubsystemXml(subsystemXml)
                .build();
    }

    /**
     * Create a new controller with the passed in operations.
     *
     * @param bootOperations the operations
     * @deprecated Use {@link #createKernelServicesBuilder()} instead
     */
    @Deprecated
    protected KernelServices installInController(List<ModelNode> bootOperations) throws Exception {
        return createKernelServicesBuilder(null)
                .setBootOperations(bootOperations)
                .build();
    }

    /**
     * Create a new controller with the passed in operations.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param bootOperations the operations
     * @deprecated Use {@link #createKernelServicesBuilder()} instead
     */
    @Deprecated
    protected KernelServices installInController(AdditionalInitialization additionalInit, List<ModelNode> bootOperations) throws Exception {
        return createKernelServicesBuilder(additionalInit)
                .setBootOperations(bootOperations)
                .build();
    }

   /**
    * Creates a new kernel services builder used to create a new controller containing the subsystem being tested
    *
    * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
    */
    protected KernelServicesBuilder createKernelServicesBuilder(AdditionalInitialization additionalInit) {
        return new KernelServicesBuilderImpl(additionalInit);
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
     * Dumps the target subsystem resource description to DMR format, needed by TransformerRegistry for non-standard subsystems
     *
     * @param kernelServices the kernel services for the started controller
     * @param modelVersion the target subsystem model version
     * @deprecated this might no longer be needed following refactoring of TransformerRegistry
     */
    @Deprecated
    protected void generateLegacySubsystemResourceRegistrationDmr(KernelServices kernelServices, ModelVersion modelVersion) throws IOException {
        KernelServices legacy = kernelServices.getLegacyServices(modelVersion);

        //Generate the org.jboss.as.controller.transform.subsystem-version.dmr file - just use the format used by TransformerRegistry for now
        PathAddress pathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, mainSubsystemName));
        ModelNode desc = SubsystemDescriptionDump.readFullModelDescription(pathAddress, legacy.getRootRegistration().getSubModel(pathAddress));
        File file = new File("target/classes").getAbsoluteFile();
        Assert.assertTrue(file.exists());
        for (String part : TransformerRegistry.class.getPackage().getName().split("\\.")) {
            file = new File(file, part);
            if (!file.exists()) {
                file.mkdir();
            }
        }
        PrintWriter pw = new PrintWriter(new File(file, mainSubsystemName + "-" + modelVersion.getMajor() + "." + modelVersion.getMinor() + ".dmr"));
        try {
            desc.writeString(pw, false);
        } finally {
            IoUtils.safeClose(pw);
        }
    }

    /**
     * Checks that the subsystem can be transformed into the expected target DMR
     *
     * @param kernelServices the kernel services for the started controller
     * @param modelVersion the target subsystem model version
     * @throws IOException if
     */
    protected void checkSubsystemTransformer(KernelServices kernelServices, ModelVersion modelVersion) throws IOException {
        KernelServices legacy = kernelServices.getLegacyServices(modelVersion);
        ModelNode legacyModel = legacy.readWholeModel();
        legacyModel = legacyModel.require(SUBSYSTEM);
        legacyModel = legacyModel.require(mainSubsystemName);


        ModelNode transformed = kernelServices.readTransformedModel(modelVersion).get(SUBSYSTEM, mainSubsystemName);
        compare(legacyModel, transformed, true);
    }

    /**
     * Checks that the subsystem can be transformed into the expected target DMR
     *
     * @param kernelServices the kernel services for the started controller
     * @param legacyModel the dmr model for the target subsystem version
     * @param modelVersion the model version
     */
    protected void checkSubsystemTransformer(KernelServices kernelServices, final ModelNode legacyModel, ModelVersion modelVersion) {
        final ModelNode result = kernelServices.readTransformedModel(modelVersion).get(SUBSYSTEM, mainSubsystemName);
        compare(legacyModel, result, true);
    }


    /**
     * Compares two models to make sure that they are the same
     * @param node1 the first model
     * @param node2 the second model
     * @throws AssertionFailedError if the models were not the same
     */
    protected void compare(ModelNode node1, ModelNode node2) {
        compare(node1, node2, false);
    }
    /**
     * Compares two models to make sure that they are the same
     * @param node1 the first model
     * @param node2 the second model
     * @param ignoreUndefined {@code true} if keys containing undefined nodes should be ignored
     * @throws AssertionFailedError if the models were not the same
     */
    protected void compare(ModelNode node1, ModelNode node2, boolean ignoreUndefined) {
        Assert.assertEquals(getCompareStackAsString() + " types", node1.getType(), node2.getType());
        if (node1.getType() == ModelType.OBJECT) {
            ModelNode model1 = ignoreUndefined ? trimUndefinedChildren(node1) : node1;
            ModelNode model2 = ignoreUndefined ? trimUndefinedChildren(node2) : node2;
            final Set<String> keys1 = model1.keys();
            final Set<String> keys2 = model2.keys();

            Assert.assertEquals(node1 + "\n" + node2, keys1.size(), keys2.size());
            Assert.assertTrue(keys1.containsAll(keys2));

            for (String key : keys1) {
                final ModelNode child1 = model1.get(key);
                Assert.assertTrue("Missing: " + key + "\n" + node1 + "\n" + node2, model2.has(key));
                final ModelNode child2 = model2.get(key);

                if (child1.isDefined()) {
                    if (!ignoreUndefined) {
                        Assert.assertTrue("key="+ key + "\n with child1 \n" + child1.toString() + "\n has child2 not defined\n node2 is:\n" + node2.toString(), child2.isDefined());
                    }
                    stack.get().push(key + "/");
                    compare(child1, child2, ignoreUndefined);
                    stack.get().pop();
                } else if (!ignoreUndefined){
                    Assert.assertFalse(child2.asString(), child2.isDefined());
                }
            }
        } else if (node1.getType() == ModelType.LIST) {
            List<ModelNode> list1 = node1.asList();
            List<ModelNode> list2 = node2.asList();
            Assert.assertEquals(list1 + "\n" + list2, list1.size(), list2.size());

            for (int i = 0; i < list1.size(); i++) {
                stack.get().push(i + "/");
                compare(list1.get(i), list2.get(i), ignoreUndefined);
                stack.get().pop();
            }

        } else if (node1.getType() == ModelType.PROPERTY) {
            Property prop1 = node1.asProperty();
            Property prop2 = node2.asProperty();
            Assert.assertEquals(prop1 + "\n" + prop2, prop1.getName(), prop2.getName());
            stack.get().push(prop1.getName() + "/");
            compare(prop1.getValue(), prop2.getValue(), ignoreUndefined);
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

    private ModelNode trimUndefinedChildren(ModelNode model) {
        ModelNode copy = model.clone();
        for (String key : new HashSet<String>(copy.keys())) {
            if (!copy.hasDefined(key)) {
                copy.remove(key);
            }
        }
        return copy;
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
            for (Map.Entry<String, SubsystemInformation> entry : extensionParsingRegistry.getAvailableSubsystems(extension).entrySet()) {
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

    public static void validateModelDescriptions(PathAddress address, ManagementResourceRegistration reg) {
        ModelNode attributes = reg.getModelDescription(PathAddress.EMPTY_ADDRESS).getModelDescription(Locale.getDefault()).get(ATTRIBUTES);
        Set<String> regAttributeNames = reg.getAttributeNames(PathAddress.EMPTY_ADDRESS);
        Set<String> attributeNames = new HashSet<String>();
        if (attributes.isDefined()) {
            if (attributes.asList().size() != regAttributeNames.size()) {
                for (Property p : attributes.asPropertyList()) {
                    attributeNames.add(p.getName());
                }
                if (regAttributeNames.size() > attributeNames.size()) {
                    regAttributeNames.removeAll(attributeNames);
                    Assert.fail("More attributes defined on resource registration than in description, missing: " + regAttributeNames + " for " + address);
                } else if (regAttributeNames.size() < attributeNames.size()) {
                    attributeNames.removeAll(regAttributeNames);
                    Assert.fail("More attributes defined in description than on resource registration, missing: " + attributeNames + " for " + address);
                }
            }
            if (!attributeNames.containsAll(regAttributeNames)) {
                for (Property p : attributes.asPropertyList()) {
                    attributeNames.add(p.getName());
                }
                Set<String> missDesc = new HashSet<String>(attributeNames);
                missDesc.removeAll(regAttributeNames);

                Set<String> missReg = new HashSet<String>(regAttributeNames);
                missReg.removeAll(attributeNames);

                if (!missReg.isEmpty()) {
                    Assert.fail("There are different attributes defined on resource registration than in description, registered only on Resource Reg: " + missReg + " for " + address);
                }
                if (!missDesc.isEmpty()) {
                    Assert.fail("There are different attributes defined on resource registration than in description, registered only int description: " + missDesc + " for " + address);
                }
            }
        }
        for (PathElement pe : reg.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            ManagementResourceRegistration sub = reg.getSubModel(PathAddress.pathAddress(pe));
            validateModelDescriptions(address.append(pe), sub);
        }
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

    static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode model = new ModelNode();
            model.get(DESCRIPTION).set("The test model controller");
            return model;
        }
    };

    private class KernelServicesBuilderImpl implements KernelServicesBuilder {
        private final AdditionalInitialization additionalInit;
        private List<ModelNode> bootOperations = Collections.emptyList();
        private String subsystemXml;
        private String subsystemXmlResource;
        private boolean built;
        private Map<ModelVersion, LegacyKernelServiceInitializerImpl> legacyControllerInitializers = new HashMap<ModelVersion, AbstractSubsystemTest.LegacyKernelServiceInitializerImpl>();

        public KernelServicesBuilderImpl(AdditionalInitialization additionalInit) {
            this.additionalInit = additionalInit == null ? new AdditionalInitialization() : additionalInit;
        }

        @Override
        public KernelServicesBuilder setSubsystemXmlResource(String resource) throws IOException, XMLStreamException {
            validateNotAlreadyBuilt();
            validateSubsystemConfig();
            this.subsystemXmlResource = resource;
            internalSetSubsystemXml(readResource(resource));
            return this;
        }

        @Override
        public KernelServicesBuilder setSubsystemXml(String subsystemXml) throws XMLStreamException {
            validateNotAlreadyBuilt();
            validateSubsystemConfig();
            this.subsystemXml = subsystemXml;
            internalParseSubsystemXml(subsystemXml);
            return this;
        }

        @Override
        public KernelServicesBuilder setBootOperations(List<ModelNode> bootOperations) {
            validateNotAlreadyBuilt();
            validateSubsystemConfig();
            this.bootOperations = bootOperations;
            return this;
        }

        public LegacyKernelServicesInitializer createLegacyKernelServicesBuilder(AdditionalInitialization additionalInit, ModelVersion modelVersion) {
            validateNotAlreadyBuilt();
            if (legacyControllerInitializers.containsKey(modelVersion)) {
                throw new IllegalArgumentException("There is already a legacy controller for " + modelVersion);
            }
            if (additionalInit != null) {
                if (additionalInit.getRunningMode() != RunningMode.ADMIN_ONLY) {
                    throw new IllegalArgumentException("The additional initialization must have a running mode of ADMIN_ONLY, it was " + additionalInit.getRunningMode());
                }
            }

            LegacyKernelServiceInitializerImpl initializer = new LegacyKernelServiceInitializerImpl(additionalInit, modelVersion);
            legacyControllerInitializers.put(modelVersion, initializer);
            return initializer;
        }

        public KernelServices build() throws Exception {
            validateNotAlreadyBuilt();
            built = true;
            KernelServices kernelServices = KernelServices.create(mainSubsystemName, additionalInit, cloneExtensionRegistry(), bootOperations, testParser, mainExtension, null);
            AbstractSubsystemTest.this.kernelServices.add(kernelServices);

            validateDescriptionProviders(additionalInit, kernelServices);
            ManagementResourceRegistration subsystemReg =  kernelServices.getRootRegistration().getSubModel(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM,mainSubsystemName)));
            validateModelDescriptions(PathAddress.EMPTY_ADDRESS, subsystemReg);

            for (Map.Entry<ModelVersion, LegacyKernelServiceInitializerImpl> entry : legacyControllerInitializers.entrySet()) {
                LegacyKernelServiceInitializerImpl legacyInitializer = entry.getValue();

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

        private void internalSetSubsystemXml(String subsystemXml) throws XMLStreamException {
            this.subsystemXml = subsystemXml;
            this.internalParseSubsystemXml(subsystemXml);
        }

        private void internalParseSubsystemXml(String subsystemXml) throws XMLStreamException {
            bootOperations = parse(additionalInit, subsystemXml);
        }

        private void validateSubsystemConfig() {
            if (subsystemXmlResource != null) {
                throw new IllegalArgumentException("Xml resource is already set");
            }
            if (subsystemXml != null) {
                throw new IllegalArgumentException("Xml string is already set");
            }
            if (bootOperations != Collections.EMPTY_LIST) {
                throw new IllegalArgumentException("Boot operations are already set");
            }
        }

        private void validateNotAlreadyBuilt() {
            if (built) {
                throw new IllegalStateException("Already built");
            }
        }
    }

    private class LegacyKernelServiceInitializerImpl implements LegacyKernelServicesInitializer {

        private final AdditionalInitialization additionalInit;
        private String extensionClassName;
        private ModelVersion modelVersion;
        private List<URL> classloaderURLs = new ArrayList<URL>();
        private List<Pattern> parentFirst = new ArrayList<Pattern>();
        private List<Pattern> childFirst = new ArrayList<Pattern>();

        public LegacyKernelServiceInitializerImpl(AdditionalInitialization additionalInit, ModelVersion modelVersion) {
            this.additionalInit = additionalInit == null ? AdditionalInitialization.MANAGEMENT : additionalInit;
            this.modelVersion = modelVersion;
        }

        @Override
        public LegacyKernelServicesInitializer setExtensionClassName(String extensionClassName) {
            this.extensionClassName = extensionClassName;
            return this;
        }


        @Override
        public LegacyKernelServicesInitializer addURL(URL url) {
            classloaderURLs.add(url);
            return this;
        }

        @Override
        public LegacyKernelServicesInitializer addSimpleResourceURL(String resource) throws MalformedURLException {
            classloaderURLs.add(ChildFirstClassLoader.createSimpleResourceURL(resource));
            return this;
        }

        @Override
        public LegacyKernelServicesInitializer addMavenResourceURL(String artifactGav) throws MalformedURLException {
            classloaderURLs.add(ChildFirstClassLoader.createMavenGavURL(artifactGav));
            return this;
        }

        @Override
        public LegacyKernelServiceInitializerImpl addParentFirstClassPattern(String pattern) {
            parentFirst.add(compilePattern(pattern));
            return this;
        }

        @Override
        public LegacyKernelServiceInitializerImpl addChildFirstClassPattern(String pattern) {
            childFirst.add(compilePattern(pattern));
            return this;
        }

        private Pattern compilePattern(String pattern) {
            return Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*"));
        }

        private KernelServices install(List<ModelNode> bootOperations) throws Exception {
            ClassLoader parent = this.getClass().getClassLoader() != null ? this.getClass().getClassLoader() : null;
            ClassLoader legacyCl = new ChildFirstClassLoader(parent, parentFirst, childFirst, classloaderURLs.toArray(new URL[classloaderURLs.size()]));

            Class<?> clazz = legacyCl.loadClass(extensionClassName != null ? extensionClassName : mainExtension.getClass().getName());
            Assert.assertEquals(legacyCl, clazz.getClassLoader());
            Assert.assertTrue(Extension.class.isAssignableFrom(clazz));
            Extension extension = (Extension)clazz.newInstance();

            //Initialize the parsers for the legacy subsystem (copied from the @Before method)
            XMLMapper xmlMapper = XMLMapper.Factory.create();
            TestParser testParser = new TestParser(mainSubsystemName);
            ExtensionRegistry extensionParsingRegistry = new ExtensionRegistry(getProcessType(), new RunningModeControl(RunningMode.NORMAL));
            xmlMapper.registerRootElement(new QName(TEST_NAMESPACE, "test"), testParser);
            extension.initializeParsers(extensionParsingRegistry.getExtensionParsingContext("Test", xmlMapper));

            //TODO extra parsers from additionalInit
            return KernelServices.create(mainSubsystemName, additionalInit, cloneExtensionRegistry(), bootOperations, testParser, extension, modelVersion);
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
                DescriptionProvider descriptionProvider, boolean inherited, EnumSet<Flag> flags) {
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

    static class RootResourceGrabber implements OperationStepHandler, DescriptionProvider {
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

package org.jboss.as.subsystem.test;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import junit.framework.AssertionFailedError;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestKernelServices;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;

/**
 * The base class for parsing tests which does the work of setting up the environment for parsing
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSubsystemTest {

    /**
     * Use {@link AbstractSubsystemTest#getMainSubsystemName()}
     */
    @Deprecated
    protected final String mainSubsystemName;
    private final SubsystemTestDelegate delegate;

    protected AbstractSubsystemTest(final String mainSubsystemName, final Extension mainExtension) {
        this(mainSubsystemName, mainExtension, null);
    }

    protected AbstractSubsystemTest(final String mainSubsystemName, final Extension mainExtension, final Comparator<PathAddress> removeOrderComparator) {
        this.mainSubsystemName = mainSubsystemName;
        this.delegate = new SubsystemTestDelegate(this.getClass(), mainSubsystemName, mainExtension, removeOrderComparator);
    }

    public String getMainSubsystemName() {
        return delegate.getMainSubsystemName();
    }

    @Before
    public void initializeParser() throws Exception {
        delegate.initializeParser();
    }

    @After
    public void cleanup() throws Exception {
        delegate.cleanup();
    }

    protected Extension getMainExtension() {
        return delegate.getMainExtension();
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
        return ModelTestUtils.readResource(getClass(), name);
    }


    /**
     * Parse the subsystem xml and create the operations that will be passed into the controller
     *
     * @param subsystemXml the subsystem xml to be parsed
     * @return the created operations
     * @throws XMLStreamException if there is a parsing problem
     */
    protected List<ModelNode> parse(String subsystemXml) throws XMLStreamException {
        return delegate.parse(subsystemXml);
    }

    /**
     * Parse the subsystem xml and create the operations that will be passed into the controller
     *
     * @param additionalParsers additional initialization that should be done to the parsers before initializing our extension. These parsers
     *                          will only be initialized the first time this method is called from within a test
     * @param subsystemXml      the subsystem xml to be parsed
     * @return the created operations
     * @throws XMLStreamException if there is a parsing problem
     */
    protected List<ModelNode> parse(AdditionalParsers additionalParsers, String subsystemXml) throws XMLStreamException {
        return delegate.parse(additionalParsers, subsystemXml);
    }

    /**
     * Output the model to xml
     *
     * @param model the model to marshall
     * @return the xml
     */
    protected String outputModel(ModelNode model) throws Exception {
        return delegate.outputModel(model);

    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml.
     *
     * @param subsystemXml the subsystem xml to be parsed
     * @return the kernel services allowing access to the controller and service container
     * @deprecated Use {@link #createKernelServicesBuilder(AdditionalInitialization)} instead
     */
    @Deprecated
    protected KernelServices installInController(String subsystemXml) throws Exception {
        return delegate.installInController(subsystemXml);
    }

    /**
     * Initializes the controller and populates the subsystem model from the passed in xml.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param subsystemXml   the subsystem xml to be parsed
     * @deprecated Use {@link #createKernelServicesBuilder(AdditionalInitialization)} instead
     */
    @Deprecated
    protected KernelServices installInController(AdditionalInitialization additionalInit, String subsystemXml) throws Exception {
        return delegate.installInController(additionalInit, subsystemXml);
    }

    /**
     * Create a new controller with the passed in operations.
     *
     * @param bootOperations the operations
     * @deprecated Use {@link #createKernelServicesBuilder(AdditionalInitialization)} instead
     */
    @Deprecated
    protected KernelServices installInController(List<ModelNode> bootOperations) throws Exception {
        return delegate.installInController(bootOperations);
    }

    /**
     * Create a new controller with the passed in operations.
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     * @param bootOperations the operations
     * @deprecated Use {@link #createKernelServicesBuilder(AdditionalInitialization)} instead
     */
    @Deprecated
    protected KernelServices installInController(AdditionalInitialization additionalInit, List<ModelNode> bootOperations) throws Exception {
        return delegate.installInController(additionalInit, bootOperations);
    }

    /**
     * Creates a new kernel services builder used to create a new controller containing the subsystem being tested
     *
     * @param additionalInit Additional initialization that should be done to the parsers, controller and service container before initializing our extension
     */
    protected KernelServicesBuilder createKernelServicesBuilder(AdditionalInitialization additionalInit) {
        return delegate.createKernelServicesBuilder(additionalInit);
    }

    /**
     * Gets the ProcessType to use when initializing the parsers. Defaults to {@link ProcessType#EMBEDDED_SERVER}
     * To tweak the process type when installing a controller, override {@link AdditionalInitialization} and pass in to
     * {@link #createKernelServicesBuilder(AdditionalInitialization)} instead.
     *
     * @return the process type
     */
    protected final ProcessType getProcessType() {
        return ProcessType.EMBEDDED_SERVER;
    }

    /**
     * Checks that the result was successful and gets the real result contents
     *
     * @param result the result to check
     * @return the result contents
     */
    protected static ModelNode checkResultAndGetContents(ModelNode result) {
        return ModelTestUtils.checkResultAndGetContents(result);
    }

    /**
     * Checks that the result was successful
     *
     * @param result the result to check
     * @return the result contents
     */
    protected static ModelNode checkOutcome(ModelNode result) {
        return ModelTestUtils.checkOutcome(result);
    }

    /**
     * Checks that the subystem resources can be removed, i.e. that people have registered
     * working 'remove' operations for every 'add' level. This cannot be called after the
     * kernelServices have been shut down.
     *
     *
     * @param kernelServices the kernel services used to access the controller
     */
    protected void assertRemoveSubsystemResources(KernelServices kernelServices) {
        delegate.assertRemoveSubsystemResources(kernelServices);
    }

    /**
     * Checks that the subystem resources can be removed, i.e. that people have registered
     * working 'remove' operations for every 'add' level.
     *
     * @param kernelServices        the kernel services used to access the controller
     * @param ignoredChildAddresses child addresses that should not be removed, they are managed by one of the parent resources.
     *                              This set cannot contain the subsystem resource itself
     */
    protected void assertRemoveSubsystemResources(KernelServices kernelServices, Set<PathAddress> ignoredChildAddresses) {
        delegate.assertRemoveSubsystemResources(kernelServices, ignoredChildAddresses);
    }

    /**
     * Grabs the current root resource
     *
     * @param kernelServices the kernel services used to access the controller
     */
    protected Resource grabRootResource(ModelTestKernelServices<?> kernelServices) {
        return ModelTestModelControllerService.grabRootResource(kernelServices);
    }

    /**
     * Dumps the target subsystem resource description to DMR format, needed by TransformerRegistry for non-standard subsystems
     *
     * @param kernelServices the kernel services for the started controller
     * @param modelVersion   the target subsystem model version
     * @deprecated this might no longer be needed following refactoring of TransformerRegistry
     */
    @Deprecated
    protected void generateLegacySubsystemResourceRegistrationDmr(KernelServices kernelServices, ModelVersion modelVersion) throws IOException {
        delegate.generateLegacySubsystemResourceRegistrationDmr(kernelServices, modelVersion);

    }

    /**
     * Checks that the transformed model is the same as the model built up in the legacy subsystem controller via the transformed operations,
     * and that the transformed model is valid according to the resource definition in the legacy subsystem controller.
     *
     * @param kernelServices the main kernel services
     * @param modelVersion   the model version of the targetted legacy subsystem
     * @return the whole model of the legacy controller
     */
    protected ModelNode checkSubsystemModelTransformation(KernelServices kernelServices, ModelVersion modelVersion) throws IOException {
        return checkSubsystemModelTransformation(kernelServices, modelVersion, null);
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
    protected ModelNode checkSubsystemModelTransformation(KernelServices kernelServices, ModelVersion modelVersion, ModelFixer legacyModelFixer) throws IOException {
        return delegate.checkSubsystemModelTransformation(kernelServices, modelVersion, legacyModelFixer);
    }


    /**
     * Compares two models to make sure that they are the same
     *
     * @param node1 the first model
     * @param node2 the second model
     * @throws AssertionFailedError if the models were not the same
     */
    protected void compare(ModelNode node1, ModelNode node2) {
        ModelTestUtils.compare(node1, node2);
    }

    /**
     * Compares two models to make sure that they are the same
     *
     * @param node1           the first model
     * @param node2           the second model
     * @param ignoreUndefined {@code true} if keys containing undefined nodes should be ignored
     * @throws AssertionFailedError if the models were not the same
     */
    protected void compare(ModelNode node1, ModelNode node2, boolean ignoreUndefined) {
        ModelTestUtils.compare(node1, node2, ignoreUndefined);
    }


    /**
     * Normalize and pretty-print XML so that it can be compared using string
     * compare. The following code does the following: - Removes comments -
     * Makes sure attributes are ordered consistently - Trims every element -
     * Pretty print the document
     *
     * @param xml The XML to be normalized
     * @return The equivalent XML, but now normalized
     */

    protected String normalizeXML(String xml) throws Exception {
        return ModelTestUtils.normalizeXML(xml);
    }

    public static void validateModelDescriptions(PathAddress address, ManagementResourceRegistration reg) {
        ModelTestUtils.validateModelDescriptions(address, reg);
    }

    /**
     * Validate the marshalled xml without adjusting the namespaces for the original and marshalled xml.
     *
     * @param configId   the id of the xml configuration
     * @param original   the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     * @throws Exception
     */
    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {
        ModelTestUtils.compareXml(original, marshalled);
    }

    /**
     * Validate the marshalled xml without adjusting the namespaces for the original and marshalled xml.
     *
     * @param configId        TODO
     * @param original        the original subsystem xml
     * @param marshalled      the marshalled subsystem xml
     * @param ignoreNamespace if {@code true} the subsystem's namespace is ignored, otherwise it is taken into account when comparing the normalized xml.
     * @throws Exception
     */
    protected void compareXml(String configId, final String original, final String marshalled, final boolean ignoreNamespace) throws Exception {
        ModelTestUtils.compareXml(original, marshalled, ignoreNamespace);
    }
}

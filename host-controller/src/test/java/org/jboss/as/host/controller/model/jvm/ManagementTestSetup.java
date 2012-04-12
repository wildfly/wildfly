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
package org.jboss.as.host.controller.model.jvm;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
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
 * TODO this class can perhaps be generalized into a lightweight version of the subsystem test framework
 * for the non-subsystem parts of the model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagementTestSetup {

    private ServiceContainer container;
    private ModelController controller;
    private DelegatingConfigurationPersister persister;

    public ManagementTestSetup() {
    }

    @Before
    public void setupController() throws InterruptedException {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        persister = new DelegatingConfigurationPersister();
        ModelControllerService svc = new ModelControllerService(container, processState, ProcessType.HOST_CONTROLLER, persister);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await();
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
    }

    static void createModel(final OperationContext context, final ModelNode node) {
        createModel(context, PathAddress.EMPTY_ADDRESS, node);
    }

    static void createModel(final OperationContext context, final PathAddress base, final ModelNode node) {
        if(! node.isDefined()) {
            return;
        }
        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        final Set<String> children = registration.getChildNames(base);
        final ModelNode current = new ModelNode();
        final Resource resource = base.size() == 0 ? context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS) : context.createResource(base);
        if(node.getType() == ModelType.OBJECT) {
            for(final String key : node.keys()) {
                if(! children.contains(key)) {
                    current.get(key).set(node.get(key));
                }
            }
            resource.getModel().set(current);
        } else {
            resource.getModel().set(node);
            return;
        }
        if(children != null && ! children.isEmpty()) {
            for(final String childType : children) {
                if(node.hasDefined(childType)) {
                    for(final String key : node.get(childType).keys()) {
                        createModel(context, base.append(PathElement.pathElement(childType, key)), node.get(childType, key));
                    }
                }
            }
        }
    }

    protected DescriptionProvider getRootDescriptionProvider() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("The root node of the test management API");
                node.get(CHILDREN, JVM, DESCRIPTION).set("A list of paths");
                node.get(CHILDREN, JVM, MIN_OCCURS).set(0);
                node.get(CHILDREN, JVM, MODEL_DESCRIPTION);
                return node;
            }
        };
    }

    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {

        registration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(UNDEFINE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.UNDEFINE_ATTRIBUTE, CommonProviders.UNDEFINE_ATTRIBUTE_PROVIDER, true);
        registration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
    }

    protected ModelNode createOperation(String name, String...address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(name);
        if (address.length == 0) {
            op.get(OP_ADDR).setEmptyList();
        } else {
            for (int i = 0 ; i < address.length - 1 ; i = i + 2) {
                op.get(OP_ADDR).add(address[i], address[i + 1]);
            }
        }
        return op;
    }

    protected void executeExpectingSuccess(List<ModelNode> ops) throws OperationFailedException {
        for (ModelNode op : ops) {
            executeForResult(op);
        }
    }

    protected Parser createParser(XMLElementReader<List<ModelNode>> parser, Namespace namespace, String rootElementName) {
        return new Parser(parser, namespace, rootElementName);
    }

    protected ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = controller.execute(operation, null, null, null);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }

    protected void executeForFailure(ModelNode operation) throws OperationFailedException {
        try {
            executeForResult(operation);
            Assert.fail("Should have given error");
        } catch (OperationFailedException expected) {
        }
    }

    protected ModelNode readModel(boolean recursive) throws OperationFailedException {
        ModelNode op = createOperation(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);
        return executeForResult(op);
    }

    protected String readResource(String resourceName) throws IOException {
        URL configURL = getClass().getResource(resourceName);
        org.junit.Assert.assertNotNull(resourceName + " url is null", configURL);

        BufferedReader reader = new BufferedReader(new InputStreamReader(configURL.openStream()));
        StringWriter writer = new StringWriter();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write("\n");
            }
        } finally {
            reader.close();
        }
        return writer.toString();
    }

    protected void compareXml(String expected, String marshalled, boolean removeNamespace) throws Exception {
        //TODO improve this if we end up using this for other parts of the global model
        if (removeNamespace) {
            expected = removeNamespace(expected);
            marshalled = removeNamespace(marshalled);
        }
        expected = normalizeXML(expected);
        marshalled = normalizeXML(marshalled);
        Assert.assertEquals(expected, marshalled);
    }

    private String removeNamespace(String xml) {
        return xml.replaceFirst(" xmlns=\".*\"", "");
    }

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

    class ModelControllerService extends AbstractControllerService {

        private final CountDownLatch latch = new CountDownLatch(2);

        ModelControllerService(final ServiceContainer serviceContainer, final ControlledProcessState processState, final ProcessType processType, final DelegatingConfigurationPersister persister) {
            super(processType, new RunningModeControl(RunningMode.NORMAL), persister, processState, getRootDescriptionProvider(), null, ExpressionResolver.DEFAULT);
        }

        @Override
        public void start(StartContext context) throws StartException {
            try {
                super.start(context);
            } finally {
                latch.countDown();
            }
        }

        @Override
        protected boolean boot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure)
                throws ConfigurationPersistenceException {
            try {
                return super.boot(bootOperations, rollbackOnRuntimeFailure);
            } finally {
                latch.countDown();
            }
        }

        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            try {
                ManagementTestSetup.this.initModel(rootResource, rootRegistration);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    class DelegatingConfigurationPersister extends AbstractConfigurationPersister {

        AbstractConfigurationPersister delegate;
        public DelegatingConfigurationPersister() {
            super(null);
        }

        /** {@inheritDoc} */
        @Override
        public PersistenceResource store(final ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
            if (delegate == null) {
                return new NullPersistenceResource();
            } else {
                return delegate.store(model, affectedAddresses);
            }
        }

        /** {@inheritDoc} */
        @Override
        public List<ModelNode> load() {
            return new ArrayList<ModelNode>();
        }

        private class NullPersistenceResource implements ConfigurationPersister.PersistenceResource {
            @Override
            public void commit() {
            }

            @Override
            public void rollback() {
            }
        }
    }

    static class StringConfigurationPersister extends AbstractConfigurationPersister {

        volatile String marshalled;

        public StringConfigurationPersister(XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(rootDeparser);
        }

        @Override
        public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses)
                throws ConfigurationPersistenceException {
            return new StringPersistenceResource(model, this);
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            return new ArrayList<ModelNode>();
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


    protected class Parser {
        private final XMLElementReader<List<ModelNode>> parser;
        private final String rootElementName;
        private final Namespace namespace;
        private XMLMapper xmlMapper;
        private String persistedXml;

        Parser(XMLElementReader<List<ModelNode>> parser, Namespace namespace, String rootElementName){
            this.parser = parser;
            this.namespace = namespace;
            this.rootElementName = rootElementName;
            xmlMapper = XMLMapper.Factory.create();
            xmlMapper.registerRootElement(new QName(namespace.getUriString(), rootElementName), parser);
        }

        public List<ModelNode> parseOperations(String xml) throws XMLStreamException {
            if (xmlMapper == null) {
                throw new IllegalStateException("xmlMapper not initialized, use the constructor passing in xml details");
            }
            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            final List<ModelNode> operationList = new ArrayList<ModelNode>();
            xmlMapper.parseDocument(operationList, reader);
            return operationList;
        }

        public void installParsedOperations(String xml) throws XMLStreamException, OperationFailedException {
            StringConfigurationPersister stringPersister = null;
            if (parser instanceof XMLElementWriter ) {
                stringPersister = new StringConfigurationPersister((XMLElementWriter<ModelMarshallingContext>)parser);
                persister.delegate = stringPersister;
            }
            try {
                List<ModelNode> ops = parseOperations(xml);
                executeExpectingSuccess(ops);
            } finally {
                if (stringPersister != null) {
                    persistedXml = stringPersister.marshalled;
                    persister.delegate = null;
                }
            }
        }

        public String getPersistedXml() {
            return persistedXml;
        }
    }
}

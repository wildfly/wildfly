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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.model.test.ModelTestBootOperationsBuilder;
import org.jboss.as.model.test.ModelTestKernelServices;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator.ValidationFailure;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.OperationValidation;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLMapper;



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

    private void validateDescriptionProviders(TestModelType type, ModelTestKernelServices kernelServices) {
        ModelNode op = new ModelNode();
        op.get(OP).set("read-resource-description");
        op.get(OP_ADDR).setEmptyList();
        op.get("recursive").set(true);
        op.get("inherited").set(false);
        op.get("operations").set(true);
        op.get("include-aliases").set(true);
        ModelNode result = kernelServices.executeOperation(op);
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new RuntimeException(result.get(FAILURE_DESCRIPTION).asString());
        }
        ModelNode model = result.get(RESULT);

        if (type == TestModelType.HOST) {
            //TODO (1)
            //Big big hack to get around the fact that the tests install the host description twice
            //we're only interested in the host model anyway
            //See KnownIssuesValidator.createHostPlatformMBeanAddress
            model = model.require("children").require("host").require("model-description").require("master");
        }

        //System.out.println(model);

        System.out.println(model);

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
    private class KernelServicesBuilderImpl implements KernelServicesBuilder, ModelTestBootOperationsBuilder.BootOperationParser {

        private final TestModelType type;
        private final ModelTestBootOperationsBuilder bootOperationBuilder = new ModelTestBootOperationsBuilder(testClass, this);
        private final TestParser testParser;
        private ProcessType processType;
        private RunningMode runningMode;
        private ModelInitializer modelInitializer;
        //TODO set this to EXIT_ON_VALIDATION_ERROR once model is fixed
        private OperationValidation validateOperations = OperationValidation.LOG_VALIDATION_ERRORS;
        private boolean validateDescription;
        private XMLMapper xmlMapper = XMLMapper.Factory.create();


        public KernelServicesBuilderImpl(TestModelType type) {
            this.type = type;
            this.processType = type == TestModelType.HOST || type == TestModelType.DOMAIN ? ProcessType.HOST_CONTROLLER : ProcessType.STANDALONE_SERVER;
            runningMode = RunningMode.ADMIN_ONLY;
            testParser = TestParser.create(xmlMapper, type);
        }

        public KernelServicesBuilder setDontValidateOperations() {
            bootOperationBuilder.validateNotAlreadyBuilt();
            validateOperations = OperationValidation.NONE;
            return this;
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
            KernelServices kernelServices = KernelServices.create(processType, runningMode, validateOperations, bootOperations, testParser, null, type, modelInitializer);
            CoreModelTestDelegate.this.kernelServices.add(kernelServices);

            if (validateDescription) {
                validateDescriptionProviders(type, kernelServices);
            }


            ModelTestUtils.validateModelDescriptions(PathAddress.EMPTY_ADDRESS, kernelServices.getRootRegistration());

            return kernelServices;
        }
        @Override
        public List<ModelNode> parse(String xml) throws XMLStreamException {
            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            final List<ModelNode> operationList = new ArrayList<ModelNode>();
            xmlMapper.parseDocument(operationList, reader);
            return operationList;
        }
    }
}

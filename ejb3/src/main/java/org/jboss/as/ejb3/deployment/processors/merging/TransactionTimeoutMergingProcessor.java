package org.jboss.as.ejb3.deployment.processors.merging;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;
import org.jboss.modules.Module;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import org.jboss.as.ejb3.tx.TransactionTimeoutDetails;
import org.jboss.ejb3.annotation.TransactionTimeout;

public class TransactionTimeoutMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {


    public TransactionTimeoutMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        processTransactionTimeoutAnnotation(applicationClasses, deploymentReflectionIndex, componentClass, null, componentConfiguration);
        for (ViewDescription view : componentConfiguration.getViews()) {

            try {
                final Class<?> viewClass = module.getClassLoader().loadClass(view.getViewClassName());
                EJBViewDescription ejbView = (EJBViewDescription) view;
                processTransactionTimeoutAnnotation(applicationClasses, deploymentReflectionIndex, viewClass, ejbView.getMethodIntf(), componentConfiguration);
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failToLoadEjbViewClass(e);
            }

        }
    }

    private void processTransactionTimeoutAnnotation(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, MethodIntf methodIntf, final EJBComponentDescription componentConfiguration) {
        final RuntimeAnnotationInformation<TransactionTimeoutDetails> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, TransactionTimeout.class);
        for (Map.Entry<String, List<TransactionTimeoutDetails>> entry : data.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                //we can't specify both methodIntf and class name
                final String className = methodIntf == null ? entry.getKey() : null;
                componentConfiguration.getTransactionTimeouts().setAttribute(methodIntf, className, entry.getValue().get(0));
            }
        }

        for (Map.Entry<Method, List<TransactionTimeoutDetails>> entry : data.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final MethodIdentifier method = MethodIdentifier.getIdentifierForMethod(entry.getKey());
                componentConfiguration.getTransactionTimeouts().setAttribute(methodIntf, entry.getValue().get(0), entry.getKey().getDeclaringClass().getName(), method.getName(), method.getParameterTypes());
            }
        }

    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
    }


}
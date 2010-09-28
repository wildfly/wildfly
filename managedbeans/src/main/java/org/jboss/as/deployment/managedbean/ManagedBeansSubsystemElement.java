package org.jboss.as.deployment.managedbean;

import javax.xml.stream.XMLStreamException;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.ParseUtils;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


final class ManagedBeansSubsystemElement extends AbstractSubsystemElement<ManagedBeansSubsystemElement> {
    private final Logger log = Logger.getLogger("org.jboss.as.deployment.managedbean");

    ManagedBeansSubsystemElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        ParseUtils.requireNoContent(reader);
    }

    private <T extends DeploymentUnitProcessor> void addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        batchBuilder.addService(JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }

    /** @inheritDoc} */
    private long elementHash() {
        return 42;
    }

    /** @inheritDoc} */
    protected Class<ManagedBeansSubsystemElement> getElementClass() {
        return ManagedBeansSubsystemElement.class;
    }

    /** @inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }
}

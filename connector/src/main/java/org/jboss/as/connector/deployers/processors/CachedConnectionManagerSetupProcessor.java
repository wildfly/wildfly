package org.jboss.as.connector.deployers.processors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.resource.ResourceException;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * Setup processor that adds sets operations for the cached connection manager. These operations are run around
 * incoming requests.
 *
 * @author Stuart Douglas
 */
public class CachedConnectionManagerSetupProcessor implements DeploymentUnitProcessor {

    private static final ServiceName SERVICE_NAME = ServiceName.of("jca", "cachedConnectionManagerSetupProcessor");


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceName serviceName = deploymentUnit.getServiceName().append(SERVICE_NAME);
        final CachedConnectionManagerSetupAction action = new CachedConnectionManagerSetupAction(serviceName);

        phaseContext.getServiceTarget().addService(serviceName, action)
                .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, action.cachedConnectionManager)
                .install();
        deploymentUnit.addToAttachmentList(Attachments.WEB_SETUP_ACTIONS, action);
        deploymentUnit.addToAttachmentList(Attachments.OTHER_EE_SETUP_ACTIONS, action);

    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }


    private static class CachedConnectionManagerSetupAction  implements SetupAction, Service<Void> {

        private final InjectedValue<CachedConnectionManager> cachedConnectionManager = new InjectedValue<CachedConnectionManager>();


        private final ServiceName serviceName;

        private static final Set<?> unsharable = new HashSet<Object>();

        private CachedConnectionManagerSetupAction(final ServiceName serviceName) {
            this.serviceName = serviceName;
        }


        @Override
        public void setup(final Map<String, Object> properties) {
            try {
                cachedConnectionManager.getValue().pushMetaAwareObject(this, unsharable);
            } catch (ResourceException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void teardown(final Map<String, Object> properties) {
            try {
                cachedConnectionManager.getValue().popMetaAwareObject(unsharable);
            } catch (ResourceException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Set<ServiceName> dependencies() {
            return Collections.singleton(serviceName);
        }

        @Override
        public void start(final StartContext context) throws StartException {

        }

        @Override
        public void stop(final StopContext context) {

        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }
}

package org.jboss.as.ejb3.remote;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.jboss.ejb.server.Association;
import org.jboss.msc.Service;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * This service provides an instance of Association which can be swapped between one of two
 * Association instances:
 * - a NoDeploymentsAssociation instance which is to be used when there are no @Remote beans deployed
 * - a DeploymentsAssociation instance which is to be used when @Remote beans are deployed
 *
 * The need for such a service which can "swap out" different instances of Association is to
 * prevent cache-based service from being started `at boot time, when no deployments are available.
 */
public final class AssociationService implements Service {

    public static Logger logger = Logger.getLogger("org.jboss.as.ejb3.remote.AssociationService");

    // this should be "jboss.ejb.association"
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "association");

    private final Consumer<AssociationService> associationServiceConsumer;
    private volatile DelegatingAssociationImpl value;
    private volatile Executor executor;

    public AssociationService(final Consumer<AssociationService> associationServiceConsumer) {
        this.associationServiceConsumer = associationServiceConsumer;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        logger.trace("Starting service");
        value = new DelegatingAssociationImpl();
        // set the delegate to the "no deployments" instance of Association
        value.accept(NoDeploymentsAssociationImpl.INSTANCE);
        this.associationServiceConsumer.accept(this);
        logger.trace("Started service");
    }

    @Override
    public void stop(final StopContext context) {
        logger.trace("Stopping service");
        this.associationServiceConsumer.accept(null);
        // set the delegate to null
        value.accept(null);
        logger.trace("Stopping service");
    }

    public Executor getExecutor() {
        return executor;
    }

    // invoked by EJBRemoteConnectorService on stop
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    // invoked by EJBRemoteConnectorService on stop
    public void sendTopologyUpdateIfLastNodeToLeave() {
        this.value.sendTopologyUpdateIfLastNodeToLeave();
    }

    public Association getAssociation() {
        return value.getDelegate();
    }

    public DelegatingAssociationImpl getDelegator() {
        return value;
    }
}

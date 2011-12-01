package org.jboss.as.ejb3.iiop;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.com.sun.corba.se.impl.javax.rmi.RemoteObjectSubstitution;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHandle;
import org.jboss.ejb.client.EJBHomeHandle;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.EJBMetaDataImpl;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class RemoteObjectSubstitutionService implements RemoteObjectSubstitution, Service<RemoteObjectSubstitution> {

    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<DeploymentRepository>();

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "iiop", "remoteObjectSubstitution");

    @Override
    public Object writeReplaceRemote(final Object object) {
        final DeploymentRepository deploymentRepository = deploymentRepositoryInjectedValue.getOptionalValue();
        //if we are not started yet just return
        if (deploymentRepository == null) {
            return object;
        }

        if (EJBClient.isEJBProxy(object)) {
            EJBLocator<? extends Object> locator;
            try {
                locator = EJBClient.getLocatorFor(object);
            } catch (Exception e) {
                //not a EJB proxy
                locator = null;
            }
            if (locator != null) {
                final ModuleDeployment module = deploymentRepository.getModules().get(new DeploymentModuleIdentifier(locator.getAppName(), locator.getModuleName(), locator.getDistinctName()));
                if (module == null) {
                    EjbLogger.ROOT_LOGGER.couldNotFindEjbForLocatorIIOP(locator);
                    return object;
                }
                final EjbDeploymentInformation ejb = module.getEjbs().get(locator.getBeanName());
                if (ejb == null) {
                    EjbLogger.ROOT_LOGGER.couldNotFindEjbForLocatorIIOP(locator);
                    return object;
                }
                final EjbIIOPService factory = ejb.getIorFactory();
                if (factory == null) {
                    EjbLogger.ROOT_LOGGER.ejbNotExposedOverIIOP(locator);
                    return object;
                }
                return factory.referenceForLocator(locator);
            }
        } else if(object instanceof EJBHandle) {
            //TODO: this also needs to swap out the handle implementations
            EJBHandle handle = (EJBHandle) object;
        } else if(object instanceof EJBHomeHandle) {

        } else if(object instanceof EJBMetaDataImpl) {

        }
        return object;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public RemoteObjectSubstitution getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjectedValue() {
        return deploymentRepositoryInjectedValue;
    }
}

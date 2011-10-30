package org.jboss.as.ejb3.iiop;

import java.lang.reflect.Proxy;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBLocator;

/**
 *
 * Provides static convenence methods for translating local EJB proxies to CORBA objects an vice versa
 *
 * @author Stuart Douglas
 */
public class ProxyTranslater {

    /**
     *
     * @param deploymentRepository The deployement repository for this server
     * @param retVal The potential EJB proxy to wrap
     * @return <code>retVal</code> if not a proxy, or a corba representaion of the proxy if it is
     */
    public static Object wrapPotentialProxy(final DeploymentRepository deploymentRepository, Object retVal) {
        if (retVal instanceof Proxy) {
            EJBLocator<? extends Object> locator;
            try {
                locator = (EJBLocator<? extends Object>) EJBClient.getLocatorFor(retVal);
            } catch (Exception e) {
                //not a EJB proxy
                locator = null;
            }
            if (locator != null) {
                final ModuleDeployment module = deploymentRepository.getModules().get(new DeploymentModuleIdentifier(locator.getAppName(), locator.getModuleName(), locator.getDistinctName()));
                if (module == null) {
                    throw new RuntimeException("Could not locate EJB for " + locator);
                }
                final EjbDeploymentInformation ejb = module.getEjbs().get(locator.getBeanName());
                if(ejb == null) {
                    throw new RuntimeException("Could not locate EJB for " + locator);
                }
                final EjbIIOPService factory = ejb.getIorFactory();
                if(factory == null) {
                    throw new RuntimeException("EJB " + locator + " is not exposed via IIOP");
                }
                retVal = factory.referenceForLocator(locator);
            }
        }
        return retVal;
    }

}

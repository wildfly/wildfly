package org.jboss.as.arquillian.container;

import java.lang.reflect.InvocationTargetException;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.Extension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Stuart Douglas
 */
public class InContainerExtensionSetup {

    public synchronized void handleBeforeDeployment(@Observes BeforeDeploy event, Container container) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        final Archive<?> archive = event.getDeployment().getArchive();
        //add the required classes
        if(archive instanceof LibraryContainer) {
            JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbossInContainerSetupObserverExtensionSupportClasses.jar");
            jar.addClasses(InContainerExtension.class, InContainerManagementClientProvider.class, ManagementClient.class);
            jar.addAsServiceProvider(Extension.class, InContainerExtension.class);
            ((LibraryContainer) archive).addAsLibrary(jar);
        } else if(archive instanceof ClassContainer) {
            ((ClassContainer) archive).addClasses(InContainerExtension.class, InContainerManagementClientProvider.class, ManagementClient.class);
            ((ManifestContainer) archive).addAsServiceProvider(Extension.class, InContainerExtension.class);
        }
    }
}

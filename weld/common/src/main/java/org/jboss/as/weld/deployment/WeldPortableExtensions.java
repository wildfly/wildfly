package org.jboss.as.weld.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.MetadataImpl;

/**
 * Container class that is attached to the top level deployment that holds all portable extension metadata.
 * <p/>
 * A portable extension may be available to multiple deployment class loaders, however for each PE we
 * only want to register a single instance.
 * <p/>
 * This container provides a mechanism for making sure that only a single PE of a given type is registered.
 *
 * @author Stuart Douglas
 */
public class WeldPortableExtensions {

    public static final AttachmentKey<WeldPortableExtensions> ATTACHMENT_KEY = AttachmentKey.create(WeldPortableExtensions.class);

    public static WeldPortableExtensions getPortableExtensions(final DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            WeldPortableExtensions pes = deploymentUnit.getAttachment(WeldPortableExtensions.ATTACHMENT_KEY);
            if (pes == null) {
                deploymentUnit.putAttachment(ATTACHMENT_KEY, pes = new WeldPortableExtensions());
            }
            return pes;
        } else {
            return getPortableExtensions(deploymentUnit.getParent());
        }
    }

    private final Map<Class<?>, Metadata<Extension>> extensions = new HashMap<>();

    public synchronized void tryRegisterExtension(final Class<?> extensionClass, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        if (!Extension.class.isAssignableFrom(extensionClass)) {
            throw WeldLogger.ROOT_LOGGER.extensionDoesNotImplementExtension(extensionClass);
        }
        if (extensions.containsKey(extensionClass)) {
            return;
        }
        try {
            extensions.put(extensionClass, new MetadataImpl<>((Extension) extensionClass.newInstance(), deploymentUnit.getName()));
        } catch (Exception e) {
            WeldLogger.DEPLOYMENT_LOGGER.couldNotLoadPortableExceptionClass(extensionClass.getName(), e);
        }

    }

    public synchronized void registerExtensionInstance(final Extension extension, final DeploymentUnit deploymentUnit) {
        extensions.put(extension.getClass(), new MetadataImpl<>(extension, deploymentUnit.getName()));
    }

    public Collection<Metadata<Extension>> getExtensions() {
        return new HashSet<>(extensions.values());
    }

}

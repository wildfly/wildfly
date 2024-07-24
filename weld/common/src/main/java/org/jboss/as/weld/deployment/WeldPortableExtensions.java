/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.MetadataImpl;
import org.jboss.weld.exceptions.IllegalArgumentException;

/**
 * Container class that is attached to the top level deployment that holds all portable and build compatible extensions
 * metadata.
 * <p/>
 * A CDI extension may be available to multiple deployment class loaders, however for each PE we
 * only want to register a single instance.
 * <p/>
 * This container provides a mechanism for making sure that only a single PE/BCE of a given type is registered.
 *
 * @author Stuart Douglas
 *
 * @deprecated Use WeldCapability to get access to the functionality of this class.
 */
@Deprecated
public class WeldPortableExtensions {
    // Once we can remove this class from this package, we should move it under org.jboss.as.weld._private.
    // It will protect their uses outside of weld subsystem and will force external callers to use WeldCapability
    // instead to utilize this class.

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
    private final Collection<Class<? extends BuildCompatibleExtension>> buildCompatibleExtensions = new ArrayList<>();

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

    public synchronized void registerBuildCompatibleExtension(final Class<? extends BuildCompatibleExtension> extension) {
        buildCompatibleExtensions.add(extension);
    }

    /**
     * If there was at least one build compatible extension discovered or registered, this method will add a portable
     * extension allowing their execution. It is a no-op if there are no build compatible extensions.
     *
     * @param extensionCreator a function that can create an instance of the {{@code LiteExtensionTranslator}} from given collection of found BCEs
     * @throws IllegalArgumentException if the deployment unit parameter equals null
     */
    public synchronized void registerLiteExtensionTranslatorIfNeeded(Function<List<Class<? extends BuildCompatibleExtension>>,
                Extension> extensionCreator, DeploymentUnit deploymentUnit) throws IllegalArgumentException {
        if (deploymentUnit == null) {
            throw WeldLogger.ROOT_LOGGER.incorrectBceTranslatorSetup();
        }

        // only register if we found any BCE be it via discovery or manual registration
        if (!buildCompatibleExtensions.isEmpty()) {
            registerExtensionInstance(extensionCreator.apply(getBuildCompatibleExtensions()), deploymentUnit);
        }
    }

    public Collection<Metadata<Extension>> getExtensions() {
        return new HashSet<>(extensions.values());
    }

    public List<Class<? extends BuildCompatibleExtension>> getBuildCompatibleExtensions() {
        return new ArrayList<>(buildCompatibleExtensions);
    }


}

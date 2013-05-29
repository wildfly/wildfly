/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.management;

import static org.jboss.as.patching.Constants.JBOSS_PATCHING;
import static org.jboss.as.patching.Constants.JBOSS_PRODUCT_CONFIG_SERVICE;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.version.ProductConfig;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchInfoService implements Service<PatchInfo> {

    public static ServiceName NAME = JBOSS_PATCHING.append("info");

    private final InjectedValue<ProductConfig> productConfig = new InjectedValue<ProductConfig>();

    private volatile DirectoryStructure directoryStructure;
    private volatile PatchInfo patchInfo;

    /**
     * This field is set to true when a patch is applied/rolled back at runtime.
     * It prevents another patch to be applied and overrides the modifications brought by the previous one
     * unless the process is restarted first
     *
     * This field has to be {@code static} in order to survive server reloads.
     */
    private static final AtomicBoolean restartRequired = new AtomicBoolean(false);

    /**
     * Install the patch info service
     *
     * @param serviceTarget
     *
     * @return the service controller for the installed patch info service
     */
    public static ServiceController<PatchInfo> installService(final ServiceTarget serviceTarget) {
        final PatchInfoService service = new PatchInfoService();
        return serviceTarget.addService(PatchInfoService.NAME, service)
                .addDependency(JBOSS_PRODUCT_CONFIG_SERVICE, ProductConfig.class, service.productConfig)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private PatchInfoService() {
        //
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        final ProductConfig config = productConfig.getValue();
        final File file = new File(System.getProperty("jboss.home.dir"));
        final DirectoryStructure structure = DirectoryStructure.createLegacy(file);
        try {
            this.patchInfo = LocalPatchInfo.load(config, structure);
            // ROOT_LOGGER.usingModulePath(asList(patchInfo.getModulePath()));
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        this.patchInfo = null;
    }

    @Override
    public synchronized PatchInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return patchInfo;
    }

    public DirectoryStructure getStructure() {
        return directoryStructure;
    }

    public boolean requiresRestart() {
        return restartRequired.get();
    }

    /**
     * Require a restart. This will set the patching service to read-only
     * and the server has to be restarted in order to execute the next
     * patch operation.
     *
     * In case the patch operation does not succeed it needs to clear the
     * reload required state using {@link #clearRestartRequired()}.
     *
     * @return this will return {@code true}
     */
    protected boolean restartRequired() {
        return restartRequired.compareAndSet(false, true);
    }

    protected void clearRestartRequired() {
        restartRequired.set(false);
    }

}

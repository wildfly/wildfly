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

package org.jboss.as.patching;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.version.ProductConfig;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author Emanuel Muckenhuber
 */
public final class PatchInfoService implements Service<PatchInfoService> {

    public static ServiceName NAME = ServiceName.JBOSS.append("patch").append("info");

    private final ProductConfig config;
    private final DirectoryStructure structure;
    private volatile PatchInfo patchInfo;

    private static final AtomicReferenceFieldUpdater<PatchInfoService, PatchInfo> updater = AtomicReferenceFieldUpdater.newUpdater(PatchInfoService.class, PatchInfo.class, "patchInfo");

    public PatchInfoService(final ProductConfig config, final File jbossHome) {
        this.config = config;
        this.structure = DirectoryStructure.createDefault(jbossHome);
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            this.patchInfo = LocalPatchInfo.load(config, structure);
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        this.patchInfo = null;
    }

    @Override
    public synchronized PatchInfoService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public DirectoryStructure getStructure() {
        return structure;
    }

    public PatchInfo getPatchInfo() {
        return patchInfo;
    }

    boolean setPatchInfo(final PatchInfo oldInfo, final PatchInfo newInfo) {
        return updater.compareAndSet(this, oldInfo, newInfo);
    }

}
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.common;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

/**
 * @author Remy Maucherat
 */
public class WarMetaData {

    public static final AttachmentKey<WarMetaData> ATTACHMENT_KEY = AttachmentKey.create(WarMetaData.class);

    /**
     * jboss-web.xml metadata.
     */
    private volatile JBossWebMetaData jbossWebMetaData;

    /**
     * Main web.xml metadata.
     */
    private volatile WebMetaData webMetaData;

    /**
     * Web fragments metadata.
     */
    private volatile Map<String, WebFragmentMetaData> webFragmentsMetaData;

    /**
     * Annotations metadata.
     */
    private volatile Map<String, WebMetaData> annotationsMetaData;

    /**
     * additional module annotations metadata.
     */
    private volatile List<WebMetaData> additionalModuleAnnotationsMetadata;
    /**
     * Order.
     */
    private volatile List<String> order;

    /**
     * No order flag.
     */
    private volatile boolean noOrder = false;

    /**
     * Overlays.
     */
    private volatile Set<VirtualFile> overlays;

    /**
     * SCIs.
     */
    private volatile Map<String, VirtualFile> scis;

    /**
     * Final merged metadata.
     */
    private volatile JBossWebMetaData mergedJBossWebMetaData;

    private File tempDir;


    private final Set<ServiceName> additionalDependencies = new HashSet<ServiceName>();

    public JBossWebMetaData getJBossWebMetaData() {
        return jbossWebMetaData;
    }

    public void setJBossWebMetaData(JBossWebMetaData jbossWebMetaData) {
        this.jbossWebMetaData = jbossWebMetaData;
    }

    public WebMetaData getWebMetaData() {
        return webMetaData;
    }

    public void setWebMetaData(WebMetaData webMetaData) {
        this.webMetaData = webMetaData;
    }

    public Map<String, WebFragmentMetaData> getWebFragmentsMetaData() {
        return webFragmentsMetaData;
    }

    public void setWebFragmentsMetaData(Map<String, WebFragmentMetaData> webFragmentsMetaData) {
        this.webFragmentsMetaData = webFragmentsMetaData;
    }

    public Map<String, WebMetaData> getAnnotationsMetaData() {
        return annotationsMetaData;
    }

    public void setAnnotationsMetaData(Map<String, WebMetaData> annotationsMetaData) {
        this.annotationsMetaData = annotationsMetaData;
    }

    public List<String> getOrder() {
        return order;
    }

    public void setOrder(List<String> order) {
        this.order = order;
    }

    public boolean isNoOrder() {
        return noOrder;
    }

    public void setNoOrder(boolean noOrder) {
        this.noOrder = noOrder;
    }

    public Set<VirtualFile> getOverlays() {
        return overlays;
    }

    public void setOverlays(Set<VirtualFile> overlays) {
        this.overlays = overlays;
    }

    public Map<String, VirtualFile> getScis() {
        return scis;
    }

    public void setScis(Map<String, VirtualFile> scis) {
        this.scis = scis;
    }

    public JBossWebMetaData getMergedJBossWebMetaData() {
        return mergedJBossWebMetaData;
    }

    public void setMergedJBossWebMetaData(JBossWebMetaData mergedJBossWebMetaData) {
        this.mergedJBossWebMetaData = mergedJBossWebMetaData;
    }

    public List<WebMetaData> getAdditionalModuleAnnotationsMetadata() {
        return additionalModuleAnnotationsMetadata;
    }

    public void setAdditionalModuleAnnotationsMetadata(List<WebMetaData> additionalModuleAnnotationsMetadata) {
        this.additionalModuleAnnotationsMetadata = additionalModuleAnnotationsMetadata;
    }

    public void addAdditionalDependency(final ServiceName serviceName) {
        this.additionalDependencies.add(serviceName);
    }

    public Set<ServiceName> getAdditionalDependencies() {
        return Collections.unmodifiableSet(additionalDependencies);
    }

    public File getTempDir() {
        return tempDir;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }
}

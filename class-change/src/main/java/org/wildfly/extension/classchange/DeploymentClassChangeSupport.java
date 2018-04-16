/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.extension.classchange;

import java.nio.file.Path;
import java.util.Map;

/**
 * Support for deployment class change events. Subsystems can register listeners that will
 * be notified when a class has been modified.
 */
public interface DeploymentClassChangeSupport {

    /**
     * Adds a listener that can react to change change events
     *
     * @param listener The listener
     */
    void addListener(ClassChangeListener listener);

    /**
     * @return A map of all known class files and their last modified time
     */
    Map<String, Long> getKnownClasses();

    /**
     * @return A map of all known web resources and their last modified time
     */
    Map<String, Long> getKnownWebResources();

    /**
     * Asks the container to scan for changes to an exploded deployment
     */
    void scanForChangedClasses();

    /**
     * Notifies the subsystem that some classes have changed, generally used by the remote agent.
     * <p>
     * This may result in a redeploy, if it is not possible to apply the class changes using Fakereplace
     *
     * @param srcFiles     Change src files
     * @param classFiles   Changed class files
     * @param webResources Changed web resources
     */
    void notifyChangedClasses(Map<String, byte[]> srcFiles, Map<String, byte[]> classFiles, Map<String, byte[]> webResources);

    /**
     * @return The path for additional web resources, or null if this is not used
     */
    Path getAdditionalWebResourcesRoot();
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.launcher;

import java.util.Map;

import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * This class implements the FrameworkFactory that is defined in the OSGi launcher specification.
 * It allows other programs to launch OSGi framework in a standard way and is also required in
 * order to run the OSGi TCK with the JBoss AS.
 *
 * @author David Bosschaert
 */
public class JBossOSGiFrameworkFactory implements FrameworkFactory {
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Framework newFramework(Map configuration) {
        EmbeddedOSGiFrameworkLauncher launcher = new EmbeddedOSGiFrameworkLauncher();
        launcher.configure(configuration);
        launcher.start();

        return new FrameworkWrapper(launcher);
    }
}

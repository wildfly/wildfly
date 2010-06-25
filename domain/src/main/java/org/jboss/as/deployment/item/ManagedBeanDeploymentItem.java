/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.item;

import java.util.List;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * A deployment item which acts as some type of JavaEE managed bean.
 */
public final class ManagedBeanDeploymentItem extends DeploymentItem {
    private final String name;
    private final String className;
    private final String postConstructMethodName;
    private final String preDestroyMethodName;
    private final List<Resource> resources;

    public static final ServiceName JBOSS_MANAGEDBEAN = ServiceName.JBOSS.append("managedbean");

    private static final String BINDING = "binding";

    protected ManagedBeanDeploymentItem(final String name, final String className, final String postConstructMethodName, final String preDestroyMethodName) {
        this.name = name;
        this.className = className;
        this.postConstructMethodName = postConstructMethodName;
        this.preDestroyMethodName = preDestroyMethodName;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getPostConstructMethodName() {
        return postConstructMethodName;
    }

    public String getPreDestroyMethodName() {
        return preDestroyMethodName;
    }

    protected DeployedItem install(final BatchBuilder builder) {
//        builder.addService(JBOSS_MANAGEDBEAN.append("module-name-here").append(BINDING).append(name), new JNDIBindingService(name));
        return null;
    }

    protected static abstract class Resource {
        
    }
}

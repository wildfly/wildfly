/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * The final configuration for Weld where the global configuration defined in the model is combined in per-deployment configuration specified in
 * <code>jboss-all.xml</code>
 *
 * @author Jozef Hartinger
 *
 */
class WeldConfiguration {

    public static final AttachmentKey<WeldConfiguration> ATTACHMENT_KEY = AttachmentKey.create(WeldConfiguration.class);

    private final boolean requireBeanDescriptor;
    private final boolean nonPortableMode;
    private final boolean developmentMode;

    public WeldConfiguration(boolean requireBeanDescriptor, boolean nonPortableMode, boolean developmentMode) {
        this.requireBeanDescriptor = requireBeanDescriptor;
        this.nonPortableMode = nonPortableMode;
        this.developmentMode = developmentMode;
    }

    public boolean isNonPortableMode() {
        return nonPortableMode;
    }

    public boolean isRequireBeanDescriptor() {
        return requireBeanDescriptor;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    @Override
    public String toString() {
        return "WeldConfiguration [requireBeanDescriptor=" + requireBeanDescriptor + ", nonPortableMode=" + nonPortableMode + ", developmentMode="
                + developmentMode + "]";
    }

}

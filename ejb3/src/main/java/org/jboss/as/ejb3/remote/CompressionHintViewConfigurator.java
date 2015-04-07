/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.remote;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.ejb.client.annotation.CompressionHint;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class CompressionHintViewConfigurator implements ViewConfigurator {

    public static final CompressionHintViewConfigurator INSTANCE = new CompressionHintViewConfigurator();

    private CompressionHintViewConfigurator() {

    }

    @Override
    public void configure(DeploymentPhaseContext context, ComponentConfiguration componentConfiguration, ViewDescription description, ViewConfiguration configuration) throws DeploymentUnitProcessingException {

        CompressedMethodsInformation value = getCompressedMethodsInformation(configuration.getViewClass());
        if(value != null) {
            configuration.getPrivateData().put(CompressedMethodsInformation.class, value);
        }
    }

    public static CompressedMethodsInformation getCompressedMethodsInformation(Class<?> viewClass) {
        Method[] methods = viewClass.getMethods();
        Map<Method, CompressionHint> hints = new HashMap<>();
        boolean present = false;

        for(Method method : methods) {
            CompressionHint hint = method.getAnnotation(CompressionHint.class);
            if(hint != null) {
                hints.put(method, hint);
                present = true;
            }
        }

        CompressionHint compression = viewClass.getAnnotation(CompressionHint.class);
        if(compression != null) {
            present = true;
        }
        CompressedMethodsInformation value = null;
        if(present) {
            value = new CompressedMethodsInformation(hints, compression);
        }
        return value;
    }

}

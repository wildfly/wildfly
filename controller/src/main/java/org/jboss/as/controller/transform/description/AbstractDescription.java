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

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.TransformersSubRegistration;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractDescription implements TransformationDescription {

    private final PathTransformation pathTransformation;
    protected final PathElement pathElement;

    AbstractDescription(final PathElement pathElement, final PathTransformation transformation) {
        this.pathElement = pathElement;
        this.pathTransformation = transformation;
    }

    @Override
    public PathElement getPath() {
        return pathElement;
    }

    @Override
    public PathTransformation getPathTransformation() {
        return pathTransformation;
    }

    @Override
    public void register(final SubsystemRegistration subsytem, ModelVersion... versions) {
        TransformationDescription.Tools.register(this, subsytem, versions);
    }

    @Override
    public void register(SubsystemRegistration subsytem, ModelVersionRange range) {
        TransformationDescription.Tools.register(this, subsytem, range);
    }

    @Override
    public void register(TransformersSubRegistration parent) {
        TransformationDescription.Tools.register(this, parent);
    }

}

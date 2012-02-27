/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;

import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Corrects the {@link CommonAttributes#RELATIVE_TO relative-to} attribute.
 * <p/>
 * Checks the {@link CommonAttributes#PATH path} attribute for an absolute path. If the path is absolute, the current
 * {@link CommonAttributes#RELATIVE_TO relative-to} attribute is not copied over. If the path is not absolute the
 * current {@link CommonAttributes#RELATIVE_TO relative-to} attribute is copied over.
 * <p/>
 * Date: 29.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FileCorrector implements ParameterCorrector {

    public static final FileCorrector INSTANCE = new FileCorrector();

    @Override
    public ModelNode correct(final ModelNode newValue, final ModelNode currentValue) {
        if (newValue.getType() == ModelType.UNDEFINED) {
            return newValue;
        }
        if (newValue.getType() != ModelType.OBJECT || currentValue.getType() != ModelType.OBJECT) {
            return newValue;
        }
        final ModelNode newPath = newValue.get(PATH.getName());
        if (newPath.isDefined() && !AbstractPathService.isAbsoluteUnixOrWindowsPath(newPath.asString())) {
            if (currentValue.hasDefined(RELATIVE_TO.getName()) && !newValue.hasDefined(RELATIVE_TO.getName())) {
                newValue.get(RELATIVE_TO.getName()).set(currentValue.get(RELATIVE_TO.getName()));
            }
        }
        return newValue;
    }
}

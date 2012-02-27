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

package org.jboss.as.logging.validators;

import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates the {@link org.jboss.as.logging.CommonAttributes#FILE file} attribute.
 * <p/>
 * A valid {@link org.jboss.as.logging.CommonAttributes#FILE file} attribute must have an absolute
 * {@link org.jboss.as.logging.CommonAttributes#PATH path} attribute or a
 * {@link org.jboss.as.logging.CommonAttributes#PATH path} attribute with a valid
 * {@link org.jboss.as.logging.CommonAttributes#RELATIVE_TO relative-to} attribute.
 * <p/>
 * Date: 28.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FileValidator extends ModelTypeValidator {

    public FileValidator() {
        super(ModelType.OBJECT);
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        final ModelNode clone = value.clone();
        RELATIVE_TO.getValidator().validateParameter(parameterName, clone.get(RELATIVE_TO.getName()));
        PATH.getValidator().validateParameter(parameterName, clone.get(PATH.getName()));
        if (value.isDefined()) {
            // Could have relative-to
            if (value.hasDefined(RELATIVE_TO.getName())) {
                final String relativeTo = value.get(RELATIVE_TO.getName()).asString();
                // Can't be an absolute path
                if (AbstractPathService.isAbsoluteUnixOrWindowsPath(relativeTo)) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidRelativeTo(relativeTo)));
                }
            }
        }
    }
}

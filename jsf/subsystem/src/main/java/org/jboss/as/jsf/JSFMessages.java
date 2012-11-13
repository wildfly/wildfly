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

package org.jboss.as.jsf;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 05.11.2011
 *
 * 12650 - 12699
 *
 * @author Stuart Douglas
 * @author Stan Silvert
 */
@MessageBundle(projectCode = "JBAS")
public interface JSFMessages {

    /**
     * The messages
     */
    JSFMessages MESSAGES = Messages.getBundle(JSFMessages.class);

    @Message(id = 12650, value = "Failed to load annotated class: %s")
    String classLoadingFailed(DotName clazz);

    @Message(id = 12651, value = "Annotation %s in class %s is only allowed on classes")
    String invalidAnnotationLocation(Object annotation, AnnotationTarget classInfo);

    @Message(id = 12652, value = "Instance creation failed")
    RuntimeException instanceCreationFailed(@Cause Throwable t);

    @Message(id = 12653, value = "Instance destruction failed")
    RuntimeException instanceDestructionFailed(@Cause Throwable t);

    @Message(id = 12654, value = "Thread local injection container not set")
    IllegalStateException noThreadLocalInjectionContainer();

    @Message(id = 12655, value = "@ManagedBean is only allowed at class level %s")
    String invalidManagedBeanAnnotation(AnnotationTarget target);

    @Message(id= 12656, value = "'%s' is not a valid JSF implementation slot")
    OperationFailedException invalidJSFImplSlot(String slot);
}

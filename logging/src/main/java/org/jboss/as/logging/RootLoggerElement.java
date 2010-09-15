/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.util.Collection;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;

import javax.xml.namespace.QName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RootLoggerElement extends AbstractLoggerElement<RootLoggerElement> {

    private static final long serialVersionUID = 5284930321162426129L;

    private static final QName ELEMENT_NAME = new QName(Namespace.CURRENT.getUriString(), Element.ROOT_LOGGER.getLocalName());

    RootLoggerElement() {
    }

    BatchServiceBuilder<Logger> createService(final BatchBuilder builder) {
        return builder.addService(LoggingSubsystemElement.JBOSS_LOGGING.append("logger", "root"), new LoggerLevelService());
    }

    void configureService(final BatchServiceBuilder<Logger> serviceBuilder, final BatchBuilder builder, final AbstractLoggerService loggerService) {
        super.configureService(serviceBuilder, builder, loggerService);
    }

    QName getElementName() {
        return ELEMENT_NAME;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<RootLoggerElement>> target, final RootLoggerElement other) {
    }

    protected Class<RootLoggerElement> getElementClass() {
        return RootLoggerElement.class;
    }

    ServiceName getLoggerServiceName() {
        return null;
    }

    AbstractLoggerService getLoggerService() {
        return null;
    }
}

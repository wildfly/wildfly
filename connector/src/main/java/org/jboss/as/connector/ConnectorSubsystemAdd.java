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

package org.jboss.as.connector;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConnectorSubsystemAdd extends AbstractSubsystemAdd<ConnectorSubsystemElement> {

    private static final long serialVersionUID = -874698675049495644L;
    private boolean archiveValidation = true;
    private boolean archiveValidationFailOnError = true;
    private boolean archiveValidationFailOnWarn = false;
    private boolean beanValidation = true;

    protected ConnectorSubsystemAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();

        final ConnectorSubsystemConfiguration config = new ConnectorSubsystemConfiguration();

        config.setArchiveValidation(archiveValidation);
        config.setArchiveValidationFailOnError(archiveValidationFailOnError);
        config.setArchiveValidationFailOnWarn(archiveValidationFailOnWarn);
        config.setBeanValidation(beanValidation);

        final ConnectorConfigService connectorConfigService = new ConnectorConfigService(config);

        final BatchServiceBuilder<ConnectorSubsystemConfiguration> serviceBuilder = builder.addService(
                ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService);
        serviceBuilder.setInitialMode(Mode.ACTIVE);

    }

    protected ConnectorSubsystemElement createSubsystemElement() {
        return new ConnectorSubsystemElement();
    }

    public boolean isArchiveValidation() {
        return archiveValidation;
    }

    public void setArchiveValidation(final boolean archiveValidation) {
        this.archiveValidation = archiveValidation;
    }

    public boolean isArchiveValidationFailOnError() {
        return archiveValidationFailOnError;
    }

    public void setArchiveValidationFailOnError(final boolean archiveValidationFailOnError) {
        this.archiveValidationFailOnError = archiveValidationFailOnError;
    }

    public boolean isArchiveValidationFailOnWarn() {
        return archiveValidationFailOnWarn;
    }

    public void setArchiveValidationFailOnWarn(final boolean archiveValidationFailOnWarn) {
        this.archiveValidationFailOnWarn = archiveValidationFailOnWarn;
    }

    public boolean isBeanValidation() {
        return beanValidation;
    }

    public void setBeanValidation(final boolean beanValidation) {
        this.beanValidation = beanValidation;
    }
}

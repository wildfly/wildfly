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

package org.jboss.as.cmp.subsystem;

import java.util.Locale;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.as.cmp.keygenerator.uuid.UUIDKeyGeneratorFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class UUIDKeyGeneratorAdd extends AbstractKeyGeneratorAdd {
    static UUIDKeyGeneratorAdd INSTANCE = new UUIDKeyGeneratorAdd();

    private UUIDKeyGeneratorAdd() {
    }

    protected Service<KeyGeneratorFactory> getKeyGeneratorFactory(final ModelNode operation) {
        return new UUIDKeyGeneratorFactory();
    }

    protected ServiceName getServiceName(final String name) {
        return UUIDKeyGeneratorFactory.SERVICE_NAME.append(name);
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }

    public ModelNode getModelDescription(final Locale locale) {
        return CmpSubsystemDescriptions.getUuidAddDescription(locale);
    }
}

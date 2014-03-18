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

package org.wildfly.extension.picketlink.federation.model;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class KeyStoreProviderResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition FILE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_FILE.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_RELATIVE_TO.getName(), ModelType.STRING, true)
        .setRequires(ModelElement.COMMON_FILE.getName())
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_PASSWORD.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SIGN_KEY_ALIAS = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_SIGN_KEY_ALIAS.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SIGN_KEY_PASSWORD = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_SIGN_KEY_PASSWORD.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();

    public static final KeyStoreProviderResourceDefinition INSTANCE = new KeyStoreProviderResourceDefinition();

    private KeyStoreProviderResourceDefinition() {
        super(ModelElement.KEY_STORE, ModelElement.KEY_STORE.getName(), KeyStoreProviderAddHandler.INSTANCE, KeyStoreProviderRemoveHandler.INSTANCE, FILE, RELATIVE_TO, PASSWORD, SIGN_KEY_ALIAS, SIGN_KEY_PASSWORD);
    }
}

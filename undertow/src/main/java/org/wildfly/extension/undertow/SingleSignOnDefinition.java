/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>  2014 Red Hat Inc.
 */
class SingleSignOnDefinition extends PersistentResourceDefinition {

    static final SimpleAttributeDefinition DOMAIN = new SimpleAttributeDefinitionBuilder(Constants.DOMAIN, ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition RE_AUTHENTICATE = new SimpleAttributeDefinitionBuilder("re-authenticate", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    /*static final SimpleAttributeDefinition CACHE_CONTAINER = new SimpleAttributeDefinitionBuilder("cache-container", ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .build();
    static final SimpleAttributeDefinition CACHE_NAME = new SimpleAttributeDefinitionBuilder("cache-name", ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .build();*/

    static final List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(DOMAIN, RE_AUTHENTICATE);

    static final SingleSignOnDefinition INSTANCE = new SingleSignOnDefinition();

    private SingleSignOnDefinition() {
        super(UndertowExtension.PATH_SSO, UndertowExtension.getResolver("single-sign-on"), new SingleSignOnAdd(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return (Collection) ATTRIBUTES;
    }

}

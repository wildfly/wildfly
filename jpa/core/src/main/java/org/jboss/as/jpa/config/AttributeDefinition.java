// $Id$
/*
 * Copyright (c) 2011, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.jboss.as.jpa.config;

import org.jboss.as.server.parsing.PropertiesValueResolver;

/**
 * Defines properties of the attribute.
 *
 * @author kulikov
 */
public class AttributeDefinition {
    //true when this attribute allow expression

    private boolean allowExpression;

    /**
     * Creates new instance of this descriptor.
     *
     * @param allowExpression true if attribute's value allow expression
     */
    public AttributeDefinition(boolean allowExpression) {
        this.allowExpression = allowExpression;
    }

    /**
     * Expression support flag.
     *
     * @return true if attributes value can be an expression.
     */
    public boolean isAllowExpression() {
        return allowExpression;
    }

    /**
     * Resolves value of the attribute from text description.
     *
     * @param value textual expression
     * @return value.
     */
    public String resolve(String value) {
        return allowExpression ? PropertiesValueResolver.replaceProperties(value) : value;
    }
}

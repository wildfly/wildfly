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

/**
 * Persistence unit attributes.
 *
 *
 * @author kulikov
 */
public final class PersistenceUnit {

    public static final AttributeDefinition CLASS = new AttributeDefinition(false);
    public static final AttributeDefinition DESCRIPTION = new AttributeDefinition(false);
    public static final AttributeDefinition EXCLUDEUNLISTEDCLASSES = new AttributeDefinition(false);
    public static final AttributeDefinition JARFILE = new AttributeDefinition(true);
    public static final AttributeDefinition JTADATASOURCE = new AttributeDefinition(true);
    public static final AttributeDefinition MAPPINGFILE = new AttributeDefinition(true);
    public static final AttributeDefinition NONJTADATASOURCE = new AttributeDefinition(true);
    public static final AttributeDefinition PROVIDER = new AttributeDefinition(true);
    public static final AttributeDefinition PROPERTY = new AttributeDefinition(true);
    public static final AttributeDefinition SHAREDCACHEMODE = new AttributeDefinition(false);
    public static final AttributeDefinition VALIDATIONMODE = new AttributeDefinition(false);
}
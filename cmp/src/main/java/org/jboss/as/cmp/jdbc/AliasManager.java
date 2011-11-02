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
package org.jboss.as.cmp.jdbc;

import java.util.HashMap;
import java.util.Map;


/**
 * This class manages aliases for generated queries.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
final class AliasManager {
    private static final String RELATION_TABLE_SUFFIX = "_RELATION_TABLE";

    private final String prefix;
    private final String suffix;
    private final int maxLength;
    private final Map aliases = new HashMap();
    private final Map relationTableAliases = new HashMap();

    private int count = 0;

    public AliasManager(String prefix, String suffix, int maxLength) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.maxLength = maxLength;
    }

    public String getAlias(String path) {
        String alias = (String) aliases.get(path);
        if (alias == null) {
            alias = createAlias(path);
            aliases.put(path, alias);
        }
        return alias;
    }

    private String createAlias(String path) {
        StringBuffer alias = new StringBuffer();
        alias.append(prefix).append(count++).append(suffix);
        alias.append(path.replace('.', '_'));

        return alias.substring(0, Math.min(maxLength, alias.length()));
    }

    public void addAlias(String path, String alias) {
        aliases.put(path, alias);
    }

    public String getRelationTableAlias(String path) {
        String relationTableAlias = (String) relationTableAliases.get(path);
        if (relationTableAlias == null) {
            relationTableAlias = createRelationTableAlias(path);
            relationTableAliases.put(path, relationTableAlias);
        }
        return relationTableAlias;
    }

    private String createRelationTableAlias(String path) {
        StringBuffer relationTableAlias = new StringBuffer();

        relationTableAlias.append(prefix).append(count++).append(suffix);

        relationTableAlias.append(path.replace('.', '_'));
        relationTableAlias.append(RELATION_TABLE_SUFFIX);

        return relationTableAlias.substring(
                0, Math.min(maxLength, relationTableAlias.length()));
    }
}

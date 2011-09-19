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

package org.jboss.as.cmp.jdbc.metadata.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaDataFactory;

/**
 * @author John Bailey
 */
public class ParsedQuery {
    String methodName;
    List<String> methodParams = new ArrayList<String>();
    String query;
    Class<?> qlCompiler;
    ParsedReadAhead readAheadMetaData;
    JDBCQueryMetaDataFactory.Type type = JDBCQueryMetaDataFactory.Type.EJB_QL;
    boolean lazyResultsetLoading;
    final Map<String, String> declaredParts = new HashMap<String, String>();
    String ejbName;

    public String getMethodName() {
        return methodName;
    }

    public List<String> getMethodParams() {
        return methodParams;
    }

    public String getQuery() {
        return query;
    }

    public Class<?> getQlCompiler() {
        return qlCompiler;
    }

    public ParsedReadAhead getReadAheadMetaData() {
        return readAheadMetaData;
    }

    public JDBCQueryMetaDataFactory.Type getType() {
        return type;
    }

    public boolean isLazyResultsetLoading() {
        return lazyResultsetLoading;
    }

    public Map<String, String> getDeclaredParts() {
        return declaredParts;
    }

    public String getEjbName() {
        return ejbName;
    }
}

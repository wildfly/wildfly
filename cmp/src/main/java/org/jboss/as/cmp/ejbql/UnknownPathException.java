/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.ejbql;

/**
 * This exception is thrown when the EJB-QL parser encounters an unknown path.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class UnknownPathException extends RuntimeException {
    private final String reason;
    private final String path;
    private final String fieldName;
    private final int errorLine;
    private final int errorColumn;

    public UnknownPathException(
            String reason,
            String path,
            String fieldName,
            int errorLine,
            int errorColumn) {

        super(reason + ": at line " + errorLine + ", " +
                "column " + errorColumn + ".  " +
                "Encountered: \"" + fieldName + "\"" +
                ((path == null) ? "" : " after: \"" + path + "\""));

        this.reason = reason;
        this.path = path;
        this.fieldName = fieldName;
        this.errorLine = errorLine;
        this.errorColumn = errorColumn;
    }

    public String getReason() {
        return reason;
    }

    public String getCurrentPath() {
        return path;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int getErrorLine() {
        return errorLine;
    }

    public int getErrorColumn() {
        return errorColumn;
    }
}

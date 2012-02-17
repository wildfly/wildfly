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
package org.jboss.as.cmp.jdbc.metadata;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public final class JDBCFunctionMappingMetaData {
    private String functionName;
    private String[] sqlChunks;
    private int[] parameters;

    public JDBCFunctionMappingMetaData() {
    }

    public JDBCFunctionMappingMetaData(final String functionName, final String sql) {
        this.functionName = functionName;
        initFromString(sql);
    }

    public JDBCFunctionMappingMetaData(String functionName, String[] sqlChunks, int[] parameters) {
        this.functionName = functionName;
        this.sqlChunks = sqlChunks;
        this.parameters = parameters;
    }

    public void setFunctionName(final String functionName) {
        this.functionName = functionName;
    }

    public void setFunctionSql(final String sql) {
        initFromString(sql);
    }

    private void initFromString(final String sql) {
        ArrayList<String> chunkList = new ArrayList<String>();
        ArrayList<Integer> parameterList = new ArrayList<Integer>();

        // add a dummy chunk so we can be assured that the sql started with chunk before a number
        if (sql.charAt(0) == '?') {
            chunkList.add("");
        }
        // break the sql into chunks and parameters
        StringBuffer chunk = new StringBuffer();
        StringReader reader = new StringReader(sql);
        try {
            for (int c = reader.read(); c >= 0; c = reader.read()) {
                if (c != '?') {
                    chunk.append((char) c);
                } else {
                    chunkList.add(chunk.toString());
                    chunk = new StringBuffer();

                    // read the number
                    StringBuffer number = new StringBuffer();
                    for (int digit = reader.read(); digit >= 0; digit = reader.read()) {
                        if (Character.isDigit((char) digit)) {
                            number.append((char) digit);
                        } else {
                            if (digit >= 0) {
                                chunk.append((char) digit);
                            }
                            break;
                        }
                    }
                    if (number.length() == 0) {
                        throw new RuntimeException("Invalid parameter in function-sql: " + sql);
                    }
                    Integer parameter;
                    try {
                        parameter = new Integer(number.toString());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid parameter number in function-sql: number=" + number + " sql=" + sql);
                    }
                    parameterList.add(parameter);
                }
            }
        } catch (IOException e) {
            // will never happen because io is in memory, but required by the interface
            throw new RuntimeException("Error parsing function-sql: " + sql);
        }
        chunkList.add(chunk.toString());

        // save out the chunks
        sqlChunks = new String[chunkList.size()];
        chunkList.toArray(sqlChunks);

        // save out the parameter order
        parameters = new int[parameterList.size()];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = parameterList.get(i) - 1;
        }
    }

    public String getFunctionName() {
        return functionName;
    }

    public StringBuffer getFunctionSql(Object[] args, StringBuffer buf) {
        for (int i = 0; i < sqlChunks.length; i++) {
            if (i < parameters.length) {
                // the logic is that if there is a parameter
                // than append its chunk unless the parameter is null
                // FIXME: I am not sure it's ok for any kind of template.
                Object arg = args[parameters[i]];
                if (arg != null) {
                    buf.append(sqlChunks[i]);
                    buf.append(arg);
                }
            } else {
                // this is tail
                buf.append(sqlChunks[i]);
            }
        }
        return buf;
    }


}

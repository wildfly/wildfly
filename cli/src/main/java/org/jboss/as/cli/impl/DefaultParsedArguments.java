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
package org.jboss.as.cli.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.cli.ParsedArguments;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultParsedArguments implements ParsedArguments {

    /** current command's arguments */
    private String argsStr;
    /** named command arguments */
    private Map<String, String> namedArgs;
    /** other command arguments */
    private List<String> otherArgs;

    public void reset(String args) {
        argsStr = args;
        namedArgs = null;
        otherArgs = null;
    }

    public void parse(String args) {
        argsStr = args;
        namedArgs = null;
        otherArgs = null;
        parseArgs();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgumentsString()
     */
    @Override
    public String getArgumentsString() {
        return argsStr;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#hasArguments()
     */
    @Override
    public boolean hasArguments() {
        if(otherArgs == null) {
            parseArgs();
        }
        return !namedArgs.isEmpty() || !otherArgs.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#hasArgument(java.lang.String)
     */
    @Override
    public boolean hasArgument(String argName) {
        if(namedArgs == null) {
            parseArgs();
        }
        return namedArgs.containsKey(argName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgument(java.lang.String)
     */
    @Override
    public String getArgument(String argName) {
        if(namedArgs == null) {
            parseArgs();
        }
        return namedArgs.get(argName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgumentNames()
     */
    @Override
    public Set<String> getArgumentNames() {
        if(namedArgs == null) {
            parseArgs();
        }
        return namedArgs.keySet();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getOtherArguments()
     */
    @Override
    public List<String> getOtherArguments() {
        if(otherArgs == null) {
            parseArgs();
        }
        return otherArgs;
    }

    private void parseArgs() {
        namedArgs = null;
        otherArgs = null;
        if (argsStr != null && !argsStr.isEmpty()) {
            String[] arr = argsStr.split("\\s+");
            if (arr.length > 0) {
                for (int i = 0; i < arr.length; ++i) {
                    String arg = arr[i];
                    if (arg.charAt(0) == '-') {
                        final String dashedArg = arg;
/*                        if (arg.length() > 1 && arg.charAt(1) == '-') {
                            dashedArg = arg.substring(2);
                        } else {
                            dashedArg = arg.substring(1);
                        }
*/
                        if (dashedArg.length() > 0) {
                            if(namedArgs == null) {
                                namedArgs = new HashMap<String, String>();
                            }

                            int equalsIndex = dashedArg.indexOf('=');
                            if(equalsIndex > 0 && equalsIndex < dashedArg.length() - 1 && dashedArg.indexOf(equalsIndex + 1, '=') < 0) {
                                final String name = dashedArg.substring(0, equalsIndex).trim();
                                final String value = dashedArg.substring(equalsIndex + 1).trim();
                                if (namedArgs == null) {
                                    namedArgs = new HashMap<String, String>();
                                }
                                namedArgs.put(name, value);
                            } else {
                                namedArgs.put(dashedArg, null);
                            }
/*                        } else {
                            if(otherArgs == null) {
                                otherArgs = new ArrayList<String>();
                            }
                            otherArgs.add(arg);
*/                        }
                    } else {
                        if(otherArgs == null) {
                            otherArgs = new ArrayList<String>();
                        }
                        otherArgs.add(arg);
//TODO this check for name=value should go away
                        int equalsIndex = arg.indexOf('=');
                        if(equalsIndex > 0 && equalsIndex < arg.length() - 1 && arg.indexOf(equalsIndex + 1, '=') < 0) {
                            final String name = arg.substring(0, equalsIndex).trim();
                            final String value = arg.substring(equalsIndex + 1).trim();
                            if (namedArgs == null) {
                                namedArgs = new HashMap<String, String>();
                            }
                            namedArgs.put(name, value);
                        }
                    }
                }

                if(otherArgs != null) {
                    otherArgs = Collections.unmodifiableList(otherArgs);
                }
            }
        }

        if(namedArgs == null) {
            namedArgs = Collections.emptyMap();
        }
        if(otherArgs == null) {
            otherArgs = Collections.emptyList();
        }
    }
}

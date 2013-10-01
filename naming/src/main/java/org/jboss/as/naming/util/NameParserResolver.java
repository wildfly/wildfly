/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.util;

import java.util.Map;

import javax.naming.Name;

/**
 * Interface which defines contract for object capable of matching name to proper name parser.
 * Note that, NameParser instance must return implementation of CompositeName. However how CompositeName parts are
 * determined, remains a mystery shrouded by NameParser interface.
 * Implementation must have no-arg constructor.
 * @author baranowb
 */
public interface NameParserResolver {

    /**
     * Key for default parser.
     */
    String KEY_DEFAULT="default";
    String KEY_RMI="rmi";
    /**
     *
     * @param name - name which is supposed to be parsed by object instance returned by this method.
     * @param parserMap - map of prefixes/keys to instances of {@link javax.naming.NameParser}. Key values depend on implementation of this interface.
     * @param defaultNameParser - this parser will be used in case - matcher did not find and NameParser and there is no entry under {@link #KEY_DEFAULT}.
     * @return
     */
    javax.naming.NameParser findNameParser(String name, Map<String, javax.naming.NameParser> parserMap, javax.naming.NameParser defaultNameParser);
    /**
    *
    * @param name - name which is supposed to be parsed by object instance returned by this method.
    * @param parserMap - map of prefixes/keys to instances of {@link javax.naming.NameParser}. Key values depend on implementation of this interface.
    * @param defaultNameParser - this parser will be used in case - matcher did not find and NameParser and there is no entry under {@link #KEY_DEFAULT}.
    * @return
    */
    javax.naming.NameParser findNameParser(Name name, Map<String, javax.naming.NameParser> parserMap, javax.naming.NameParser defaultNameParser);
}

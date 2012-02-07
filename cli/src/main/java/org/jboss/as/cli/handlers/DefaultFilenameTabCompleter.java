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
package org.jboss.as.cli.handlers;

import java.io.File;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.EscapeSelector;
import org.jboss.as.cli.Util;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultFilenameTabCompleter extends FilenameTabCompleter {

    private static final EscapeSelector ESCAPE_SELECTOR = new EscapeSelector() {
       @Override
       public boolean isEscape(char ch) {
           return ch == '\\' || ch == ' ' || ch == '"';
       }
   };

   public DefaultFilenameTabCompleter(CommandContext ctx) {
       super(ctx);
   }

   /* (non-Javadoc)
    * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext,
    * java.lang.String, int, java.util.List)
    */
   @Override
   public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

       int result = getCandidates(buffer, candidates);

       int correction = 0;
       if(buffer.length() > 0) {
           final int lastSeparator = buffer.lastIndexOf(File.separatorChar);
           if(lastSeparator > 0) {
               final String path = buffer.substring(0, lastSeparator);
               final String escaped = Util.escapeString(path, ESCAPE_SELECTOR);
               correction = escaped.length() - path.length();
           }
       }

       if(candidates.size() == 1) {
           candidates.set(0, Util.escapeString(candidates.get(0), ESCAPE_SELECTOR));
       } else {
           Util.sortAndEscape(candidates, ESCAPE_SELECTOR);
       }
       return result + correction;
   }

   @Override
   protected boolean startsWithRoot(String path) {
       return path.startsWith(File.separator);
   }
}
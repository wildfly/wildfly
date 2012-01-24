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
import org.jboss.as.cli.Util;

/**
 *
 * @author Alexey Loubyansky
 */
public class WindowsFilenameTabCompleter extends FilenameTabCompleter {


   public WindowsFilenameTabCompleter(CommandContext ctx) {
        super(ctx);
    }

/* (non-Javadoc)
    * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext,
    * java.lang.String, int, java.util.List)
    */
   @Override
   public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

       boolean openQuote = false;
       boolean dontCloseQuote = false;
       if(buffer.length() >= 2 && buffer.charAt(0) == '"') {
           int lastQuote = buffer.lastIndexOf('"');
           if(lastQuote >= 0) {
               StringBuilder buf = new StringBuilder();
               buf.append(buffer.substring(1, lastQuote));
               if(lastQuote != buffer.length() - 1) {
                   buf.append(buffer.substring(lastQuote + 1));
               }
               buffer = buf.toString();
               openQuote = true;
               dontCloseQuote = cursor <= lastQuote;
           }
       }

       int result = getCandidates(buffer, candidates);

       final String path;
       if(buffer.length() == 0) {
           path = null;
       } else {
           final int lastSeparator = buffer.lastIndexOf(File.separatorChar);
           if(lastSeparator > 0) {
               path = buffer.substring(0, lastSeparator + 1);
           } else {
               path = null;
           }
       }

       if(path != null && !openQuote) {
           openQuote = path.indexOf(' ') >= 0;
      }

       if(candidates.size() == 1) {
           final String candidate = candidates.get(0);
           if(!openQuote) {
               openQuote = candidate.indexOf(' ') >= 0;
           }
           if(openQuote) {
               StringBuilder buf = new StringBuilder();
               buf.append('"');
               if(path != null) {
                   buf.append(path);
               }
               buf.append(candidate);
               if(!dontCloseQuote) {
                   buf.append('"');
               }
               candidates.set(0, buf.toString());
           }
       } else {
           final String common = Util.getCommonStart(candidates);
           if(!openQuote && common != null) {
               openQuote = common.indexOf(' ') >= 0;
           }
           if(openQuote) {
               for(int i = 0; i < candidates.size(); ++i) {
                   StringBuilder buf = new StringBuilder();
                   buf.append('"');
                   if(path != null) {
                       buf.append(path);
                   }

                   if(common == null) {
                       if(!dontCloseQuote) {
                           buf.append('"');
                       }
                       buf.append(candidates.get(i));
                   } else {
                       buf.append(common);
                       if(!dontCloseQuote) {
                           buf.append('"');
                       }
                       buf.append(candidates.get(i).substring(common.length()));
                   }

                   candidates.set(i, buf.toString());
               }
           }
       }

       if(openQuote) {
           return 0;
       }

       return result;
   }

   @Override
   protected boolean startsWithRoot(String path) {
       return path.contains(":\\");
   }
}
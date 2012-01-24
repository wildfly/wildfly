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
import org.jboss.as.cli.CommandLineCompleter;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FilenameTabCompleter implements CommandLineCompleter {

    private final CommandContext ctx;

    public FilenameTabCompleter(CommandContext ctx) {
        if(ctx == null) {
            throw new IllegalArgumentException("ctx is null");
        }
        this.ctx = ctx;
    }

    protected abstract boolean startsWithRoot(String path);

    protected int getCandidates(String buffer, List<String> candidates) {

        final String translated = translatePath(buffer);

        final File f = new File(translated);
        final File dir;
        if (translated.endsWith(File.separator)) {
            dir = f;
        } else {
            dir = f.getParentFile();
        }

        final File[] entries = (dir == null) ? new File[0] : dir.listFiles();
        return matchFiles(buffer, translated, entries, candidates);
    }

    public String translatePath(String path) {
        final String translated;
        // special character: ~ maps to the user's home directory
        if (path.startsWith("~" + File.separator)) {
            translated = System.getProperty("user.home") + path.substring(1);
        } else if (path.startsWith("~")) {
            translated = new File(System.getProperty("user.home"))
                    .getParentFile().getAbsolutePath();
        } else if (!startsWithRoot(path)) {
            translated = ctx.getCurrentDir().getAbsolutePath() + File.separator + path;
        } else {
            translated = path;
        }
        return translated;
    }

   private static String unescapeName(String name) {
       for(int i = 0; i < name.length(); ++i) {
           char ch = name.charAt(i);
           if(ch == '\\') {
               StringBuilder builder = new StringBuilder();
               builder.append(name, 0, i);
               boolean escaped = true;
               for(int j = i + 1; j < name.length(); ++j) {
                   ch = name.charAt(j);
                   if(escaped) {
                       builder.append(ch);
                       escaped = false;
                   } else if(ch == '\\') {
                       escaped = true;
                   } else {
                       builder.append(ch);
                   }
               }
               return builder.toString();
           }
       }
       return name;
   }

   /**
    * Match the specified <i>buffer</i> to the array of <i>entries</i> and
    * enter the matches into the list of <i>candidates</i>. This method can be
    * overridden in a subclass that wants to do more sophisticated file name
    * completion.
    *
    * @param buffer
    *            the untranslated buffer
    * @param translated
    *            the buffer with common characters replaced
    * @param entries
    *            the list of files to match
    * @param candidates
    *            the list of candidates to populate
    *
    * @return the offset of the match
    */
   protected int matchFiles(String buffer, String translated, File[] entries, List<String> candidates) {
       if (entries == null) {
           return -1;
       }

       int matches = 0;

       // first pass: just count the matches
       for (int i = 0; i < entries.length; i++) {
           if (entries[i].getAbsolutePath().startsWith(translated)) {
               matches++;
           }
       }

       for (int i = 0; i < entries.length; i++) {
           if (entries[i].getAbsolutePath().startsWith(translated)) {

               final String name;
               if(matches == 1 && entries[i].isDirectory()) {
                   name = entries[i].getName() + File.separatorChar;
               } else {
                   name = entries[i].getName();
               }
               candidates.add(name);
           }
       }

       final int index = buffer.lastIndexOf(File.separatorChar);
       return index + 1 /* - separator character */;
   }

   public static void main(String[] args) throws Exception {
       String name = "../../../../my\\ dir/";
//        System.out.println(name);
//        name = escapeName(name);
//        System.out.println(name);
       System.out.println(unescapeName(name));
   }
}
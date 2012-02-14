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
package org.jboss.as.server.deployment.scanner;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An extensible filter for VFS files.  Three arrays are maintained for checking: a prefix, suffix, and match array.  If
 * the filename starts with any of the prefixes, ends with any of the suffixes, or exactly matches any of the matches,
 * then the accepts method will return false.
 * <p/>
 * NOTE: the arrays *must* be sorted for the string matching to work, and suffixes use the 'reverseComparator'
 *
 * @author Scott.Stark@jboss.org
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ExtensibleFilter implements FileFilter {

    /**
     * Compare the strings backwards.  This assists in suffix comparisons.
     */
    private static final Comparator<String> reverseComparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            int idx1 = o1.length();
            int idx2 = o2.length();
            int comp = 0;
            while (comp == 0 && idx1 > 0 && idx2 > 0)
                comp = o1.charAt(--idx1) - o2.charAt(--idx2);
            return (comp == 0) ? (idx1 - idx2) : comp;
        }
    };

    /**
     * the default prefix list
     */
    private static final String[] DEFAULT_PREFIXES =
            { "#", "%", ",", ".", "_$" };

    /**
     * the default suffix list
     */
    private static final String[] DEFAULT_SUFFIXES =
            { "#", "$", "%", "~", ",v", ".BAK", ".bak", ".old", ".orig", ".tmp", ".rej", ".sh", ".txt" };

    /**
     * the default matches list
     */
    private static final String[] DEFAULT_MATCHES =
            { ".make.state", ".nse_depinfo", "CVS", "CVS.admin", "RCS", "RCSLOG", "SCCS", "TAGS", "core", "tags" };

    /**
     * The list of disallowed suffixes, sorted using reverse values
     */
    private final List<String> suffixes;

    /**
     * The sorted list of disallowed prefixes
     */
    private final List<String> prefixes;

    /**
     * The sorted list of disallowed values
     */
    private final List<String> matches;

    /**
     * Use the default values for suffixes, prefixes, and matches
     */
    public ExtensibleFilter() {
        this(DEFAULT_MATCHES, DEFAULT_PREFIXES, DEFAULT_SUFFIXES);
    }

    /**
     * Create using a custom set of matches, prefixes, and suffixes.  If any of these arrays are null, then the
     * corresponding default will be substituted.
     *
     * @param matches the matches
     * @param prefixes the prefixes
     * @param suffixes the suffixes
     */
    public ExtensibleFilter(String[] matches, String[] prefixes, String[] suffixes) {
        if (matches == null)
            matches = DEFAULT_MATCHES;
        Arrays.sort(matches);
        this.matches = new ArrayList<String>(Arrays.asList(matches));
        if (prefixes == null)
            prefixes = DEFAULT_PREFIXES;
        Arrays.sort(prefixes);
        this.prefixes = new ArrayList<String>(Arrays.asList(prefixes));
        if (suffixes == null)
            suffixes = DEFAULT_SUFFIXES;
        Arrays.sort(suffixes, reverseComparator);
        this.suffixes = new ArrayList<String>(Arrays.asList(suffixes));
    }

    public void addPrefix(String prefix) {
        prefixes.add(prefix);
        Collections.sort(prefixes);
    }

    public void addPrefixes(String[] prefixes) {
        this.prefixes.addAll(Arrays.asList(prefixes));
        Collections.sort(this.prefixes);
    }

    public void delPrefix(String prefix) {
        prefixes.remove(prefix);
    }

    public void delPrefixes(String[] prefixes) {
        this.prefixes.removeAll(Arrays.asList(prefixes));
        Collections.sort(this.prefixes);
    }

    public void addSuffix(String suffix) {
        suffixes.add(suffix);
        Collections.sort(suffixes, reverseComparator);
    }

    public void addSuffixes(String[] suffixes) {
        this.suffixes.addAll(Arrays.asList(suffixes));
        Collections.sort(this.suffixes, reverseComparator);
    }

    public void delSuffix(String suffix) {
        suffixes.remove(suffix);
    }

    public void delSuffixes(String[] suffixes) {
        this.suffixes.removeAll(Arrays.asList(suffixes));
        Collections.sort(this.suffixes, reverseComparator);
    }

    public String[] getSuffixes() {
        String[] tmp = new String[suffixes.size()];
        suffixes.toArray(tmp);
        return tmp;
    }

    public void setSuffixes(String[] suffixes) {
        Arrays.sort(suffixes, reverseComparator);
        this.suffixes.clear();
        this.suffixes.addAll(Arrays.asList(suffixes));
    }

    public String[] getPrefixes() {
        String[] tmp = new String[prefixes.size()];
        prefixes.toArray(tmp);
        return tmp;
    }

    public void setPrefixes(String[] prefixes) {
        Arrays.sort(prefixes);
        this.prefixes.clear();
        this.prefixes.addAll(Arrays.asList(prefixes));
    }

    public String[] getMatches() {
        String[] tmp = new String[matches.size()];
        matches.toArray(tmp);
        return tmp;
    }

    public void setMatches(String[] matches) {
        Arrays.sort(matches);
        this.matches.clear();
        this.matches.addAll(Arrays.asList(matches));
    }

    /**
     * If the filename matches any string in the prefix, suffix, or matches array, return false.  Perhaps a bit of
     * overkill, but this method operates in log(n) time, where n is the size of the arrays.
     *
     * @param file The file to be tested
     *
     * @return <code>false</code> if the filename matches any of the prefixes, suffixes, or matches.
     */
    @Override
    public boolean accept(File file) {
        String name = file.getName();
        // check exact match
        int index = Collections.binarySearch(matches, name);
        if (index >= 0)
            return false;
        // check prefix
        index = Collections.binarySearch(prefixes, name);
        if (index >= 0)
            return false;
        if (index < -1) {
            // The < 0 index gives the first index greater than name
            int firstLessIndex = -2 - index;
            String prefix = prefixes.get(firstLessIndex);
            // If name starts with an ignored prefix ignore name
            if (name.startsWith(prefix))
                return false;
        }
        // check suffix
        index = Collections.binarySearch(suffixes, name, reverseComparator);
        if (index >= 0)
            return false;
        if (index < -1) {
            // The < 0 index gives the first index greater than name
            int firstLessIndex = -2 - index;
            String suffix = suffixes.get(firstLessIndex);
            // If name ends with an ignored suffix ignore name
            if (name.endsWith(suffix))
                return false;
        }
        // everything checks out.
        return true;
    }
}

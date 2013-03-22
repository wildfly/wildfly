/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jdr.vfs;

import org.jboss.as.jdr.util.WildcardPattern;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.util.MatchAllVirtualFileFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author csams@redhat.com
 *         Date: 11/23/12
 */
public class Filters {

    public static final VirtualFileFilter TRUE = MatchAllVirtualFileFilter.INSTANCE;

    public static VirtualFileFilter not(final VirtualFileFilter filter) {
        return new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                return !filter.accepts(file);
            }
        };
    }

    public static VirtualFileFilter and(final VirtualFileFilter... filters) {
        return new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                for(VirtualFileFilter f: filters) {
                    if(!f.accepts(file)){
                        return false;
                    }
                }
                return true;
            }
        };
    }

    public static VirtualFileFilter or(final VirtualFileFilter... filters) {
        return new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                for(VirtualFileFilter f: filters) {
                    if(f.accepts(file)){
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static VirtualFileFilter wildcard(final String p){
        return new VirtualFileFilter() {
            private WildcardPattern pattern = new WildcardPattern(p);
            @Override
            public boolean accepts(VirtualFile file) {
                return pattern.matches(file.getPathName());
            }
        };
    }

    public static BlacklistFilter wildcardBlackList() {
        return new WildcardBlacklistFilter();
    }

    public static BlacklistFilter wildcardBlacklistFilter(final String... patterns){
        return new WildcardBlacklistFilter(patterns);
    }

    public static BlacklistFilter regexBlackList() {
        return new RegexBlacklistFilter();
    }

    public static BlacklistFilter regexBlackList(String... patterns) {
        return new RegexBlacklistFilter(patterns);
    }

    public static VirtualFileFilter suffix(final String s){
        return new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                return file.getPathName().endsWith(s);
            }
        };
    }

    public interface BlacklistFilter extends VirtualFileFilter {
        void add(final String... patterns);
    }

    private static class WildcardBlacklistFilter implements BlacklistFilter {

        private final List<WildcardPattern> patterns;

        public WildcardBlacklistFilter() {
            patterns = new ArrayList<WildcardPattern>();
            patterns.add(new WildcardPattern("*-users.properties"));
        }

        public WildcardBlacklistFilter(final String... patterns) {
            this.patterns = new ArrayList<WildcardPattern>(patterns.length);
            add(patterns);
        }

        @Override
        public boolean accepts(VirtualFile file) {
            for(WildcardPattern p: this.patterns){
                if(p.matches(file.getName())){
                    return false;
                }
            }
            return true;
        }

        public void add(final String... patterns){
            for(String p: patterns) {
                this.patterns.add(new WildcardPattern(p));
            }
        }
    }

    private static class RegexBlacklistFilter implements BlacklistFilter {
        private final List<Pattern> patterns;

        public RegexBlacklistFilter(){
            this.patterns = Arrays.asList(Pattern.compile(".*-users.properties"));
        }

        public RegexBlacklistFilter(final String... patterns){
            this.patterns = new ArrayList<Pattern>(patterns.length);
            add(patterns);
        }

        @Override
        public boolean accepts(final VirtualFile file) {
            for(Pattern p: this.patterns){
                if(p.matcher(file.getName()).matches()){
                    return false;
                }
            }
            return true;
        }

        public void add(final String... patterns){
            for(String p: patterns){
                this.patterns.add(Pattern.compile(p));
            }
        }
    }
}

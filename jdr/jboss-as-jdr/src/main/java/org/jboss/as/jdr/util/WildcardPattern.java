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
package org.jboss.as.jdr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Simple wildcard matcher for *
 * @author csams@redhat.com
 *         Date: 11/6/12
 */
public class WildcardPattern {

    private static final String WILDCARDS = "*";

    private final String[] tokens;

    public WildcardPattern(String pattern){

        if (!pattern.startsWith("*")) {
            pattern = "*" + pattern;
        }

        StringTokenizer st = new StringTokenizer(pattern, WILDCARDS, true);
        List<String> t = new ArrayList<String>();
        while(st.hasMoreTokens()){
            t.add(st.nextToken());
        }
        tokens = t.toArray(new String[t.size()]);
    }

    public boolean matches(final String target){
        int targetIdx = 0;
        int targetEnd = target.length();

        int tokenIdx = 0;
        int tokenEnd = tokens.length;

        while(tokenIdx < tokenEnd && targetIdx < targetEnd && targetIdx > -1){
            if("*".equals(tokens[tokenIdx])){
                if(tokenIdx == (tokenEnd - 1)){
                    targetIdx = targetEnd;
                    tokenIdx = tokenEnd;
                } else {
                    targetIdx = target.indexOf(tokens[tokenIdx+1], targetIdx);
                    tokenIdx++;
                }
            }
            else{
                if(target.substring(targetIdx).startsWith(tokens[tokenIdx])){
                    targetIdx += tokens[tokenIdx].length();
                    tokenIdx++;
                } else {
                    targetIdx = -1;
                    break;
                }
            }
        }
        return (tokenIdx == tokenEnd && targetIdx == targetEnd);

    }

    public static boolean matches(final String pattern, final String target) {
        return new WildcardPattern(pattern).matches(target);
    }
}

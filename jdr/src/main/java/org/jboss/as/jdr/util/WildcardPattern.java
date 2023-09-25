/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

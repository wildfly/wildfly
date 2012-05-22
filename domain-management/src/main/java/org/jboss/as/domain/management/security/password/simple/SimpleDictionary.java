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

package org.jboss.as.domain.management.security.password.simple;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.domain.management.security.password.Dictionary;

/**
 * @author baranowb
 *
 */
public class SimpleDictionary implements Dictionary {

    protected Set<String> words = new TreeSet<String>();

    public SimpleDictionary() {
        InputStream is = Dictionary.class.getResourceAsStream("dictionary.properties");
        if(is!=null){
            this.init(is);
        }else{
            //log?
        }
    }

    public SimpleDictionary(InputStream is) {
        this.init(is);
    }

    protected void init(InputStream is) {
        try {
            Properties props = new Properties();
            props.load(is);
            Iterator it = props.keySet().iterator();
            while (it.hasNext()) {
                String word = (String) it.next();
                words.add(word);
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.domain.management.security.password.Dictionary#dictionaryScore(java.lang.String)
     */
    @Override
    public int dictionarySequence(String password) {
        // simple search.
        if (password.length() == 0) {
            return 0;
        }
        if (words.contains(password)) {
            return password.length();
        } else {
            int higher = 0;
            for (int index = 1; index < password.length(); index++) {
                String tmp = password.substring(index);
                int count = dictionarySequence(tmp);
                if (count > 0 && higher<count) {
                    higher = count;
                }
            }

            for (int index = password.length() - 1; 1 < index; index--) {
                String tmp = password.substring(0, index);
                int count = dictionarySequence(tmp);
                if (count > 0 && higher<count) {
                    higher = count;
                }
            }
            return higher;
        }
    }
}
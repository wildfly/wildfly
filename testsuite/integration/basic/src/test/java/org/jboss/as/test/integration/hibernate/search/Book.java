/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.solr.analysis.ASCIIFoldingFilterFactory;
import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.LengthFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.PatternTokenizerFactory;
import org.apache.solr.analysis.PorterStemFilterFactory;
import org.apache.solr.analysis.ShingleFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.StopFilterFactory;
import org.apache.solr.analysis.WordDelimiterFilterFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

@Entity
@Indexed
@AnalyzerDefs({
   @AnalyzerDef(name = "customanalyzer",
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
      @TokenFilterDef(factory = ASCIIFoldingFilterFactory.class),
               @TokenFilterDef(factory = LowerCaseFilterFactory.class),
               @TokenFilterDef(factory = StopFilterFactory.class, params = {
                  @Parameter(name = "words",
                        value = "stopwordslist.properties"),
                  @Parameter(name = "resource_charset", value = "UTF-8"),
                  @Parameter(name = "ignoreCase", value = "true")
               }),
               @TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = {
                  @Parameter(name = "language", value = "English")
               })
   }),

   @AnalyzerDef(name = "pattern_analyzer",
         tokenizer = @TokenizerDef(factory = PatternTokenizerFactory.class, params = {
            @Parameter(name = "pattern", value = ",")
         })),

   @AnalyzerDef(name = "standard_analyzer",
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
            @TokenFilterDef(factory = StandardFilterFactory.class)
         }),
   @AnalyzerDef(name = "html_standard_analyzer",
         charFilters = {
            @CharFilterDef(factory = HTMLStripCharFilterFactory.class)
         },
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
            @TokenFilterDef(factory = StandardFilterFactory.class)
         }),

   @AnalyzerDef(name = "html_whitespace_analyzer",
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         charFilters = {
            @CharFilterDef(factory = HTMLStripCharFilterFactory.class)
         }),

   @AnalyzerDef(name = "length_analyzer",
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
            @TokenFilterDef(factory = LengthFilterFactory.class, params = {
               @Parameter(name = "min", value = "3"),
               @Parameter(name = "max", value = "5")
            })
         }),

   @AnalyzerDef(name = "porter_analyzer",
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
            @TokenFilterDef(factory = PorterStemFilterFactory.class)
         }),

   @AnalyzerDef(name = "word_analyzer",
         charFilters = {
            @CharFilterDef(factory = HTMLStripCharFilterFactory.class)
         },
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
            @TokenFilterDef(factory = WordDelimiterFilterFactory.class, params = {
               @Parameter(name = "splitOnCaseChange", value = "1")
            })
         }),

   @AnalyzerDef(name = "shingle_analyzer",
         charFilters = {
            @CharFilterDef(factory = HTMLStripCharFilterFactory.class)
         },
         tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
         filters = {
            @TokenFilterDef(factory = ShingleFilterFactory.class)
         })
})
public class Book {

   @Id @GeneratedValue
   Long id;

   @Field
   String title;

}

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
package org.jboss.as.test.integration.hibernate.search.cdi.beans.bridge;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.InternationalizedValue;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.Language;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.LocalizationService;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.model.EntityWithCDIAwareBridges;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class InternationalizedValueClassBridgeImpl implements InternationalizedValueClassBridge {

    @Inject
    private LocalizationService localizationService;

    @Override
    public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
        builder.field(name, FieldType.STRING);
    }

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        EntityWithCDIAwareBridges entity = (EntityWithCDIAwareBridges) value;
        InternationalizedValue internationalizedValue = entity.getInternationalizedValue();
        for (Language language : Language.values()) {
            String localizedValue = localizationService.localize(internationalizedValue, language);
            luceneOptions.addFieldToDocument(name, localizedValue, document);
        }
    }

}
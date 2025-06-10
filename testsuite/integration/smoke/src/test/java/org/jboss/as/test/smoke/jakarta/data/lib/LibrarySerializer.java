/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;


/** Used for simple serialization of a Library when we don't want all the fields. */
public class LibrarySerializer  implements JsonbSerializer<Library> {
    @Override
    public void serialize(Library library, JsonGenerator generator, SerializationContext ctx) {
        generator.write(library.getName());
    }
}

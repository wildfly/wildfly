/*
 * Copyright 2022 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.jms;

import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.utils.Base64;

/**
 * Temporary class to be removed once we upgrade to Artemis 2.20
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
public class JsonUtil {

    static String toJSON(final Map<String, Object>[] messages) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (Map<String, Object> message : messages) {
            array.add(toJsonObject(message));
        }
        return array.build().toString();
    }

    private static JsonObject toJsonObject(Map<String, ?> map) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            addToObject(String.valueOf(entry.getKey()), entry.getValue(), jsonObjectBuilder);
        }
        return jsonObjectBuilder.build();
    }

    private static void addToObject(final String key, final Object param, final JsonObjectBuilder jsonObjectBuilder) {
        if (param instanceof Integer) {
            jsonObjectBuilder.add(key, (Integer) param);
        } else if (param instanceof Long) {
            jsonObjectBuilder.add(key, (Long) param);
        } else if (param instanceof Double) {
            jsonObjectBuilder.add(key, (Double) param);
        } else if (param instanceof String) {
            jsonObjectBuilder.add(key, (String) param);
        } else if (param instanceof Boolean) {
            jsonObjectBuilder.add(key, (Boolean) param);
        } else if (param instanceof Map) {
            JsonObject mapObject = toJsonObject((Map<String, Object>) param);
            jsonObjectBuilder.add(key, mapObject);
        } else if (param instanceof Short) {
            jsonObjectBuilder.add(key, (Short) param);
        } else if (param instanceof Byte) {
            jsonObjectBuilder.add(key, ((Byte) param).shortValue());
        } else if (param instanceof Number) {
            jsonObjectBuilder.add(key, ((Number) param).doubleValue());
        } else if (param instanceof SimpleString) {
            jsonObjectBuilder.add(key, param.toString());
        } else if (param == null) {
            jsonObjectBuilder.addNull(key);
        } else if (param instanceof byte[]) {
            JsonArrayBuilder byteArrayObject = toJsonArrayBuilder((byte[]) param);
            jsonObjectBuilder.add(key, byteArrayObject);
        } else if (param instanceof Object[]) {
            final JsonArrayBuilder objectArrayBuilder = Json.createArrayBuilder();
            for (Object parameter : (Object[]) param) {
                addToArray(parameter, objectArrayBuilder);
            }
            jsonObjectBuilder.add(key, objectArrayBuilder);
        } else {
            jsonObjectBuilder.add(key, param.toString());
        }
    }

    private static void addToArray(final Object param, final JsonArrayBuilder jsonArrayBuilder) {
      if (param instanceof Integer) {
         jsonArrayBuilder.add((Integer) param);
      } else if (param instanceof Long) {
         jsonArrayBuilder.add((Long) param);
      } else if (param instanceof Double) {
         jsonArrayBuilder.add((Double) param);
      } else if (param instanceof String) {
         jsonArrayBuilder.add((String) param);
      } else if (param instanceof Boolean) {
         jsonArrayBuilder.add((Boolean) param);
      } else if (param instanceof Map) {
         JsonObject mapObject = toJsonObject((Map<String, Object>) param);
         jsonArrayBuilder.add(mapObject);
      } else if (param instanceof Short) {
         jsonArrayBuilder.add((Short) param);
      } else if (param instanceof Byte) {
         jsonArrayBuilder.add(((Byte) param).shortValue());
      } else if (param instanceof Number) {
         jsonArrayBuilder.add(((Number)param).doubleValue());
      } else if (param == null) {
         jsonArrayBuilder.addNull();
      } else if (param instanceof byte[]) {
         JsonArrayBuilder byteArrayObject = toJsonArrayBuilder((byte[]) param);
         jsonArrayBuilder.add(byteArrayObject);
      } else if (param instanceof CompositeData[]) {
         JsonArrayBuilder innerJsonArray = Json.createArrayBuilder();
         for (Object data : (CompositeData[])param) {
            String s = Base64.encodeObject((CompositeDataSupport) data);
            innerJsonArray.add(s);
         }
         JsonObjectBuilder jsonObject = Json.createObjectBuilder();
         jsonObject.add(CompositeData.class.getName(), innerJsonArray);
         jsonArrayBuilder.add(jsonObject);
      } else if (param instanceof Object[]) {
         JsonArrayBuilder objectArrayBuilder = Json.createArrayBuilder();
         for (Object parameter : (Object[])param) {
            addToArray(parameter, objectArrayBuilder);
         }
         jsonArrayBuilder.add(objectArrayBuilder);
      } else {
         jsonArrayBuilder.add(param.toString());
      }
   }

    private static JsonArrayBuilder toJsonArrayBuilder(byte[] byteArray) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        if (byteArray != null) {
            for (int i = 0; i < byteArray.length; i++) {
                jsonArrayBuilder.add(((Byte) byteArray[i]).shortValue());
            }
        }
        return jsonArrayBuilder;
    }

}

/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.KeyValue;

public final class SignalUtil {
    private SignalUtil() {
    }

    public static String fromByteString(ByteString bs) {
        return HexFormat.of().formatHex(bs.toByteArray());
    }

    public static Map<String, String> fromKeyValueList(List<KeyValue> kvList) {
        return kvList.stream().collect(Collectors.toMap(KeyValue::getKey, kv -> kv.getValue().toString(),
                (a, b) -> b, HashMap::new));
    }

}

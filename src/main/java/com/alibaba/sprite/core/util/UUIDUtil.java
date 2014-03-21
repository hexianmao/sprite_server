/*
 * Copyright 1999-2014 Alibaba Group.
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
package com.alibaba.sprite.core.util;

import java.util.UUID;

/**
 * @author xianmao.hexm
 */
public final class UUIDUtil {

    private static final char[] b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static String newKey() {
        UUID uuid = UUID.randomUUID();
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        char[] ca = new char[22];
        for (int i = 0; i < 22; i++) {
            if (i < 10) {
                int x = 58 - 6 * i;
                ca[i] = b64[(byte) ((mostSigBits >>> x) & 0x3F)];
            } else if (i == 10) {
                ca[i] = b64[(byte) (((mostSigBits << 2) | (leastSigBits >>> 62)) & 0x3F)];
            } else if (i == 21) {
                ca[i] = b64[(byte) ((leastSigBits << 4) & 0x3F)];
            } else {
                int x = 122 - 6 * i;
                ca[i] = b64[(byte) ((leastSigBits >>> x) & 0x3F)];
            }
        }
        return new String(ca);
    }

}

/*
 * Copyright 2023 zoukang, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.tmzk1005.formatter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * collect a batch of files formats results, group by {@code FormatResult}
 */
public class FormatResults {

    private final Set<String> unchanged = new ConcurrentSkipListSet<>();

    private final Set<String> succeed = new ConcurrentSkipListSet<>();

    private final Map<String, String> failed = new ConcurrentHashMap<>();

    /**
     * add file to {@code FormatResult.UNCHANGED} group
     *
     * @param fileName file name
     */
    public void addUnchangedFile(String fileName) {
        unchanged.add(fileName);
    }

    /**
     * add file to {@code FormatResult.SUCCEED} group
     *
     * @param fileName file name
     */
    public void addSucceed(String fileName) {
        succeed.add(fileName);
    }

    /**
     * add file to {@code FormatResult.FAILED} group
     *
     * @param fileName file name
     * @param reason   format failed reason
     */
    public void addFailed(String fileName, String reason) {
        failed.put(fileName, reason);
    }

    /**
     * get unchanged file collection
     *
     * @return Set of String, file names
     */
    public Set<String> getUnchanged() {
        return unchanged;
    }

    /**
     * get success formatted file collection
     *
     * @return Set of String, file names
     */
    public Set<String> getSucceed() {
        return succeed;
    }

    /**
     * get format failed file collection
     *
     * @return Map of String to string, file name to failed reason
     */
    public Map<String, String> getFailed() {
        return failed;
    }

}

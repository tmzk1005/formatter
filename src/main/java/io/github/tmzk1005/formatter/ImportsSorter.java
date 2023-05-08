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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Util class to sort import lines of java source code file
 */
public class ImportsSorter {

    private static final String KEYWORD_IMPORT_PREFIX = "import ";

    private static final String PREFIX_STATIC = KEYWORD_IMPORT_PREFIX + "static ";

    private static final String LINE_SEP = "\n";

    private List<String> importOrder;

    /**
     * create ImportsSorter instance with specified configuration
     *
     * @param importOrder List of unduplicated strings of package name prefix, blank "" means
     *                    others, and "#" means all static imports.
     */
    public ImportsSorter(List<String> importOrder) {
        this.importOrder = importOrder;
    }

    /**
     * create ImportsSorter instance with default configuration
     */
    public ImportsSorter() {
        this(new ArrayList<>(1));
    }

    /**
     * set import order configuration
     *
     * @param importOrder List of unduplicated strings of package name prefix, blank "" means
     *                    others, and "#" means all static imports.
     */
    public void setImportOrder(List<String> importOrder) {
        this.importOrder = importOrder;
    }

    /**
     * generate content for a java.importorder file
     *
     * @return content for a java.importorder file
     */
    public String getImportOrderFileContent() {
        if (Objects.isNull(importOrder) || importOrder.isEmpty()) {
            return "0=";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < importOrder.size(); ++i) {
            sb.append(i);
            sb.append('=');
            sb.append(importOrder.get(i));
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * sort import lines of java source code
     *
     * @param formattedCode java source code, as a string
     * @return formatted java source code, with import lines sorted, as a string
     */
    public String sortImportsInFormattedCode(String formattedCode) {
        // 已经经过格式化后的代码, import语句肯定全部集中在一起,中间不会夹杂其他代码或者注释
        // 在import代码块之前,也不会有多行模式的注释的中间行是以import开头的
        int importPartStart;
        if (formattedCode.startsWith(KEYWORD_IMPORT_PREFIX)) {
            importPartStart = 0;
        } else {
            importPartStart = formattedCode
                    .indexOf(LINE_SEP + KEYWORD_IMPORT_PREFIX);
        }
        if (importPartStart == -1) {
            // No imports
            return formattedCode;
        } else if (importPartStart != 0) {
            // 第一个字符是换行，不要
            ++importPartStart;
        }
        int importPartEnd = formattedCode
                .lastIndexOf(LINE_SEP + KEYWORD_IMPORT_PREFIX)
                + LINE_SEP.length()
                + KEYWORD_IMPORT_PREFIX.length();
        while (formattedCode.charAt(importPartEnd) != '\n') {
            ++importPartEnd;
        }
        // 最后一个字符是换行，不要
        --importPartEnd;
        String importPart = formattedCode
                .substring(importPartStart, importPartEnd);
        String codeBeforeImport = importPartStart == 0 ? ""
                : formattedCode.substring(0, importPartStart);
        String codeAfterImport = formattedCode
                .substring(importPartEnd);
        String orderedImportLines = new SortContext(importPart)
                .sort();
        return codeBeforeImport + orderedImportLines
                + codeAfterImport;
    }

    class SortContext {

        private static final String KEY_STATIC = "__static__";

        private static final String KEY_OTHER = "__other__";

        private final String rawImportPart;

        private final Map<String, List<String>> groups;

        private final List<String> selfOrderedImportOrder;

        SortContext(String rawImportPart) {
            this.rawImportPart = rawImportPart;
            this.selfOrderedImportOrder = new ArrayList<>(
                    importOrder.size()
            );
            this.groups = new HashMap<>(importOrder.size());
            for (String key : importOrder) {
                if (key.equals("")) {
                    groups.put(KEY_OTHER, new ArrayList<>());
                } else if (key.equals("#")) {
                    groups.put(KEY_STATIC, new ArrayList<>());
                } else {
                    String keyWithPrefix = KEYWORD_IMPORT_PREFIX
                            + key;
                    groups.put(keyWithPrefix, new ArrayList<>());
                    selfOrderedImportOrder.add(keyWithPrefix);
                }
            }

            // A是B的前缀， import语句的包名C，以A为前缀，那么一定也以B为前缀，A更加具体，把C划到B组，而不是A组
            // 因此，这里要排序备用
            selfOrderedImportOrder.sort(Comparator.reverseOrder());
        }

        private void divideIntoGroups() {
            String[] importLines = rawImportPart.split(LINE_SEP);
            for (String line : importLines) {
                if (!line.startsWith(KEYWORD_IMPORT_PREFIX)) {
                    continue;
                }
                putLineInTheRightGroup(line);
            }
            for (List<String> group : groups.values()) {
                group.sort(ImportsSorter::importLineCompare);
            }
        }

        private void putLineInTheRightGroup(String line) {
            for (String prefix : selfOrderedImportOrder) {
                if (line.startsWith(prefix)) {
                    groups.get(prefix).add(line);
                    return;
                }
            }
            if (line.startsWith(PREFIX_STATIC)) {
                groups.get(KEY_STATIC).add(line);
            } else {
                groups.get(KEY_OTHER).add(line);
            }
        }

        private String sort() {
            divideIntoGroups();
            StringBuilder sb = new StringBuilder();
            for (String key : importOrder) {
                List<String> group;
                if (key.equals("")) {
                    group = groups.get(KEY_OTHER);
                } else if (key.equals("#")) {
                    group = groups.get(KEY_STATIC);
                } else {
                    String keyWithPrefix = KEYWORD_IMPORT_PREFIX
                            + key;
                    group = groups.get(keyWithPrefix);
                }

                for (String line : group) {
                    sb.append(line);
                    sb.append(LINE_SEP);
                }
                if (!group.isEmpty()) {
                    sb.append(LINE_SEP);
                }
            }
            return sb.toString().trim();
        }

    }

    private static int importLineCompare(String line1, String line2) {
        // 去掉末尾的分号再比较
        return line1.substring(0, line1.length() - 1)
                .compareTo(line2.substring(0, line2.length() - 1));
    }

}

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

package zk.gradle.plugin.formatter;

/**
 * file format result type
 */
public enum FormatResult {

    /**
     * file was pretty formatted already, so unchanged while format it.
     */
    UNCHANGED,

    /**
     * file wat not pretty formatted, and it is changed while format it, success.
     */
    SUCCEED,

    /**
     * failed to format, maybe exception happened
     */
    FAILED;

    private String message;

    /**
     * Set message to describe format result
     *
     * @param message to describe format result
     * @return this
     */
    public FormatResult withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Get message describe format result
     *
     * @return String message describe format result
     */
    public String getMessage() {
        return message;
    }

}

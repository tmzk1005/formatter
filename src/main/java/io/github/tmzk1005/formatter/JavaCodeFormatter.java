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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

/**
 * java source code format implementation, based on eclipse formatter
 */
public class JavaCodeFormatter {

    private CodeFormatter codeFormatter;

    private final ImportsSorter importsSorter;

    private DefaultCodeFormatterOptions codeFormatterOptions;

    /**
     * create an Instance with default configuration
     */
    public JavaCodeFormatter() {
        initOptions();
        codeFormatter = ToolFactory.createCodeFormatter(codeFormatterOptions.getMap(), ToolFactory.M_FORMAT_EXISTING);
        importsSorter = new ImportsSorter();
    }

    private void initOptions() {
        codeFormatterOptions = new DefaultCodeFormatterOptions(null);
        codeFormatterOptions.setJavaConventionsSettings();
        codeFormatterOptions.tab_char = DefaultCodeFormatterOptions.SPACE;
        codeFormatterOptions.line_separator = "\n";
        codeFormatterOptions.comment_line_length = 100;
        codeFormatterOptions.page_width = 160;
        codeFormatterOptions.tab_size = 4;
        codeFormatterOptions.continuation_indentation = 2;
        codeFormatterOptions.join_lines_in_comments = false;
        codeFormatterOptions.join_wrapped_lines = false;
        codeFormatterOptions.comment_format_line_comment_starting_on_first_column = true;
        codeFormatterOptions.comment_format_header = true;
        codeFormatterOptions.comment_format_html = false;
        codeFormatterOptions.indent_switchstatements_compare_to_switch = true;
        codeFormatterOptions.parenthesis_positions_in_method_declaration = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_method_invocation = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_enum_constant_declaration = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_record_declaration = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_if_while_statement = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_for_statement = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_switch_statement = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_try_clause = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_catch_clause = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_annotation = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
        codeFormatterOptions.parenthesis_positions_in_lambda_declaration = DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED;
    }

    /**
     * configure format options
     *
     * @param options format options
     */
    public void setOptions(Map<String, String> options) {
        String settingIdPrefix = "org.eclipse.jdt.core.formatter.";
        Map<String, String> finalOptions = new HashMap<>(options.size());
        options.forEach((key, value) -> finalOptions.put(settingIdPrefix + key, value));
        codeFormatterOptions.set(finalOptions);
        codeFormatter = ToolFactory.createCodeFormatter(codeFormatterOptions.getMap(), ToolFactory.M_FORMAT_EXISTING);
    }

    /**
     * configure import lines order
     *
     * @param importOrder import lines order
     */
    public void setImportOrder(List<String> importOrder) {
        this.importsSorter.setImportOrder(importOrder);
    }

    /**
     * format a java source code file, specified by a ${@code Path}
     *
     * @param path   java source code file path
     * @param dryRun boolean, set if just dry run, if true, only try to format and
     *               get format result.
     * @return format result
     */
    public FormatResult formatFile(Path path, boolean dryRun) {
        return formatFile(path.toFile(), dryRun);
    }

    /**
     * format a java source code file, specified by a ${@code File}
     *
     * @param file   java source code file
     * @param dryRun boolean, set if just dry run, if true, only try to format and
     *               get format result.
     * @return format result
     */
    public FormatResult formatFile(File file, boolean dryRun) {
        try {
            return doFormat(file, dryRun);
        } catch (IOException | BadLocationException exception) {
            return FormatResult.FAILED.withMessage(exception.getMessage());
        }
    }

    private FormatResult doFormat(File file, boolean dryRun)
            throws IOException, BadLocationException {
        String originalCode = new String(Util.getFileCharContent(file, "\n"));
        int kind = (file.getName().equals(IModule.MODULE_INFO_JAVA) ? CodeFormatter.K_MODULE_INFO : CodeFormatter.K_COMPILATION_UNIT) | CodeFormatter.F_INCLUDE_COMMENTS;
        TextEdit textEdit = this.codeFormatter.format(kind, originalCode, 0, originalCode.length(), 0, "\n");
        if (Objects.isNull(textEdit)) {
            return FormatResult.FAILED;
        }
        IDocument document = new Document(originalCode);
        textEdit.apply(document);
        String formattedCode = document.get();

        String importSortedCode = importsSorter.sortImportsInFormattedCode(formattedCode);

        if (!dryRun && !originalCode.equals(importSortedCode)) {
            Files.writeString(file.toPath(), importSortedCode);
        }

        return originalCode.equals(importSortedCode) ? FormatResult.UNCHANGED : FormatResult.SUCCEED;
    }

    /**
     * write a format options to a xml file
     *
     * @param filePath xml file path
     * @throws IOException IOException
     */
    public void writeOptionsToXml(Path filePath) throws IOException {
        Map<String, String> options = codeFormatterOptions.getMap();
        org.dom4j.Document document = DocumentHelper.createDocument();
        Element profiles = document.addElement("profiles");
        Element profile = profiles.addElement("profile");
        options.keySet().stream().sorted().forEach(key -> {
            Element setting = profile.addElement("setting");
            setting.addAttribute("id", key);
            setting.addAttribute("value", options.get(key));
        });
        Path parent = filePath.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        XMLWriter xmlWriter = new XMLWriter(OutputFormat.createPrettyPrint());
        xmlWriter.setOutputStream(Files.newOutputStream(filePath));
        xmlWriter.write(document);
        xmlWriter.close();
    }

    /**
     * write a import order configuration to a property file
     *
     * @param filePath property file path
     * @throws IOException IOException
     */
    public void writeImportOrderFile(Path filePath) throws IOException {
        Path parent = filePath.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(filePath, importsSorter.getImportOrderFileContent());
    }

}

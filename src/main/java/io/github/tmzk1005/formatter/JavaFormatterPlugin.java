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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

/**
 * Java code format gradle plugin implementation
 */
public class JavaFormatterPlugin implements Plugin<Project> {

    private static final String JAVA_FILE_SUFFIX = ".java";

    private static final String DEFAULT_QA_DIR = "qa/format";

    private static final String FMT_FILE_NAME = "java-format.xml";

    private static final String IMPORT_ORDER_FILE_NAME = "java.importorder";

    private static final String TASK_FORMAT = "fmtFormat";

    private static final String TASK_CHECK = "fmtCheck";

    private static final String TASK_CREATE_RULES_FILE = "fmtCreateRulesFile";

    private final JavaCodeFormatter javaCodeFormatter = new JavaCodeFormatter();

    private PluginExtension formatterConf;

    private boolean confLoaded;

    /**
     * implement gradle org.gradle.api.Plugin interface, apply this plugin, register tasks
     *
     * @param project the gradle project
     */
    @Override
    public void apply(Project project) {
        formatterConf = project.getExtensions().create("formatter", PluginExtension.class);
        registerTaskCreateRuleFile(project);
        registerTaskFormat(project);
        registerTaskCheck(project);
    }

    private void loadConf() {
        if (confLoaded) {
            return;
        }
        Map<String, String> fmtOptions = formatterConf.getFmtOptions().getOrNull();
        if (Objects.nonNull(fmtOptions) && !fmtOptions.isEmpty()) {
            javaCodeFormatter.setOptions(fmtOptions);
        }
        List<String> importOrder = formatterConf.getImportOrder().getOrNull();
        if (Objects.isNull(importOrder) || importOrder.isEmpty()) {
            importOrder = List.of("java", "javax", "", "#");
        }
        javaCodeFormatter.setImportOrder(importOrder);
        confLoaded = true;
    }

    private void registerTaskFormat(Project project) {
        project.task(TASK_FORMAT)
                .dependsOn(":" + TASK_CREATE_RULES_FILE)
                .doLast(task -> {
                    loadConf();
                    doFormat(project.getProjectDir().toPath(), false, task);
                })
                .setDescription("Check Java source code files format, and auto format not pretty formatted files, also show changed file names.");
    }

    private void registerTaskCheck(Project project) {
        project.task(TASK_CHECK)
                .dependsOn(":" + TASK_CREATE_RULES_FILE)
                .doLast(task -> {
                    loadConf();
                    doFormat(project.getProjectDir().toPath(), true, task);
                }).setDescription("Check Java source code files format, and show not pretty formatted file names.");
    }

    private void registerTaskCreateRuleFile(Project project) {
        Project rootProject = project.getRootProject();
        Set<Task> tasksByName = rootProject.getTasksByName(TASK_CREATE_RULES_FILE, false);
        if (tasksByName.isEmpty()) {
            rootProject.task(TASK_CREATE_RULES_FILE).doLast(task -> {
                loadConf();
                String qaDir = formatterConf.getQaDir().getOrElse(DEFAULT_QA_DIR);
                Path qaPath = rootProject.getProjectDir().toPath().resolve(qaDir);
                createRulesFile(task, qaPath);
            }).setDescription("Generate java-format.xml and java.importorder file.");
        }
    }

    private void doFormat(Path srcDir, boolean dryRun, Task task) {
        Set<Path> allJavaFiles;
        try {
            allJavaFiles = getFiles(srcDir);
        } catch (IOException ioException) {
            return;
        }
        final FormatResults formatResults = new FormatResults();
        for (Path javaFile : allJavaFiles) {
            String filePathName = javaFile.toString();
            FormatResult fr = formatFile(javaFile, dryRun);
            switch (fr) {
                case UNCHANGED -> formatResults.addUnchangedFile(filePathName);
                case SUCCEED -> formatResults.addSucceed(filePathName);
                case FAILED -> formatResults.addFailed(filePathName, fr.getMessage());
            }
        }
        Logger logger = task.getLogger();
        if (dryRun) {
            showCheckResults(formatResults, logger);
        } else {
            showFormatResults(formatResults, logger);
        }
    }

    private Set<Path> getFiles(Path path) throws IOException {
        final Set<Path> javaFiles = new HashSet<>();
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attrs
            ) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);
                if (file.getFileName().toString().endsWith(JAVA_FILE_SUFFIX)) {
                    javaFiles.add(file);
                }
                return super.visitFile(file, attrs);
            }
        });
        return javaFiles;
    }

    private void showCheckResults(FormatResults formatResults, Logger logger) {
        if (formatResults.getFailed().isEmpty() && formatResults.getSucceed().isEmpty()) {
            logger.lifecycle("All files are pretty formatted!");
            return;
        }
        if (!formatResults.getSucceed().isEmpty()) {
            String fileListStr = String.join("\n", formatResults.getSucceed());
            logger.lifecycle(
                    "There is {} files was not pretty formatted: \n{}\n\nRun './gradlew fmtFormat' to format them",
                    formatResults.getSucceed().size(),
                    fileListStr
            );
        }
        if (!formatResults.getFailed().isEmpty()) {
            for (Map.Entry<String, String> entry : formatResults.getFailed().entrySet()) {
                logger.error("Failed to check if file {} is formatted : {}", entry.getKey(), entry.getValue());
            }
        }
    }

    private void showFormatResults(FormatResults formatResults, Logger logger) {
        if (formatResults.getSucceed().isEmpty() && formatResults.getFailed().isEmpty()) {
            logger.lifecycle("No files need to reformat.");
            return;
        }
        if (!formatResults.getSucceed().isEmpty()) {
            String fileListStr = String.join("\n", formatResults.getSucceed());
            logger.lifecycle("{} files formatted: \n{}", formatResults.getSucceed().size(), fileListStr);
        }
        if (!formatResults.getFailed().isEmpty()) {
            for (Map.Entry<String, String> entry : formatResults.getFailed().entrySet()) {
                logger.error("Failed to format file {} : {}", entry.getKey(), entry.getValue());
            }
        }
    }

    private FormatResult formatFile(Path file, boolean dryRun) {
        return javaCodeFormatter.formatFile(file, dryRun);
    }

    private void createRulesFile(Task task, Path qaPath) {
        if (Files.notExists(qaPath)) {
            Logger logger = task.getLogger();
            logger.info("The QA path {} not exists, try to create it.", qaPath);
            try {
                Files.createDirectories(qaPath);
            } catch (IOException ioE) {
                throw new GradleException("Failed to create QA directory " + qaPath);
            }
        }
        Path formatRulesFile = qaPath.resolve(FMT_FILE_NAME);
        Path importOrderFile = qaPath.resolve(IMPORT_ORDER_FILE_NAME);
        try {
            if (Files.notExists(formatRulesFile)) {
                javaCodeFormatter.writeOptionsToXml(formatRulesFile);
            }
            if (Files.notExists(importOrderFile)) {
                javaCodeFormatter.writeImportOrderFile(importOrderFile);
            }
        } catch (IOException ioException) {
            throw new GradleException(ioException.getMessage(), ioException);
        }
    }

}

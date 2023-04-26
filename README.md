# formatter-gradle-plugin

This project provides a mechanism to automatically (re)format your [Gradle](https://gradle.org/)
project during a Gradle build, or to verify its formatting, so that your project
can converge on consistent code style regardless of user preferences, IDE
settings, etc.

This plugin uses the Eclipse code formatter to format java code, format options can be set to overwrite
the default, and also support custom import order.

This software is provided WITHOUT ANY WARRANTY, and is available under the Apache License,
Version 2. Any code loss caused by using this plugin is not the responsibility of the author(s).
Be sure to use some source repository management system such as GIT before using this plugin.

## how to use

View the Gradle plugin documentation for the latest release to learn how to use a gradle plugin.

To use this plugin, include the following in your `build.gradle` :

```groovy
plugins {
    id "zk.gradle.plugin.java-formatter" version "x.y.z"
}
```

This plugin will add 3 tasks to your project, with name prefix 'fmt', as shown below.

### fmtCheck

Check the Java source files format, and show not pretty formatted file names.

### fmtFormat

Check the Java source files format, and auto format not pretty formatted files,
also show changed file names.

### fmtCreateRulesFile

generate two files: `java-format.xml` and `java.import`, under directory `qa/format` within your root project directory (this directory can be changed).
These two file is used to work with [IDEA Eclipse Formatter Plugin](https://github.com/krasa/EclipseCodeFormatter), view
its documentation to learn how to use `java-format.xml` and `java.import`.
Format java sources file by execute `gradle fmtFormat` or by IDEA `Ctl-Atl-L` should have same effect.

## how to configure

This plugin is configurable, here is an example, include the following in your `build.gradle` :

```groovy
formatter {

    qaDir = "qa/format"

    importOrder = ["java", "javax", "org.springframework", "", "#"]

    fmtOptions [
            "lineSplit": "160",
            "tabulation.size": "4",
    ]

}
```

### qaDir

`qaDir` is the directory to put generated `java-format.xml` and `java.import` by
task `fmtCreateRulesFile`, should be an relative path string to your root project.

default values is `qa/format`

### importOrder

`importOrder` is an array of unduplicated strings of package name prefix, 
blank `""` means others, and `"#"` means all static imports.

default values is `["java", "javax", "", "#"]`

### fmtOptions

`fmtOptions` is string to string Map (some values is number type, but still config as string). 
There are hundreds of options, run gradle task `fmtCreateRulesFile` to see
all the options and value in the generated xml file `java-format.xml`, 
option name is the setting id without prefix "org.eclipse.jdt.core.formatter.".

### Build this project locally

You can clone this project, build and use it locally.

#### Build locally

Execute `./gradlew publishToMavenLocal`, this plugin will build and publish 
to your local maven repository for later use.

If you have build problems, a noteworthy point is that this project use itself to format its code,
so if you want to run tasks add by this plugin, a "Pre build" is needed.

#### Use locally

After build locally, include the following in your `build.gradle`:

```groovy
plugins {
    id "zk.gradle.plugin.java-formatter" version "x.y.z"
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}
```

include the following in your `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

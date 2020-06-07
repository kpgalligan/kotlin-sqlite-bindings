/*
 * Copyright 2020 Google, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.birbit.ksqlite.build.AndroidSetup
import com.birbit.ksqlite.build.Dependencies
import com.birbit.ksqlite.build.Publishing
import com.birbit.ksqlite.build.SqliteCompilation
import com.birbit.ksqlite.build.SqliteCompilationConfig
import com.birbit.ksqlite.build.setupCommon
import org.jetbrains.kotlin.konan.target.Family.ANDROID

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
}
AndroidSetup.configure(project)
kotlin {
    setupCommon(
        gradle = gradle,
        includeAndroidNative = true) {
        binaries {
            sharedLib(namePrefix = "sqlite3jni")
        }
        compilations["main"].defaultSourceSet {
            dependencies {
                project(":sqlitebindings-api")
                implementation(kotlin("stdlib-common"))
            }
        }
        // jni already exists on android so we don't need it there
        if (this.konanTarget.family != ANDROID) {
            compilations["main"].cinterops.create("jni") {
                // JDK is required here, JRE is not enough
                val javaHome = File(System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
                var include = File(javaHome, "include")
                if (!include.exists()) {
                    // look upper
                    include = File(javaHome, "../include")
                }
                if (!include.exists()) {
                    throw GradleException("cannot find include")
                }
                // match the name on android to use the same code for native.
                // TODO could be abstract this into another module?
                packageName = "platform.android"
                includeDirs(
                    Callable { include },
                    Callable { File(include, "darwin") },
                    Callable { File(include, "linux") },
                    Callable { File(include, "win32") }
                )
            }
        }
    }

    val combinedSharedLibsFolder = project.buildDir.resolve("combinedSharedLibs")
    val combineSharedLibsTask =
        com.birbit.ksqlite.build.CollectNativeLibrariesTask
            .create(
                project = project,
                namePrefix = "sqlite3jni",
                outFolder = combinedSharedLibsFolder,
                forAndroid = false)

    val combinedAndroidSharedLibsFolder = project.buildDir.resolve("combinedAndroidSharedLibs")
    val combineAndroidSharedLibsTask =
        com.birbit.ksqlite.build.CollectNativeLibrariesTask
            .create(
                project = project,
                namePrefix = "sqlite3jni",
                outFolder = combinedAndroidSharedLibsFolder,
                forAndroid = true)
    project.android.sourceSets {
        val main by getting {
            this.jniLibs.srcDir(combinedAndroidSharedLibsFolder)
        }
    }
    project.android.libraryVariants.all {
        this.javaCompileProvider.dependsOn(combineAndroidSharedLibsTask)
    }
    jvm().compilations["main"].compileKotlinTask.dependsOn(combineSharedLibsTask)
    android {
        publishAllLibraryVariants()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(project(":sqlitebindings-api"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val commonJvmMain = create("commonJvmMain") {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val androidTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("androidx.test.ext:junit:1.1.1")
                implementation("androidx.test:runner:1.2.0")
            }
        }

        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(Dependencies.NATIVE_LIB_LOADER)
            }
            resources.srcDir(combinedSharedLibsFolder)
        }
        // JVM-specific tests and their dependencies:
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
SqliteCompilation.setup(
    project,
    SqliteCompilationConfig(
        version = "3.31.1"
    )
)
Publishing.setup(project)

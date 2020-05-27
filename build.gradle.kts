/*
 * Copyright 2020 Google, Inc.
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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KotlinConstants.LICENSE_HEADER_DELIMITER

plugins {
    id("com.diffplug.gradle.spotless") version "4.0.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "com.diffplug.gradle.spotless")
    this.extensions.getByType(SpotlessExtension::class).apply {
        kotlin {
            target("**/*.kt")
            ktlint().userData(
                mapOf(
                    "max_line_length" to "120"
                )
            )
            licenseHeaderFile(project.rootProject.file("scripts/copyright.txt"))
        }
        kotlinGradle {
            ktlint()
        }
    }
}
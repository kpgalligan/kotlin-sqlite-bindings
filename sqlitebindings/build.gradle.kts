import com.birbit.ksqlite.build.Dependencies
import com.birbit.ksqlite.build.SqliteCompilationConfig
import com.birbit.ksqlite.build.setupCommon
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") //version "1.3.72"
}

group = "com.birbit"
version = "0.1-SNAPSHOT"

repositories {
    maven("https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:1.4.0-dev-1793,branch:(default:any)/artifacts/content/maven")
    mavenCentral()
}

kotlin {
    setupCommon(gradle) {
        binaries {
            sharedLib(namePrefix = "sqlite3jni")
        }

        compilations["main"].cinterops.create("jni") {
            // JDK is required here, JRE is not enough
            val javaHome = File(System.getenv("JAVA_HOME") ?: System.getProperty("java.home"))
            println("java home:$javaHome")
            var include = File(javaHome, "include")
            if (!include.exists()) {
                // look upper
                include = File(javaHome, "../include")
            }
            if (!include.exists()) {
                throw GradleException("cannot find include")
            }
            packageName = "com.birbit.jni"
            includeDirs(
                Callable { include },
                Callable { File(include, "darwin") },
                Callable { File(include, "linux") },
                Callable { File(include, "win32") }
            )
        }
    }

    val combinedSharedLibsFolder = project.buildDir.resolve("combinedSharedLibs")
    val combineSharedLibsTask =
        com.birbit.ksqlite.build.CollectNativeLibrariesTask.Companion.create(project, "sqlite3jni", combinedSharedLibsFolder)
    jvm().compilations["main"].compileKotlinTask.dependsOn(combineSharedLibsTask)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
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
com.birbit.ksqlite.build.SqliteCompilation.setup(
    project,
    SqliteCompilationConfig(
        version = "3.31.1"
    )
)
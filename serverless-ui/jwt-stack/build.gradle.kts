import groovy.lang.Closure
import org.akhikhl.gretty.GrettyExtension
import org.wisepersist.gradle.plugins.gwt.GwtCompileOptions
import org.wisepersist.gradle.plugins.gwt.GwtPluginExtension
import org.wisepersist.gradle.plugins.gwt.GwtSuperDevOptions

plugins {
    kotlin("jvm") version "1.4.30"
    id("application")
    id("java")
    id("idea")
    id("java-library")
    id("war")

    // Creates fat JAR
    id("com.github.johnrengelman.shadow") version "6.1.0"

    id("org.wisepersist.gwt") version "1.1.11"
    id("org.gretty") version "3.0.3"

    id("com.github.ben-manes.versions") version "0.36.0"
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

idea.module.setDownloadSources(true)
idea.module.setDownloadJavadoc(true)

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

// Required if using DominoMVP and annotations that generate code
sourceSets {
    val main by getting
    main.java.srcDirs("build/generated/sources/annotationProcessor/java/main")
}

// Required for shadow JAR but we don't use it. Can not be replaced with application.mainClass.set.
application.mainClassName = "not-necessary"

val gradleDependencyVersion = "6.8.1"

tasks.wrapper {
    gradleVersion = gradleDependencyVersion
    distributionType = Wrapper.DistributionType.ALL
}

repositories {
    mavenCentral()
    // Required for gretty
    jcenter()
    // Required for AWS Lambda Servlet annotation processor
    maven(url = "https://jitpack.io")
    // Required for Domino UI
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    // Required for Domino UI
    maven(url = "https://repo.vertispan.com/gwt-snapshot/")
    // Required for Gradle Tooling API
    maven(url = "https://repo.gradle.org/gradle/libs-releases-local/")
}

tasks.distZip { enabled = false }
tasks.distTar { enabled = false }
tasks.shadowDistZip { enabled = false }
tasks.shadowDistTar { enabled = false }

val awsLambdaJavaCoreVersion = "1.2.1"
val awsLambdaJavaLog4jVersion = "1.0.1"
val jacksonVersion = "2.12.1"
val vavrVersion = "0.10.3"
val awsSdk2Version = "2.16.3"
val gwtServletVersion = "2.8.2"
val junitVersion = "4.13.2"
val jettyVersion = "11.0.0"
val slf4jSimpleVersion = "1.7.30"
val bouncyCastleVersion = "1.68"
val vertxVersion = "4.0.2"
val jjwtVersion = "3.13.0"
val dominoKitVersion = "1.0-alpha-gwt2.8.2-SNAPSHOT"
val awsCdkConstructsForJava = "0.5.6"
val awsLambdaServletVersion = "0.2.4"

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:$awsLambdaJavaCoreVersion")
    implementation("com.amazonaws:aws-lambda-java-log4j:$awsLambdaJavaLog4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("io.vavr:vavr:$vavrVersion")
    implementation("io.vavr:vavr-jackson:$vavrVersion")
    implementation("software.amazon.awssdk:iot:$awsSdk2Version")
    implementation("software.amazon.awssdk:iotdataplane:$awsSdk2Version")
    implementation("software.amazon.awssdk:sts:$awsSdk2Version")
    implementation("com.google.gwt:gwt-servlet:$gwtServletVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jSimpleVersion")
    implementation("com.auth0:java-jwt:$jjwtVersion")
    implementation("io.vertx:vertx-core:$vertxVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    // 1.0-SNAPSHOT
    api("org.dominokit:domino-ui:$dominoKitVersion") { setChanging(true) }

    // 1.0-rc.4-SNAPSHOT
    api("org.dominokit:domino-rest-gwt:$dominoKitVersion") { setChanging(true) }
    annotationProcessor("org.dominokit:domino-rest-apt:$dominoKitVersion") { setChanging(true) }

    // 1.0-rc.5-SNAPSHOT
    api("org.dominokit.domino:domino-gwt-view:$dominoKitVersion") { setChanging(true) }
    annotationProcessor("org.dominokit.domino.apt:apt-client:$dominoKitVersion") { setChanging(true) }

    api("com.github.aws-samples:aws-lambda-servlet:$awsLambdaServletVersion")
    annotationProcessor("com.github.aws-samples:aws-lambda-servlet:$awsLambdaServletVersion")

    implementation("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJava")
    annotationProcessor("com.github.aws-samples:aws-cdk-constructs-for-java:$awsCdkConstructsForJava")

    testImplementation("junit:junit:$junitVersion")
}

configurations.all {
    // Check for updates on changing dependencies at most every 10 minutes
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)

    // Don't allow any configuration to use a broken version of HTTP client
    resolutionStrategy.force("org.apache.httpcomponents:httpclient:4.5.13")
}

configurations.compile {
    // Fixes - Caused by: java.util.ServiceConfigurationError: org.apache.juli.logging.Log: org.eclipse.jetty.apache.jsp.JuliLog not a subtype
    exclude("org.mortbay.jasper")
}

// Extension configuration
configure<GrettyExtension> {
    // Supported values:
    // "jetty7", "jetty8", "jetty9", "tomcat7", "tomcat8"
    servletContainer = "jetty9.4"
}

configure<GwtPluginExtension> {
    gwtVersion = "2.8.2"
    maxHeapSize = "2048M"

    modules("com.awslabs.iatt.spe.serverless.gwt.Jwt")
    devModules("com.awslabs.iatt.spe.serverless.gwt.JwtDev")

    compiler(closureOf<GwtCompileOptions> {
        localWorkers = 8
        optimize = 9
        maxHeapSize = "2048M"
        // Set this value if you need to debug the generated source
        // style = Style.PRETTY
    } as Closure<GwtCompileOptions>)

    superDev(closureOf<GwtSuperDevOptions> {
        noPrecompile = true
        maxHeapSize = "2048M"
    } as Closure<GwtSuperDevOptions>)
}

// Task configuration
val awsIotBrowserBundle = "aws-iot-sdk-browser-bundle-min.js"
val awsIotBrowserBundleRelativeDestination = "./war/" + awsIotBrowserBundle
val awsIotBrowserBundleAbsoluteDestination =
    project.projectDir.absolutePath + "/" + awsIotBrowserBundleRelativeDestination

val copyBrowserBundle by tasks.registering(Exec::class) {
    dependsOn += createDockerContainerForBrowserBundle

    commandLine(
        "docker",
        "cp",
        "aws-iot-device-sdk-js-container:/node_modules/aws-iot-device-sdk/browser/aws-iot-sdk-browser-bundle-min-uglifyjs.js",
        awsIotBrowserBundleRelativeDestination
    )
}

tasks.warTemplate {
    if (!File(awsIotBrowserBundleAbsoluteDestination).exists()) {
        // If we don't have the browser bundle we'll need to build and copy it before the war template task runs
        dependsOn += copyBrowserBundle
    }
}

tasks.war {
    // Make sure GWT code is compiled before the war is generated
    dependsOn += tasks.compileGwt

    // Make sure the fixed keys are generated before the WAR
    dependsOn += tasks.test
}

tasks.shadowJar {
    // Wait until the war is generated so we can get the files from its output directory
    dependsOn += tasks.war
    dependsOn += tasks.warTemplate

    // Make sure the fixed keys are generated before the shadow JAR
    dependsOn += tasks.test

    // Get all of the GWT compiler output
    inputs.files(fileTree("build/gwt/out"))
    from("build/gwt/out")

    // Get all of the static assets
    inputs.files(fileTree("war"))
    from("war")

    // Exclude WEB-INF so we don't include a bunch of classes we don't need
    exclude("WEB-INF")

    dependencies {
        // NOTE: Do not exclude the following dependencies or things will break
        // - javax.servlet:.*
        // - com.google.gwt:gwt-servlet:.*
        exclude(dependency("org.gwtproject.i18n:.*"))
        exclude(dependency("org.gwtproject.core:.*"))
        exclude(dependency("org.gwtproject.editor:.*"))
        exclude(dependency("org.gwtproject.timer:.*"))
        exclude(dependency("org.gwtproject.event:.*"))
        exclude(dependency("org.gwtproject.safehtml:.*"))
        exclude(dependency("org.dominokit:.*"))
        exclude(dependency("io.vertx:.*"))
        exclude(dependency("net.sourceforge.htmlunit:.*"))
        exclude(dependency("colt:.*"))
        exclude(dependency("com.github.tdesjardins:.*"))
        exclude(dependency("tapestry:.*"))
        exclude(dependency("com.google.gwt:gwt-dev:.*"))
        exclude(dependency("com.google.gwt:gwt-user:.*"))
        exclude(dependency("net.sourceforge.cssparser:.*"))
        exclude(dependency("com.ibm.icu:.*"))
        exclude(dependency("javax.websocket:.*"))
        exclude(dependency("org.glassfish:.*"))
    }

    isZip64 = true
}

val validateDockerIsAvailable by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    commandLine("bash", "-c", "docker &> /dev/null")

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("Docker is required for this build")
        }
    }
}

val validateUserHasPermissionsToRunDocker by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    dependsOn += validateDockerIsAvailable
    commandLine("bash", "-c", "docker ps &> /dev/null")

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("This user does not have permission to use Docker or Docker is not running. " +
                    "If you recently added this user to the Docker group try logging out and logging back in again. " +
                    "If you are still unable to run this script but can run 'docker ps' in your shell try killing any existing Gradle daemons that were started before the user was added to the Docker group.")

        }
    }
}

val buildDockerImageForBrowserBundle by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    dependsOn += validateUserHasPermissionsToRunDocker
    commandLine("docker", "build", "--target", "awsIotDeviceSdk", "-t", "aws-iot-device-sdk-js-build", ".")

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("The docker build of the Javascript IoT device SDK failed")
        }
    }
}

val removeBrowserBundleContainerBefore by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    commandLine("docker", "rm", "-f", "aws-iot-device-sdk-js-container")
}

val createDockerContainerForBrowserBundle by tasks.registering(Exec::class) {
    isIgnoreExitValue = true
    dependsOn += buildDockerImageForBrowserBundle
    dependsOn += removeBrowserBundleContainerBefore

    commandLine(
        "docker",
        "create",
        "-ti",
        "--name",
        "aws-iot-device-sdk-js-container",
        "aws-iot-device-sdk-js-build:latest",
        "bash"
    )

    doLast {
        if (execResult!!.exitValue != 0) {
            throw GradleException("Creating the container to extract the Javascript IoT device SDK failed")
        }
    }
}

val serverCode by tasks.registering(Exec::class) {
    inputs.files(fileTree(""))
}

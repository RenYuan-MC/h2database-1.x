import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("java")
    id("org.teavm") version "0.12.1-EAGLER-R3"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

sourceSets {
    main {
        java {
            srcDirs("../h2/src/main")
            // Java 8 sources of H2 1.4 (JSR-310 helpers).
            srcDirs("../h2/src/java8/src")
            // Server / console integrations that need javax.servlet, java.awt or sockets.
            exclude("org/h2/server/web/**")
            exclude("org/h2/tools/Server.java")
            exclude("org/h2/tools/Shell.java")
            exclude("org/h2/tools/Console.java")
            exclude("org/h2/tools/GUIConsole.java")
            exclude("org/h2/tools/ChangeFileEncryption.java")
            // RecoverTester is only reached through reflection and needs FilePathRec.
            exclude("org/h2/store/RecoverTester.java")
            // Optional integrations whose third-party dependencies are not available.
            exclude("org/h2/util/OsgiDataSourceFactory.java")
            exclude("org/h2/util/DbDriverActivator.java")
            exclude("org/h2/util/geometry/JTSUtils.java")
            exclude("org/h2/fulltext/FullTextLucene.java")
            exclude("org/h2/message/TraceWriterAdapter.java")
            // JMX / JNDI / XA APIs are not part of the browser runtime.
            exclude("org/h2/jmx/**")
            exclude("org/h2/jdbcx/**")
            // Authentication back ends that need javax.xml / javax.security.auth /
            // javax.naming.
            exclude("org/h2/security/auth/DefaultAuthenticator.java")
            exclude("org/h2/security/auth/H2AuthConfigXml.java")
            exclude("org/h2/security/auth/impl/JaasCredentialsValidator.java")
            exclude("org/h2/security/auth/impl/LdapCredentialsValidator.java")
            // File providers that need java.io.File, map() or NIO paths.
            exclude("org/h2/store/fs/FilePathDisk.java")
            exclude("org/h2/store/fs/FilePathNio.java")
            exclude("org/h2/store/fs/FilePathNioMapped.java")
            exclude("org/h2/store/fs/FilePathNioMem.java")
            exclude("org/h2/store/fs/FilePathAsync.java")
            exclude("org/h2/store/fs/FilePathSplit.java")
            exclude("org/h2/store/fs/FilePathEncrypt.java")
            exclude("org/h2/store/fs/FilePathRec.java")
            exclude("org/h2/store/fs/FilePathRetryOnInterrupt.java")
            exclude("org/h2/store/fs/FilePathZip.java")
        }
        resources {
            srcDirs("src/main/resources", "../h2/src/main")
            include("org/h2/res/**")
            include("META-INF/services/**")
        }
    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://eaglercraft-teavm-fork.github.io/maven/")
    }
    mavenCentral()
}

dependencies {
    implementation("org.teavm:teavm-jso-apis:0.12.1-EAGLER-R3")
    // Needed for org.teavm.classlib.ResourceSupplier (resource embedding entry point).
    compileOnly("org.teavm:teavm-core:0.12.1-EAGLER-R3")
}

// The Eaglercraft classlib is Java 11 bytecode and uses StringBuilder for string
// concatenation, so invokedynamic (StringConcatFactory) is disabled here.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-XDstringConcat=inline")
}

tasks.withType<Copy>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// H2 1.4 keeps help.csv outside the Java source tree; the release jar places it in
// org/h2/res, so it has to be mapped here before it can be embedded in the module.
tasks.named<ProcessResources>("processResources") {
    from("../h2/src/docsrc/help") {
        include("help.csv")
        into("org/h2/res")
    }
}

teavm.wasmGC {
    targetFileName = "h2wasm.wasm"
    mainClass = "org.h2.wasm.H2Wasm"
    optimization = OptimizationLevel.AGGRESSIVE
    outOfProcess = false
    processMemory = 1024
    debugInformation = true
}

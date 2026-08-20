import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
    alias(libs.plugins.intellijPlatform)
}

group = "com.qualitest"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Plugin.Java)

        // Bundled plugins
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("org.intellij.plugins.markdown")
    }

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Lombok for reducing boilerplate (getters/setters/builders)
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // PSI libraries are provided by IntelliJ Platform
    compileOnly("com.google.guava:guava:32.1.3-jre")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // 不限制 until-build，由 Marketplace 按实际兼容性推进
            untilBuild = provider { null }
        }

        changeNotes = """
            1.0.0 - Initial release
            - Java API scanning support
            - Extract API info from Controller classes
            - Upload to QualiTest platform
            - Support Spring MVC, Swagger and Validation annotations
        """.trimIndent()
    }

    // 跳过启动沙箱 IDE 建索引，显著缩短 buildPlugin / verifyPlugin
    buildSearchableOptions = false

    pluginVerification {
        ides {
            // 日常：只验 platformVersion 对应的一套 IDE（约数分钟）
            // 发版全量矩阵：.\gradlew verifyPlugin -PverifyRecommended=true（可能 20～30+ 分钟）
            // 注：ides.current() 需更高版本 Gradle Plugin；2.10.5 用 create 等价实现
            val useRecommended = providers.gradleProperty("verifyRecommended")
                .map(String::toBoolean)
                .orElse(false)
                .get()
            if (useRecommended) {
                recommended()
            } else {
                create(
                    org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdea,
                    providers.gradleProperty("platformVersion"),
                )
            }
        }
    }

    // 阶段 B：Marketplace 上架后在 release.yml marketplace job 中启用 PUBLISH_TOKEN
    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        // channels.set(listOf("stable"))
    }

    // 阶段 B：Marketplace 要求签名时再取消注释并配置 Org Secrets
    // signing {
    //     certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    //     privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    //     password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    // }
}

tasks.register("printVersion") {
    group = "help"
    description = "Print project version for CI tag validation"
    val projectVersion = version.toString()
    doLast {
        println(projectVersion)
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    patchPluginXml {
        sinceBuild = providers.gradleProperty("pluginSinceBuild")
        untilBuild = provider { null }
    }
}

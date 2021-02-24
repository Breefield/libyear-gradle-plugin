import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import pl.allegro.tech.build.axion.release.domain.RepositoryConfig
import pl.allegro.tech.build.axion.release.domain.TagNameSerializationConfig

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  id("com.gradle.plugin-publish").version("0.12.0")
  id("org.jlleitschuh.gradle.ktlint").version("9.4.1")
  id("pl.allegro.tech.build.axion-release").version("1.12.1")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

scmVersion {
  repository(
    closureOf<RepositoryConfig> {
      directory = project.rootProject.file("..")
    }
  )

  tag(
    closureOf<TagNameSerializationConfig> {
      prefix = "v"
      versionSeparator = ""
    }
  )
}

group = "com.libyear"
version = scmVersion.version

val functionalTest by sourceSets.creating

tasks.withType(JavaCompile::class) {
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

tasks.withType(KotlinCompile::class) {
  kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
  plugins {
    create("libyearPlugin") {
      id = "com.libyear.libyear-gradle-plugin"
      implementationClass = "com.libyear.LibYearPlugin"
    }
  }

  testSourceSets(functionalTest)
}

pluginBundle {
  website = "https://libyear.com/"
  vcsUrl = "https://github.com/f4lco/libyear-gradle-plugin"
  description = "Measure libyears of your application"
  tags = listOf(
    "dependency",
    "dependencies",
    "dependency-insight",
    "dependency-analysis",
    "libyear",
    "age"
  )

  (plugins) {
    "libyearPlugin" {
      displayName = "Libyear Plugin"
    }
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.squareup.okhttp3:okhttp:4.8.1")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
  testImplementation("org.assertj:assertj-core:3.18.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

  "functionalTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.0")
  "functionalTestImplementation"("org.assertj:assertj-core:3.18.1")
  "functionalTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.withType(Test::class) {
  useJUnitPlatform()
  testLogging {
    events("started")
    showExceptions = true
  }
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
  description = "Runs functional tests."
  group = "verification"
  testClassesDirs = functionalTest.output.classesDirs
  classpath = functionalTest.runtimeClasspath
  mustRunAfter(tasks.test)
}

tasks.check {
  dependsOn(functionalTestTask)
}

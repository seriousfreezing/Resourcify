/*
 * This file is part of Resourcify
 * Copyright (C) 2023 DeDiamondPro
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import com.matthewprenger.cursegradle.Options
import com.replaymod.gradle.preprocess.PreprocessTask
import gg.essential.gradle.util.noServerRunConfigs
import gg.essential.gradle.util.setJvmDefault
import org.jetbrains.kotlin.com.google.gson.Gson
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin)
    id(egt.plugins.multiversion.get().pluginId)
    id(egt.plugins.defaults.get().pluginId)
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.cursegradle)
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

preprocess {
    vars.put("MODERN", if (project.platform.mcMinor >= 16) 1 else 0)
    keywords.set(
        mutableMapOf(
            ".java" to PreprocessTask.DEFAULT_KEYWORDS,
            ".kt" to PreprocessTask.DEFAULT_KEYWORDS,
            ".gradle" to PreprocessTask.DEFAULT_KEYWORDS,
            ".json" to PreprocessTask.DEFAULT_KEYWORDS,
            ".mcmeta" to PreprocessTask.DEFAULT_KEYWORDS,
            ".cfg" to PreprocessTask.CFG_KEYWORDS,
            ".accesswidener" to PreprocessTask.CFG_KEYWORDS
        )
    )
}

blossom {
    replaceToken("@NAME@", mod_name)
    replaceToken("@ID@", mod_id)
    replaceToken("@VER@", mod_version)
}

version = mod_version
group = "dev.dediamondpro"
base {
    archivesName.set(mod_id)
}

tasks.compileKotlin.setJvmDefault(if (platform.mcVersion >= 11400) "all" else "all-compatibility")
loom.noServerRunConfigs()
loom {
    if (project.platform.isLegacyForge) runConfigs {
        "client" { programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker") }
    }

    // we only use access wideners in 1.20.4 or above
    if (project.platform.mcVersion >= 12004) {
        accessWidenerPath = project.file("src/main/resources/resourcify.accesswidener")
    }
    if (project.platform.isForge) forge {
        mixinConfig("${project.platform.loaderStr}.mixins.${mod_id}.json")
        mixin.defaultRefmapName.set("forge.mixins.${mod_id}.refmap.json")
    }
    if (project.platform.isNeoForge) mixin {
        useLegacyMixinAp = true
        defaultRefmapName.set("forge.mixins.${mod_id}.refmap.json")
    }
}

if (project.platform.mcVersion != 10809) {
    tasks.getByName("validateAccessWidener").dependsOn("preprocessResources")
}

repositories {
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://repo.essential.gg/repository/maven-public/")
    maven("https://maven.dediamondpro.dev/releases")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.neoforged.net/releases/")
    mavenCentral()
    mavenLocal()
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadeRuntime: Configuration by configurations.creating {
    configurations.runtimeOnly.get().extendsFrom(this)
}

dependencies {
    val elementaPlatform: String? by project
    val universalPlatform: String? by project
    val universalVersion = libs.versions.universal.get() + when {
        project.platform.mcVersion == 12005 && platform.isFabric -> "+diamond.1.20.5"
        project.platform.isNeoForge -> "+diamond.neoforge"
        else -> ""
    }
    if (platform.isFabric) {
        val fabricApiVersion: String by project
        // Our loom version doesn't support mixin remap thingy, so we can't load it in dev env on newer versions
        if (platform.mcVersion <= 12001) {
            modImplementation(fabricApi.module("fabric-resource-loader-v0", fabricApiVersion))
        }
        modImplementation("net.fabricmc:fabric-language-kotlin:${libs.versions.fabric.language.kotlin.get()}")
        modCompileOnly("gg.essential:elementa-${elementaPlatform ?: platform}:${libs.versions.elementa.get()}")
        modImplementation("include"("gg.essential:universalcraft-${universalPlatform ?: platform}:${universalVersion}")!!)
    } else if (platform.isForgeLike) {
        if (platform.isLegacyForge) {
            shade(libs.bundles.kotlin) { isTransitive = false }
            shade(libs.mixin) { isTransitive = false }
            annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
        } else {
            val kotlinForForgeVersion: String by project
            if (platform.isNeoForge) {
                implementation("thedarkcolour:kotlinforforge-neoforge:$kotlinForForgeVersion")
            } else {
                implementation("thedarkcolour:kotlinforforge:$kotlinForForgeVersion")
            }
        }
        shade("gg.essential:universalcraft-${universalPlatform ?: platform}:$universalVersion") {
            isTransitive = false
        }
    }
    // Always shade elementa since we use a custom version, relocate to avoid conflicts
    shade("gg.essential:elementa-${elementaPlatform ?: platform}:${libs.versions.elementa.get()}") {
        isTransitive = false
    }
    // Since elementa is relocated, and MineMark doesn't guarantee backwards compatibility, we need to shade this
    shade(libs.bundles.markdown) {
        isTransitive = false
    }

    val irisVersion: String by project
    // if (!platform.isLegacyForge) modCompileOnly(
    //     if (platform.isFabric) "maven.modrinth:iris:$irisVersion"
    //     else "maven.modrinth:oculus:$irisVersion"
    // )
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_6
            apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_6
        }
    }
    processResources {
        inputs.property("id", mod_id)
        inputs.property("name", mod_name)
        inputs.property("version", mod_version)
        inputs.property("fabricMcVersion", getFabricMcVersionRange())
        inputs.property("forgeMcVersion", getForgeMcVersionRange())
        filesMatching(listOf("mcmod.info", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "fabric.mod.json")) {
            expand(
                mapOf(
                    "id" to mod_id,
                    "name" to mod_name,
                    "version" to mod_version,
                    "fabricMcVersion" to getFabricMcVersionRange(),
                    "forgeMcVersion" to getForgeMcVersionRange(),
                )
            )
        }

        if (project.platform.mcMinor <= 12) {
            dependsOn("generateLangFiles")
            from("${project.buildDir}/generated/lang") {
                into("assets/$mod_id/lang")
            }
            exclude("**/assets/$mod_id/lang/*.json")
        }
        if (project.platform.mcMinor > 16) {
            exclude("ssl/*")
        }

        if (!project.platform.isFabric) exclude("fabric.mod.json", "fabric.mixins.${mod_id}.json")
        if (!project.platform.isLegacyForge) exclude("mcmod.info")
        if (project.platform.isLegacyForge) exclude("resourcify.accesswidener")
        if (!platform.isModLauncher) exclude("pack.mcmeta")
        if (!platform.isForgeLike) exclude("forge.mixins.${mod_id}.json")
        if (!platform.isForge && (!platform.isNeoForge || platform.mcVersion >= 12005)) exclude("META-INF/mods.toml")
        if (!platform.isNeoForge || platform.mcVersion < 12005) exclude("META-INF/neoforge.mods.toml")
    }
    register("generateLangFiles") {
        val gson = Gson()
        val generatedDir = File(project.buildDir, "generated/lang")
        generatedDir.mkdirs()
        rootProject.file("src/main/resources/assets/$mod_id/lang").listFiles()?.filter {
            it.extension == "json"
        }?.forEach { jsonFile ->
            val map: Map<String, String> =
                gson.fromJson(jsonFile.reader(), Map::class.java) as Map<String, String>
            val fileName = jsonFile.nameWithoutExtension.split("_").let {
                "${it[0]}_${it[1].uppercase()}.lang"
            }
            val langFile = File(generatedDir, fileName)
            langFile.printWriter().use { out ->
                map.forEach { (key, value) ->
                    out.println("$key=$value")
                }
            }
        }
    }
    withType<Jar> {
        from(rootProject.file("LICENSE"))
        from(rootProject.file("LICENSE.LESSER"))
    }
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("dev")
        configurations = listOf(shade, shadeRuntime)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        exclude("META-INF/versions/9/**")

        mergeServiceFiles()
        relocate("gg.essential.elementa", "dev.dediamondpro.resourcify.libs.elementa")
        relocate("dev.dediamondpro.minemark", "dev.dediamondpro.resourcify.libs.minemark")
        relocate("org.commonmark", "dev.dediamondpro.resourcify.libs.commonmark")
        relocate("org.ccil.cowan.tagsoup", "dev.dediamondpro.resourcify.libs.tagsoup")
        if (platform.isForgeLike) {
            relocate("gg.essential.universal", "dev.dediamondpro.resourcify.libs.universal")
        }
    }
    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("")
        finalizedBy("copyJar")
        archiveFileName.set("$mod_name (${getPrettyVersionRange()}-${platform.loaderStr})-${mod_version}.jar")
        if (platform.isForgeLike && platform.mcVersion >= 12004) {
            atAccessWideners.add("resourcify.accesswidener")
        }
    }
    jar {
        if (project.platform.isLegacyForge) {
            manifest {
                attributes(
                    mapOf(
                        "ModSide" to "CLIENT",
                        "TweakOrder" to "0",
                        "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                        "ForceLoadAsMod" to true
                    )
                )
            }
        }
        dependsOn(shadowJar)
        archiveClassifier.set("")
        enabled = false
    }
    register<Copy>("copyJar") {
        File("${project.rootDir}/jars").mkdir()
        from(remapJar.get().archiveFile)
        into("${project.rootDir}/jars")
    }
    clean { delete("${project.rootDir}/jars") }
    project.modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("resourcify")
        versionNumber.set(mod_version)
        versionName.set("[${getPrettyVersionRange()}-${platform.loaderStr}] Resourcify $mod_version")
        uploadFile.set(remapJar.get().archiveFile as Any)
        gameVersions.addAll(getSupportedVersionList())
        if (platform.isFabric) {
            loaders.add("fabric")
            loaders.add("quilt")
        } else if (platform.isForge) {
            loaders.add("forge")
            if (platform.mcVersion == 12001) loaders.add("neoforge")
        } else if (platform.isNeoForge) {
            loaders.add("neoforge")
        }
        changelog.set(file("../../changelog.md").readText())
        dependencies {
            if (platform.isForgeLike && !platform.isLegacyForge) {
                required.project("kotlin-for-forge")
            } else if (platform.isFabric) {
                required.project("fabric-api")
                required.project("fabric-language-kotlin")
            }
        }
    }
    project.curseforge {
        project(closureOf<CurseProject> {
            apiKey = System.getenv("CURSEFORGE_TOKEN")
            id = "870076"
            changelog = file("../../changelog.md")
            changelogType = "markdown"
            if (!platform.isLegacyForge) relations(closureOf<CurseRelation> {
                if (platform.isForgeLike && !platform.isLegacyForge) {
                    requiredDependency("kotlin-for-forge")
                } else if (platform.isFabric) {
                    requiredDependency("fabric-api")
                    requiredDependency("fabric-language-kotlin")
                }
            })
            gameVersionStrings.addAll(getSupportedVersionList())
            if (platform.isFabric) {
                addGameVersion("Fabric")
                addGameVersion("Quilt")
            } else if (platform.isForge) {
                addGameVersion("Forge")
                if (platform.mcVersion == 12001) addGameVersion("NeoForge")
            } else if (platform.isNeoForge) {
                addGameVersion("NeoForge")
            }
            releaseType = "release"
            mainArtifact(remapJar.get().archiveFile, closureOf<CurseArtifact> {
                displayName = "[${getPrettyVersionRange()}-${platform.loaderStr}] Resourcify $mod_version"
            })
        })
        options(closureOf<Options> {
            javaVersionAutoDetect = false
            javaIntegration = false
            forgeGradleIntegration = false
        })
    }
    register("publish") {
        dependsOn(modrinth)
        dependsOn(curseforge)
    }
}

// Function to get the range of mc versions supported by a version we are building for.
// First value is start of range, second value is end of range or null to leave the range open
fun getSupportedVersionRange(): Pair<String, String?> = when (platform.mcVersion) {
    12005 -> "1.20.5" to null
    12004 -> "1.20.2" to "1.20.4"
    12001 -> "1.20" to "1.20.1"
    11904 -> "1.19.4" to "1.19.4"
    11902 -> "1.19" to "1.19.2"
    11802 -> (if (platform.isForge) "1.18.2" else "1.18") to "1.18.2"
    11602 -> "1.16" to "1.16.5"
    11202 -> "1.12.2" to "1.12.2"
    10809 -> "1.8.9" to "1.8.9"
    else -> error("Undefined version range for ${platform.mcVersion}")
}

fun getPrettyVersionRange(): String {
    val supportedVersionRange = getSupportedVersionRange()
    return when {
        supportedVersionRange.first == supportedVersionRange.second -> supportedVersionRange.first
        listOf("1.16", "1.18").contains(supportedVersionRange.first) -> "${supportedVersionRange.first}.x"
        else -> "${supportedVersionRange.first}${supportedVersionRange.second?.let { "-$it" } ?: "+"}"
    }
}

fun getFabricMcVersionRange(): String {
    val supportedVersionRange = getSupportedVersionRange()
    if (supportedVersionRange.first == supportedVersionRange.second) return supportedVersionRange.first
    return ">=${supportedVersionRange.first}${supportedVersionRange.second?.let { " <=$it" } ?: ""}"
}

fun getForgeMcVersionRange(): String {
    val supportedVersionRange = getSupportedVersionRange()
    if (supportedVersionRange.first == supportedVersionRange.second) return "[${supportedVersionRange.first}]"
    return "[${supportedVersionRange.first},${supportedVersionRange.second?.let { "$it]" } ?: ")"}"
}

fun getSupportedVersionList(): List<String> {
    val supportedVersionRange = getSupportedVersionRange()
    return when (supportedVersionRange.first) {
        "1.20.5" -> listOf("1.20.5", "1.20.6")
        else -> {
            val minorVersion = supportedVersionRange.first.let {
                if (it.count { c -> c == '.' } == 1) it else it.substringBeforeLast(".")
            }
            val start = supportedVersionRange.first.let {
                if (it.count { c -> c == '.' } == 1) 0 else it.substringAfterLast(".").toInt()
            }
            val end = supportedVersionRange.second!!.let {
                if (it.count { c -> c == '.' } == 1) 0 else it.substringAfterLast(".").toInt()
            }
            val versions = mutableListOf<String>()
            for (i in start..end) {
                versions.add("$minorVersion${if (i == 0) "" else ".$i"}")
            }
            versions
        }
    }
}
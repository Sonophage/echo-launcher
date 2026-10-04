// Top-level build file
plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.android.library)         apply false
    alias(libs.plugins.kotlin.jvm)              apply false
    alias(libs.plugins.kotlin.compose)          apply false
    alias(libs.plugins.kotlin.serialization)    apply false
    alias(libs.plugins.ksp)                     apply false
    alias(libs.plugins.hilt)                    apply false
    alias(libs.plugins.jetbrains.compose)       apply false
    alias(libs.plugins.android.test)            apply false
    alias(libs.plugins.baselineprofile)         apply false
}

// One command to build every shippable release artifact into <root>/dist (gitignored):
// the launcher APK and the Theme Studio installer for the current OS. The per-module copy
// tasks (finalizing each release build) do the actual placing.
tasks.register("dist") {
    group = "distribution"
    description = "Builds the release APK and the Theme Studio installer into <root>/dist."
    dependsOn(
        ":app:assembleRelease",
        ":studio:packageReleaseDistributionForCurrentOS",
    )
}


// The repo's shape, checked (owner, 2026-10-04). The gate and CI run it; each problem names its fix.
tasks.register("checkStructure") {
    group = "verification"
    description = "Fails when the module list, the package layout, feature dependencies or CLAUDE.md's module map drift."
    val root = rootDir
    doLast {
        val problems = mutableListOf<String>()
        val modules = Regex("""include\("(:[^"]+)"\)""")
            .findAll(root.resolve("settings.gradle.kts").readText()).map { it.groupValues[1] }.toSet()
        fun dirOf(module: String) = root.resolve(module.removePrefix(":").replace(':', '/'))

        // 1. every included module exists, and every module folder is included
        modules.filterNot { dirOf(it).resolve("build.gradle.kts").isFile }
            .forEach { problems += "$it is included in settings.gradle.kts but has no build.gradle.kts" }
        val groups = listOf("core", "feature", "discord").map(root::resolve)
        (root.listFiles().orEmpty().toList() + groups.flatMap { it.listFiles().orEmpty().toList() })
            .filter { it.isDirectory && it.resolve("build.gradle.kts").isFile && it != root }
            .map { ":" + it.relativeTo(root).path.replace('/', ':') }
            .filterNot { it in modules }
            .forEach { problems += "$it has a build.gradle.kts but settings.gradle.kts does not include it" }

        // 2. a file's package is its folder
        val packageLine = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
        modules.flatMap { dirOf(it).resolve("src").listFiles().orEmpty().map { set -> set.resolve("kotlin") } }
            .filter { it.isDirectory }
            .forEach { kotlinRoot ->
                kotlinRoot.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                    val declared = packageLine.find(file.readText())?.groupValues?.get(1)
                    val folder = file.parentFile.relativeTo(kotlinRoot).path.replace('/', '.')
                    if (declared != folder) problems += "${file.relativeTo(root)}: package $declared, but its folder is $folder"
                }
            }

        // 3. a feature depends only on core; the two hosts embed other features' screens
        val hosts = setOf(":feature:feature-crossbar", ":feature:feature-settings")
        val allowed = mapOf<String, String>()
        val featureDep = Regex("""project\("(:feature:[^"]+)"\)""")
        val edges = modules.filter { it.startsWith(":feature:") }.flatMap { m ->
            featureDep.findAll(dirOf(m).resolve("build.gradle.kts").readText()).map { "$m -> ${it.groupValues[1]}" }.toList()
        }.toSet()
        edges.filter { it.substringBefore(" ->") !in hosts && it !in allowed }
            .forEach { problems += "$it: a feature depends only on core modules; move what the two share into core" }
        allowed.keys.filterNot { it in edges }
            .forEach { problems += "$it is an allowed exception in checkStructure but no longer exists; remove the exception" }

        // 4. CLAUDE.md's module map names every module, and only those
        val doc = root.resolve("CLAUDE.md")
        if (!doc.isFile) {
            problems += "CLAUDE.md is missing"
        } else {
            val mapped = Regex("""^\|\s*`(:[\w:-]+)`""", RegexOption.MULTILINE).findAll(doc.readText()).map { it.groupValues[1] }.toSet()
            (modules - mapped).forEach { problems += "$it is not in CLAUDE.md's module map; add a row saying what it owns" }
            (mapped - modules).forEach { problems += "CLAUDE.md's module map lists $it, which settings.gradle.kts does not include" }
        }

        if (problems.isNotEmpty()) throw GradleException("checkStructure found ${problems.size} problem(s):\n" + problems.joinToString("\n") { "  - $it" })
        println("checkStructure: ${modules.size} modules; layout, feature dependencies and CLAUDE.md agree")
    }
}

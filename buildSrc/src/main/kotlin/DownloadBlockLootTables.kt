package org.everbuild.twaddle.build

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

abstract class DownloadBlockLootTables : DefaultTask() {
    @get:Input
    abstract val minecraftDataVersion: Property<String>

    @get:OutputFile
    abstract val aggregateFile: RegularFileProperty

    @get:OutputDirectory
    abstract val generatedResourcesDirectory: DirectoryProperty

    @TaskAction
    fun downloadAndExpand() {
        val version = minecraftDataVersion.get()
        val aggregate = aggregateFile.get().asFile
        aggregate.parentFile.mkdirs()

        val partialDownload = aggregate.resolveSibling("${aggregate.name}.part")
        URI("https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/$version/data/minecraft/loot_table/blocks/_all.json")
            .toURL()
            .openConnection()
            .apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", "tools-and-twaddle-testserver")
            }
            .getInputStream()
            .use { input -> partialDownload.outputStream().use { output -> input.copyTo(output) } }
        check(partialDownload.length() > 0L) { "Downloaded Minecraft $version block loot-table index is empty" }
        Files.move(partialDownload.toPath(), aggregate.toPath(), REPLACE_EXISTING)

        val tables = JsonSlurper().parse(aggregate) as? Map<*, *>
            ?: error("Minecraft $version block loot-table index must be a JSON object")
        check(tables.isNotEmpty()) { "Minecraft $version block loot-table index contains no tables" }

        val outputDirectory = generatedResourcesDirectory.get().asFile
        outputDirectory.deleteRecursively()
        for ((name, table) in tables) {
            require(name is String && name.split('/').all { segment ->
                segment.matches(Regex("[a-z0-9._-]+")) && segment != "." && segment != ".."
            }) {
                "Invalid block loot-table name in downloaded data: $name"
            }
            require(table is Map<*, *>) { "Block loot table '$name' is not a JSON object" }

            val output = outputDirectory.resolve("data/minecraft/loot_table/blocks/$name.json")
            output.parentFile.mkdirs()
            output.writeText(JsonOutput.toJson(table))
        }
        logger.lifecycle("Downloaded ${tables.size} Minecraft $version block loot tables to $outputDirectory")
    }
}

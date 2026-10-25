package org.everbuild.twaddle.testserver

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key

val BlockLootTablesTest by testSuite {
    test("block loot table paths retain their blocks namespace path") {
        val pattern = blockLootTablePattern()

        pattern.key("data/minecraft/loot_table/blocks/redstone_ore.json") shouldBe Key.key("minecraft:blocks/redstone_ore")
        pattern.key("data/minecraft/loot_table/blocks/target.json") shouldBe Key.key("minecraft:blocks/target")
    }

    test("block loot table pattern ignores non-block loot tables") {
        val pattern = blockLootTablePattern()

        pattern.key("data/loot_test/loot_table/test/example.json") shouldBe null
        pattern.key("data/minecraft/loot_table/entities/creeper.json") shouldBe null
        pattern.key("data/minecraft/loot_table/blocks.json") shouldBe null
    }
}

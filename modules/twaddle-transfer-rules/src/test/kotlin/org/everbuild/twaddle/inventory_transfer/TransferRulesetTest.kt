package org.everbuild.twaddle.inventory_transfer

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val transferRulesetTests by testSuite("Transfer ruleset") {
    test("Rules are evaluated from top to bottom") {
        val ruleset = transferRuleset {
            routeTo(listOf(3)) { true }
            blockAt(listOf(3)) { true }
            routeTo(listOf(3, 4)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(3),
            listOf(4),
        )
    }

    test("The first matching rule that mentions a slot claims it") {
        val ruleset = transferRuleset {
            routeTo(listOf(2, 1)) { true }
            routeTo(listOf(1, 3)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(2, 1),
            listOf(3),
        )
    }

    test("Each routing rule creates an ordered routing tier") {
        val ruleset = transferRuleset {
            routeTo(listOf(8, 6)) { true }
            routeTo(listOf(4, 2)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(8, 6),
            listOf(4, 2),
        )
    }

    test("A blocking rule keeps its slots out of every later tier") {
        val ruleset = transferRuleset {
            blockAt(listOf(2)) { true }
            routeTo(listOf(1, 2)) { true }
            routeTo(listOf(2, 3)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(1),
            listOf(3),
        )
    }

    test("A matching conditional route overrides a later unconditional block") {
        val ruleset = transferRuleset {
            routeTo(listOf(4)) { sourceSlot == 9 }
            blockAt(listOf(4)) { true }
            routeTo(listOf(4, 5)) { true }
        }

        ruleset.resolve(transferContext(sourceSlot = 9)) shouldBe listOf(
            listOf(4),
            listOf(5),
        )
    }

    test("A non-matching predicate does not claim its slots") {
        val ruleset = transferRuleset {
            routeTo(listOf(4)) { sourceSlot == 9 }
            routeTo(listOf(4)) { true }
        }

        ruleset.resolve(transferContext(sourceSlot = 1)) shouldBe listOf(listOf(4))
    }

    test("Duplicate slots are removed from later rules without reordering selectors") {
        val ruleset = transferRuleset {
            routeTo(listOf(5, 2, 5)) { true }
            routeTo(listOf(4, 2, 3, 5, 1)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(5, 2),
            listOf(4, 3, 1),
        )
    }

    test("SingleSlot resolves its one slot") {
        val ruleset = rulesetRoutingTo(SlotSelector.SingleSlot(7))

        ruleset.resolve(transferContext()) shouldBe listOf(listOf(7))
    }

    test("SlotRange resolves in its configured order") {
        val ascending = rulesetRoutingTo(
            SlotSelector.SlotRange(2, 5, ChooseSlotOrder.FIRST_TO_LAST)
        )
        val descending = rulesetRoutingTo(
            SlotSelector.SlotRange(2, 5, ChooseSlotOrder.LAST_TO_FIRST)
        )

        ascending.resolve(transferContext()) shouldBe listOf(listOf(2, 3, 4, 5))
        descending.resolve(transferContext()) shouldBe listOf(listOf(5, 4, 3, 2))
    }

    test("SlotProgression preserves direction and step") {
        val ruleset = rulesetRoutingTo(SlotSelector.SlotProgression(8 downTo 2 step 3))

        ruleset.resolve(transferContext()) shouldBe listOf(listOf(8, 5, 2))
    }

    test("SlotList preserves its declared order") {
        val ruleset = rulesetRoutingTo(SlotSelector.SlotList(intArrayOf(4, 1, 7)))

        ruleset.resolve(transferContext()) shouldBe listOf(listOf(4, 1, 7))
    }

    test("A built ruleset is unchanged when its builder is modified") {
        val builder = TransferRuleset.builder()
        builder.routeTo(1)
        val built = builder.build()

        builder.routeTo(2)

        built.resolve(transferContext()) shouldBe listOf(listOf(1))
        builder.build().resolve(transferContext()) shouldBe listOf(
            listOf(1),
            listOf(2),
        )
    }

    test("An empty ruleset produces no destinations") {
        val ruleset = TransferRuleset.builder().build()

        ruleset.resolve(transferContext()) shouldBe emptyList()
    }

    test("A fully blocked ruleset produces no destinations") {
        val ruleset = transferRuleset {
            blockAt(listOf(1, 2, 3)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe emptyList()
    }

    test("Shift click moves a remainder through routing tiers in order") {
        val fixture = transferFixture()
        val cursor = ItemStack.of(Material.DIAMOND)
        fixture.playerInventory.cursorItem = cursor
        fixture.playerInventory.setItemStack(9, ItemStack.of(Material.STONE, 10))
        fixture.inventory.setItemStack(0, ItemStack.of(Material.STONE, 60))
        val ruleset = transferRuleset {
            routeTo(0) {
                region == TransferRegion.PLAYER_MAIN && sourceSlot == 9
            }
            routeTo(1) {
                region == TransferRegion.PLAYER_MAIN && sourceSlot == 9
            }
        }

        val handled = ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        )

        handled shouldBe true
        fixture.inventory.getItemStack(0) shouldBe ItemStack.of(Material.STONE, 64)
        fixture.inventory.getItemStack(1) shouldBe ItemStack.of(Material.STONE, 6)
        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.AIR
        fixture.playerInventory.cursorItem shouldBe cursor
    }

    test("Shift click routes an open inventory source into player inventory slots") {
        val fixture = transferFixture()
        fixture.inventory.setItemStack(0, ItemStack.of(Material.STONE, 5))
        fixture.playerInventory.setItemStack(8, ItemStack.of(Material.STONE, 63))
        val ruleset = transferRuleset {
            routeTo(listOf(8, 9)) {
                region == TransferRegion.OPEN_INVENTORY && sourceSlot == 0
            }
        }

        val handled = ruleset.bindTo(fixture.inventory).shiftClick(fixture.player, 0, 0)

        handled shouldBe true
        fixture.inventory.getItemStack(0) shouldBe ItemStack.AIR
        fixture.playerInventory.getItemStack(8) shouldBe ItemStack.of(Material.STONE, 64)
        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.of(Material.STONE, 4)
    }

    test("A routing tier merges into existing stacks before filling empty slots") {
        val fixture = transferFixture()
        fixture.playerInventory.setItemStack(9, ItemStack.of(Material.STONE, 10))
        fixture.inventory.setItemStack(1, ItemStack.of(Material.STONE, 60))
        val ruleset = transferRuleset {
            routeTo(listOf(0, 1)) { true }
        }

        val handled = ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        )

        handled shouldBe true
        fixture.inventory.getItemStack(0) shouldBe ItemStack.of(Material.STONE, 6)
        fixture.inventory.getItemStack(1) shouldBe ItemStack.of(Material.STONE, 64)
        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.AIR
    }

    test("Shift click distinguishes hotbar and main inventory source regions") {
        val hotbarFixture = transferFixture()
        hotbarFixture.playerInventory.setItemStack(3, ItemStack.of(Material.STONE))
        val mainFixture = transferFixture()
        mainFixture.playerInventory.setItemStack(10, ItemStack.of(Material.STONE))
        val ruleset = transferRuleset {
            routeTo(0) { region == TransferRegion.PLAYER_HOTBAR }
            routeTo(1) { region == TransferRegion.PLAYER_MAIN }
        }

        ruleset.bindTo(hotbarFixture.inventory).shiftClick(
            hotbarFixture.player,
            hotbarFixture.inventory.size + 3,
            0,
        ) shouldBe true
        ruleset.bindTo(mainFixture.inventory).shiftClick(
            mainFixture.player,
            mainFixture.inventory.size + 10,
            0,
        ) shouldBe true

        hotbarFixture.inventory.getItemStack(0) shouldBe ItemStack.of(Material.STONE)
        hotbarFixture.inventory.getItemStack(1) shouldBe ItemStack.AIR
        mainFixture.inventory.getItemStack(0) shouldBe ItemStack.AIR
        mainFixture.inventory.getItemStack(1) shouldBe ItemStack.of(Material.STONE)
    }

    test("Shift click leaves the source untouched when no destination accepts the item") {
        val fixture = transferFixture()
        val source = ItemStack.of(Material.STONE, 5)
        fixture.playerInventory.setItemStack(9, source)
        fixture.inventory.setItemStack(0, ItemStack.of(Material.STONE, 64))
        val ruleset = transferRuleset {
            routeTo(listOf(-1, 0, fixture.inventory.size)) { true }
        }

        val handled = ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        )

        handled shouldBe false
        fixture.playerInventory.getItemStack(9) shouldBe source
        fixture.inventory.getItemStack(0) shouldBe ItemStack.of(Material.STONE, 64)
    }

    test("Vanilla behaviour treats hotbar and main as one reverse-order tier") {
        val fixture = transferFixture()
        fixture.inventory.setItemStack(0, ItemStack.of(Material.STONE, 10))
        fixture.playerInventory.setItemStack(35, ItemStack.of(Material.STONE, 60))
        val ruleset = transferRuleset {
            vanillaBehaviour()
        }

        ruleset.bindTo(fixture.inventory).shiftClick(fixture.player, 0, 0) shouldBe true
        fixture.inventory.getItemStack(0) shouldBe ItemStack.AIR
        fixture.playerInventory.getItemStack(35) shouldBe ItemStack.of(Material.STONE, 64)
        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.of(Material.STONE, 6)
        fixture.playerInventory.getItemStack(8) shouldBe ItemStack.AIR
    }

    test("Vanilla behaviour routes player items through the open inventory in ascending order") {
        val fixture = transferFixture(InventoryType.FURNACE)
        fixture.playerInventory.setItemStack(9, ItemStack.of(Material.STONE, 10))
        fixture.inventory.setItemStack(1, ItemStack.of(Material.STONE, 60))
        val ruleset = transferRuleset {
            vanillaBehaviour()
        }

        ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        ) shouldBe true

        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.AIR
        fixture.inventory.getItemStack(1) shouldBe ItemStack.of(Material.STONE, 64)
        fixture.inventory.getItemStack(0) shouldBe ItemStack.of(Material.STONE, 6)
        fixture.inventory.getItemStack(2) shouldBe ItemStack.AIR
    }

    test("Rules declared before vanilla behaviour override its fallback routing") {
        val fixture = transferFixture(InventoryType.FURNACE)
        fixture.playerInventory.setItemStack(9, ItemStack.of(Material.STONE))
        val ruleset = transferRuleset {
            routeTo(2) { region == TransferRegion.PLAYER_MAIN }
            vanillaBehaviour()
        }

        ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        ) shouldBe true

        fixture.inventory.getItemStack(2) shouldBe ItemStack.of(Material.STONE)
        fixture.inventory.getItemStack(0) shouldBe ItemStack.AIR
    }

    test("Stacks with different components do not merge") {
        val fixture = transferFixture(InventoryType.FURNACE)
        val namedDiamonds = ItemStack.builder(Material.DIAMOND)
            .amount(5)
            .set(DataComponents.CUSTOM_NAME, Component.text("Named diamonds"))
            .build()
        fixture.playerInventory.setItemStack(9, namedDiamonds)
        fixture.inventory.setItemStack(0, ItemStack.of(Material.DIAMOND, 60))
        val ruleset = transferRuleset {
            vanillaBehaviour()
        }

        ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        ) shouldBe true

        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.AIR
        fixture.inventory.getItemStack(0) shouldBe ItemStack.of(Material.DIAMOND, 60)
        fixture.inventory.getItemStack(1) shouldBe namedDiamonds
    }

    test("Custom maximum stack size leaves the correct remainder for the next slot") {
        val fixture = transferFixture(InventoryType.FURNACE)
        val customStack = ItemStack.builder(Material.DIAMOND)
            .set(DataComponents.MAX_STACK_SIZE, 99)
            .build()
        fixture.playerInventory.setItemStack(9, customStack.withAmount(20))
        fixture.inventory.setItemStack(0, customStack.withAmount(90))
        val ruleset = transferRuleset {
            vanillaBehaviour()
        }

        ruleset.bindTo(fixture.inventory).shiftClick(
            fixture.player,
            fixture.inventory.size + 9,
            0,
        ) shouldBe true

        fixture.playerInventory.getItemStack(9) shouldBe ItemStack.AIR
        fixture.inventory.getItemStack(0) shouldBe customStack.withAmount(99)
        fixture.inventory.getItemStack(1) shouldBe customStack.withAmount(11)
    }
}

private fun rulesetRoutingTo(selector: SlotSelector): TransferRuleset =
    TransferRuleset.builder().apply {
        routeTo(selector)
    }.build()

private fun transferContext(sourceSlot: Int = 0): TransferContext = mock {
    on { this.sourceSlot } doReturn sourceSlot
}

private data class TransferFixture(
    val inventory: Inventory,
    val playerInventory: PlayerInventory,
    val player: Player,
)

private fun transferFixture(inventoryType: InventoryType = InventoryType.CHEST_1_ROW): TransferFixture {
    if (MinecraftServer.process() == null) MinecraftServer.init()

    val playerInventory = PlayerInventory()
    val player = mock<Player> {
        on { inventory } doReturn playerInventory
    }
    return TransferFixture(
        inventory = Inventory(inventoryType, "Transfer test"),
        playerInventory = playerInventory,
        player = player,
    )
}

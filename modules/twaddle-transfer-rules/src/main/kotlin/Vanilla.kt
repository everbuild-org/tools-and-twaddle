package org.everbuild.trwaddle.inventory_transfer

internal const val PLAYER_HOTBAR_FIRST_SLOT = 0
internal const val PLAYER_HOTBAR_LAST_SLOT = 8
internal const val PLAYER_MAIN_FIRST_SLOT = 9
internal const val PLAYER_MAIN_LAST_SLOT = 35

internal val PLAYER_MAIN_INVENTORY = (PLAYER_MAIN_FIRST_SLOT..PLAYER_MAIN_LAST_SLOT).toList().toIntArray()
internal val PLAYER_HOTBAR = (PLAYER_HOTBAR_LAST_SLOT downTo PLAYER_HOTBAR_FIRST_SLOT).toList().toIntArray()

internal val vanillaBehaviourRules = listOf(
    TransferRule.RouteTo(
        SlotSelector.SlotList(PLAYER_MAIN_INVENTORY + PLAYER_HOTBAR),
        onlyIf = { it.region == TransferRegion.OPEN_INVENTORY }
    ),
    TransferRule.RouteTo(
        ContextualSlotSelector { context -> 0 until context.inventory.size },
        onlyIf = { it.region == TransferRegion.PLAYER_MAIN || it.region == TransferRegion.PLAYER_HOTBAR }
    ),
    TransferRule.RouteTo(
        SlotSelector.SlotList(PLAYER_MAIN_INVENTORY),
        onlyIf = { it.region == TransferRegion.PLAYER_HOTBAR }
    ),
    TransferRule.RouteTo(
        SlotSelector.SlotList(PLAYER_HOTBAR),
        onlyIf = { it.region == TransferRegion.PLAYER_MAIN }
    ),
)
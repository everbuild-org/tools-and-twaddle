package org.everbuild.twaddle.loot

import net.minestom.server.codec.Codec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.entry.createVanillaLootEntryTypeRegistry
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.loot.modifier.createVanillaLootModifierTypeRegistry

/** Vanilla loot registrations and the codec/evaluator wired to those registrations. */
class VanillaLootSupport private constructor(
    val datapackPrimitives: DatapackPrimitives,
    val entryTypes: Registry<LootEntryType>,
    val modifierTypes: Registry<LootModifierType>,
    val codec: Codec<LootTable>,
    val evaluator: LootTableEvaluator,
) {
    companion object {
        private val standard by lazy(LazyThreadSafetyMode.SYNCHRONIZED, ::create)

        /** Returns a shared vanilla configuration for applications that do not add custom types. */
        @JvmStatic
        fun vanilla(): VanillaLootSupport = standard

        /** Creates an independent configuration containing the vanilla types implemented here. */
        @JvmStatic
        fun create(): VanillaLootSupport {
            val primitives = DatapackPrimitives.vanilla()
            val entryTypes = createVanillaLootEntryTypeRegistry()
            val modifierTypes = createVanillaLootModifierTypeRegistry()
            val codec = createLootTableCodec(
                primitives.contextPredicateCodec,
                entryTypes,
                modifierTypes,
                primitives.intNumberProviderCodec,
                primitives.floatNumberProviderCodec,
                primitives.slotSourceCodec,
            )

            return VanillaLootSupport(
                primitives,
                entryTypes,
                modifierTypes,
                codec,
                LootTableEvaluator(entryTypes, modifierTypes),
            )
        }
    }
}

package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.instrument.Instrument
import net.minestom.server.registry.RegistryKey
import net.minestom.server.registry.TagKey
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.loot.modifier.context.LootModifierRegistriesContext

data class SetInstrumentLootModifier(val options: String) : LootModifier {
    override fun type() = LootModifierKeys.SET_INSTRUMENT
}

internal class SetInstrumentLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("options", Codec.STRING, { (it as SetInstrumentLootModifier).options }, ::SetInstrumentLootModifier)!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val registries = context[LootModifierRegistriesContext]
        val instrumentRegistry = registries.instrument()
        val options = (modifier as SetInstrumentLootModifier).options
        val candidates = if (options.startsWith("#")) {
            val tag = instrumentRegistry.getTag(TagKey.ofHash(options))
                ?: error("No instrument tag registered for $options")
            tag.toList()
        } else {
            val key = RegistryKey.of<Instrument>(Key.key(options))
            check(instrumentRegistry.get(key) != null) { "No instrument registered for $options" }
            listOf(key)
        }
        check(candidates.isNotEmpty()) { "Instrument options '$options' are empty" }
        val selected = candidates[random.nextInt(candidates.size)]
        return item.with(DataComponents.INSTRUMENT, selected)
    }
}

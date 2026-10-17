package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.codec.Transcoder
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.attribute.EnvironmentAttributeMap
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EnvironmentContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class EnvironmentAttributeCheckPredicate(
    val attribute: Key,
    val value: Codec.RawValue,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.ENVIRONMENT_ATTRIBUTE_CHECK

    @Suppress("UNCHECKED_CAST")
    override fun test(context: DataContext, random: Random): Boolean {
        val attributeType = EnvironmentAttribute.values().firstOrNull { it.key() == attribute }
            ?: error("Unknown environment attribute: $attribute")
        val typedAttribute = attributeType as EnvironmentAttribute<Any>
        val environment = context.getBehaviour(EnvironmentContext)
        val entry = environment.entries()[typedAttribute] as? EnvironmentAttributeMap.Entry<Any, Any>
        val actual = entry?.let { it.modifier().modify(typedAttribute.defaultValue(), it.argument()) }
            ?: typedAttribute.defaultValue()
        val expectedTag = value.convertTo(Transcoder.NBT).orElseThrow()
        val actualTag = typedAttribute.valueCodec().encode(Transcoder.NBT, actual).orElseThrow()
        return actualTag == expectedTag
    }
}

internal class EnvironmentAttributeCheckPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "attribute", Codec.KEY, EnvironmentAttributeCheckPredicate::attribute,
        "value", Codec.RAW_VALUE, EnvironmentAttributeCheckPredicate::value,
        ::EnvironmentAttributeCheckPredicate,
    ).asContextPredicateCodec()
}

package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.attribute.EnvironmentAttributeMap
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderKeys
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.context.EnvironmentContext
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

private data class EnvironmentAttributeDeclaration<T>(
    val attribute: Key,
    val arithmetic: NumberProviderArithmetic<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.ENVIRONMENT_ATTRIBUTE

    @Suppress("UNCHECKED_CAST")
    override fun get(context: DataContext, random: Random): T {
        val environment = context.getBehaviour(EnvironmentContext)
        val attributeType = EnvironmentAttribute.values().firstOrNull { it.key() == attribute }
            ?: error("Unknown environment attribute: $attribute")
        val typedAttribute = attributeType as EnvironmentAttribute<Any>
        val entry = environment.entries()[typedAttribute] as? EnvironmentAttributeMap.Entry<Any, Any>
        val value = entry?.let {
            it.modifier().modify(typedAttribute.defaultValue(), it.argument())
        } ?: typedAttribute.defaultValue()
        val number = value as? Number ?: error("Environment attribute $attribute is not numeric")
        return arithmetic.fromFloat(number.toFloat())
    }
}

internal class EnvironmentAttributeNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "attribute", Codec.KEY, { (it as EnvironmentAttributeDeclaration<T>).attribute },
        { attribute -> EnvironmentAttributeDeclaration(attribute, arithmetic) },
    ).asProviderCodec()
}

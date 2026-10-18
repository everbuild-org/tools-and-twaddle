package org.everbuild.twaddle.datapack_primitives

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.number_provider.FLOAT_NUMBER_PROVIDER_ARITHMETIC
import org.everbuild.twaddle.datapack_primitives.number_provider.INT_NUMBER_PROVIDER_ARITHMETIC
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.addAllVanillaNumberProviders
import org.everbuild.twaddle.datapack_primitives.number_provider.createNumberProviderCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.addAllVanillaContextPredicates
import org.everbuild.twaddle.datapack_primitives.predicate.createContextPredicateCodec
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateSubPredicateType
import org.everbuild.twaddle.datapack_primitives.context.addAllVanillaEntityPredicateSubPredicates
import org.everbuild.twaddle.datapack_primitives.context.createEntityPredicateCodec
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceType
import org.everbuild.twaddle.datapack_primitives.slot_source.addAllVanillaSlotSourceTypes
import org.everbuild.twaddle.datapack_primitives.slot_source.createSlotSourceCodec

/** A configured set of datapack primitive registries and their mutually linked codecs. */
class DatapackPrimitives private constructor(
    val intNumberProviderTypes: Registry<NumberProviderType<Int>>,
    val floatNumberProviderTypes: Registry<NumberProviderType<Float>>,
    val contextPredicateTypes: Registry<ContextPredicateType>,
    val entityPredicateSubPredicateTypes: Registry<EntityPredicateSubPredicateType>,
    val slotSourceTypes: Registry<SlotSourceType>,
    val intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    val floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    val contextPredicateCodec: Codec<ContextPredicate>,
    val slotSourceCodec: Codec<SlotSource>,
) {
    companion object {
        /** Creates a builder preloaded with the implemented vanilla primitive types. */
        @JvmStatic
        fun builder() = Builder()

        /** Creates a configured set containing only the implemented vanilla primitive types. */
        @JvmStatic
        fun vanilla() = builder().build()
    }

    /** Registers primitive types and links their codecs when [build] is called. */
    class Builder {
        @Suppress("UnstableApiUsage")
        private val intTypeRegistry = DynamicRegistry.create<NumberProviderType<Int>>(
            Key.key("twaddle:int_number_provider_types"),
        )

        @Suppress("UnstableApiUsage")
        private val floatTypeRegistry = DynamicRegistry.create<NumberProviderType<Float>>(
            Key.key("twaddle:float_number_provider_types"),
        )

        @Suppress("UnstableApiUsage")
        private val predicateTypeRegistry = DynamicRegistry.create<ContextPredicateType>(
            Key.key("twaddle:context_predicate_types"),
        )

        @Suppress("UnstableApiUsage")
        private val entityPredicateSubPredicateTypeRegistry = DynamicRegistry.create<EntityPredicateSubPredicateType>(
            Key.key("twaddle:entity_predicate_subpredicate_types"),
        )

        @Suppress("UnstableApiUsage")
        private val slotSourceTypeRegistry = DynamicRegistry.create<SlotSourceType>(
            Key.key("twaddle:slot_source_types"),
        )

        private var built = false

        init {
            addAllVanillaNumberProviders(intTypeRegistry, INT_NUMBER_PROVIDER_ARITHMETIC)
            addAllVanillaNumberProviders(floatTypeRegistry, FLOAT_NUMBER_PROVIDER_ARITHMETIC)
            addAllVanillaContextPredicates(predicateTypeRegistry)
            addAllVanillaEntityPredicateSubPredicates(entityPredicateSubPredicateTypeRegistry)
            addAllVanillaSlotSourceTypes(slotSourceTypeRegistry)
        }

        /** Registers a custom integer provider type. Registrations must use an unused ID. */
        fun registerIntNumberProviderType(id: Key, type: NumberProviderType<Int>) = apply {
            ensureOpen()
            registerUnique(intTypeRegistry, id, type)
        }

        /** Registers a custom float provider type. Registrations must use an unused ID. */
        fun registerFloatNumberProviderType(id: Key, type: NumberProviderType<Float>) = apply {
            ensureOpen()
            registerUnique(floatTypeRegistry, id, type)
        }

        /** Registers a custom context-predicate type. Registrations must use an unused ID. */
        fun registerContextPredicateType(id: Key, type: ContextPredicateType) = apply {
            ensureOpen()
            registerUnique(predicateTypeRegistry, id, type)
        }

        /** Registers a custom codec for an identifier-keyed entity sub-predicate. */
        fun registerEntityPredicateSubPredicateType(id: Key, type: EntityPredicateSubPredicateType) = apply {
            ensureOpen()
            registerUnique(entityPredicateSubPredicateTypeRegistry, id, type)
        }

        /** Registers a custom slot-source type. Registrations must use an unused ID. */
        fun registerSlotSourceType(id: Key, type: SlotSourceType) = apply {
            ensureOpen()
            registerUnique(slotSourceTypeRegistry, id, type)
        }

        /** Links codecs and closes this builder. The returned registries are exposed read-only. */
        fun build(): DatapackPrimitives {
            ensureOpen()

            lateinit var predicateCodec: Codec<ContextPredicate>
            val predicateCodecRef = Codec.ForwardRef { predicateCodec }
            val floatProviderCodec = createNumberProviderCodec(
                floatTypeRegistry,
                predicateCodecRef,
                FLOAT_NUMBER_PROVIDER_ARITHMETIC,
            )
            val intProviderCodec = createNumberProviderCodec(
                intTypeRegistry,
                predicateCodecRef,
                INT_NUMBER_PROVIDER_ARITHMETIC,
                floatProviderCodec,
            )
            val entityPredicateCodec = createEntityPredicateCodec(entityPredicateSubPredicateTypeRegistry)
            predicateCodec = createContextPredicateCodec(
                predicateTypeRegistry,
                intProviderCodec,
                floatProviderCodec,
                entityPredicateCodec,
            )
            val slotSourceCodec = createSlotSourceCodec(slotSourceTypeRegistry)

            built = true
            return DatapackPrimitives(
                intTypeRegistry,
                floatTypeRegistry,
                predicateTypeRegistry,
                entityPredicateSubPredicateTypeRegistry,
                slotSourceTypeRegistry,
                intProviderCodec,
                floatProviderCodec,
                predicateCodec,
                slotSourceCodec,
            )
        }

        private fun ensureOpen() {
            check(!built) { "Datapack primitives builder has already been built" }
        }

        private fun <T> registerUnique(registry: DynamicRegistry<T>, id: Key, type: T) {
            require(registry.get(id) == null) { "A datapack primitive type is already registered as $id" }
            registry.register(id, type)
        }
    }
}

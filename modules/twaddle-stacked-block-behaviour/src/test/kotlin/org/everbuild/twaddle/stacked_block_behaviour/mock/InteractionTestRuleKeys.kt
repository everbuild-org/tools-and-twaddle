package org.everbuild.twaddle.stacked_block_behaviour.mock

import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.Rule
import org.everbuild.twaddle.stacked_block_behaviour.RuleResult
import org.everbuild.twaddle.stacked_block_behaviour.dsl.RuleChainDsl
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object InteractionTestRuleKeys {
    val JUST_CONTINUE = Rule.Key<InteractionActivityContext>("twaddle_test:just_continue")
    val CANCEL_SET = Rule.Key<InteractionActivityContext>("twaddle_test:cancel_set")
    val MUTATE_PIPELINE = Rule.Key<InteractionActivityContext>("twaddle_test:mutate_pipeline")
}

fun RuleChainDsl<InteractionActivityContext>.justContinue() {
    install(Rule(InteractionTestRuleKeys.JUST_CONTINUE) { RuleResult.next() })
}

fun RuleChainDsl<InteractionActivityContext>.cancelSet() {
    install(Rule(InteractionTestRuleKeys.CANCEL_SET) { RuleResult.consume() })
}

fun RuleChainDsl<InteractionActivityContext>.mutatePipeline(newBlock: Block) {
    install(Rule(InteractionTestRuleKeys.MUTATE_PIPELINE) { RuleResult.cascade(it.copy(block = newBlock)) })
}

@OptIn(ExperimentalUuidApi::class)
fun <T : ActivityContext> RuleChainDsl<T>.record(block: (T) -> Unit) {
    install(Rule(Rule.Key("twaddle_test:record-${Uuid.generateV4()}")) { block(it); RuleResult.next() })
}
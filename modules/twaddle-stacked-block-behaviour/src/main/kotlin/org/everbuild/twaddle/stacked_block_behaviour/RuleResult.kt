package org.everbuild.twaddle.stacked_block_behaviour

/**
 * Describes how rule evaluation should proceed after a rule has been executed.
 *
 * A result controls whether the current rule chain should continue, terminate,
 * or continue using a modified activity context.
 *
 * @param T the activity context processed by the rule chain
 */
sealed interface RuleResult<out T : ActivityContext> {

    /**
     * Continues evaluation with the next rule in the current rule chain.
     */
    data object Next : RuleResult<Nothing>

    /**
     * Stops evaluation of the current rule chain.
     *
     * The activity is considered handled by the rule that returned this result.
     */
    data object Consume : RuleResult<Nothing>

    /**
     * Continues rule evaluation using [newContext] as the context for
     * later rules.
     *
     * This can be used when a rule transforms the current activity and wants
     * later rules to operate on the transformed state.
     *
     * @property newContext the context to use for subsequent rule evaluation
     */
    data class Cascade<T : ActivityContext>(
        val newContext: T,
    ) : RuleResult<T>

    companion object {
        /**
         * Returns a result instructing the rule engine to evaluate the next rule.
         */
        @JvmStatic
        fun <T : ActivityContext> next(): RuleResult<T> = Next

        /**
         * Returns a result instructing the rule engine to stop evaluation.
         */
        @JvmStatic
        fun <T : ActivityContext> consume(): RuleResult<T> = Consume

        /**
         * Returns a result instructing the rule engine to continue evaluation
         * using [newContext].
         */
        @JvmStatic
        fun <T : ActivityContext> cascade(newContext: T): RuleResult<T> =
            Cascade(newContext)
    }
}
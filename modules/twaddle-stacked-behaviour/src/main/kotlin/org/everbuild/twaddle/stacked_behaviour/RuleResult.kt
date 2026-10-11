package org.everbuild.twaddle.stacked_behaviour

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
     * Continues rule evaluation using [newContext] for later rules.
     *
     * This allows rules to reinterpret an activity, for example, by
     * substituting a different block type to reuse its behavior.
     *
     * The new context only affects subsequent rule evaluation.
     * It does not modify the underlying world or activity state.
     *
     * @property newContext the context passed to later rules
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
         * Continues rule evaluation using [newContext] for later rules.
         *
         * This allows rules to reinterpret an activity, for example, by
         * substituting a different block type to reuse its behavior.
         *
         * The new context only affects subsequent rule evaluation.
         * It does not modify the underlying world or activity state.
         */
        @JvmStatic
        fun <T : ActivityContext> cascade(newContext: T): RuleResult<T> =
            Cascade(newContext)
    }
}
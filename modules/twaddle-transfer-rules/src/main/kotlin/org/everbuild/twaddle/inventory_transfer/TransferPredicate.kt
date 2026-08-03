package org.everbuild.twaddle.inventory_transfer

fun interface TransferPredicate {
    fun invoke(context: TransferContext): Boolean

    companion object {
        @JvmField
        val ALWAYS = TransferPredicate { true }
    }
}
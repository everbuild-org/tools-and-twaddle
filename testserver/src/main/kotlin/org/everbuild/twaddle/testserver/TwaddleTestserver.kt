package org.everbuild.twaddle.testserver

import org.everbuild.twaddle.FeatureId
import org.everbuild.twaddle.feature

interface FeatureA
interface FeatureB

object Empty : FeatureA, FeatureB

val featureA = feature<FeatureA>("twaddle-test:a") {
    install {
        return@install Empty
    }
}

val featureBId = FeatureId<FeatureB>("twaddle-test:b")

val featureB = feature<FeatureB>(featureBId) {
    val a = require(featureA.id)

    install {
        return@install a.get() as Empty as FeatureB
    }
}

fun configureFeatureB(config: String) = feature<FeatureB>(featureBId) {
    val a = require(featureA.id)

    install {
        //.. use config ..
        println(config)
        return@install a.get() as Empty as FeatureB
    }
}

interface ConfigB {
    val config: String
}
data class ConfigBImpl(override val config: String) : ConfigB

val configBId = FeatureId<ConfigB>("twaddle-test:b-config")
val configBFeature = feature<ConfigB>(configBId) {
    install { ConfigBImpl("whatever") }
}

val featureBConfigured = feature<FeatureB>("twaddle-test:b") {
    val config = require(configBId)

    install {
        println(config.get().config)
        return@install Empty as FeatureB
    }
}

fun main() {

}
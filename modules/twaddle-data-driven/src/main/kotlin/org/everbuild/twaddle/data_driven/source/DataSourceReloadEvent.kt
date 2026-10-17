package org.everbuild.twaddle.data_driven.source

import net.minestom.server.event.Event
import org.everbuild.twaddle.data_driven.DataDriven
import org.everbuild.twaddle.data_driven.DataSnapshot

/** Emitted synchronously on the thread publishing the affected registry snapshot. */
class DataSourceReloadEvent(
    val registry: DataDriven<*>,
    val snapshot: DataSnapshot<*>,
) : Event
package com.millicast.android_app.compat

import android.util.Log
import com.millicast.publishers.BitrateSettings
import com.voxeet.promise.Promise
import com.voxeet.promise.PromiseDebug
import com.voxeet.promise.solve.Solver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

open abstract class CompatBase() {
    val scope = MainScope();

    fun <T> promise(block: suspend () -> T): Promise<T> {
        return Promise { solver ->
            run{
                GlobalScope.launch {
                    try {
                        solver.resolve(block());
                    } catch (err: Throwable) {
                        solver.reject(err)
                    }
                };
            }
        };
    }


}
class CompatBitrateSettings{
    var disableBWE: Boolean = false

    var maxBitrateKbps: Int? = null;

    var minBitrateKbps : Int? = null


    fun get() : BitrateSettings {
        return BitrateSettings(
                this.disableBWE,
                this.maxBitrateKbps,
                this.minBitrateKbps
        )
    }
}
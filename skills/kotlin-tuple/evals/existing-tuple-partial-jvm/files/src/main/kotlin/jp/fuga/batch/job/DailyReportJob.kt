package jp.fuga.batch.job

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DailyReportJob(
    private val salesApi: suspend () -> Int,
    private val inventoryApi: suspend () -> Int,
    private val customerApi: suspend () -> Int,
) {
    // TODO: 1 つ失敗しても他の結果は使いたい
    suspend fun run(): String = coroutineScope {
        val sales = async { salesApi() }
        val inventory = async { inventoryApi() }
        val customers = async { customerApi() }
        "sales=${sales.await()} inventory=${inventory.await()} customers=${customers.await()}"
    }
}

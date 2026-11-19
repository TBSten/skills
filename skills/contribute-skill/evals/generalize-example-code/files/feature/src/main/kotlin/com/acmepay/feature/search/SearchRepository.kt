package com.acmepay.feature.search

import com.acmepay.core.flow.retryWithBackoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SearchRepository(private val api: SearchApi) {
    fun search(query: String): Flow<List<SearchHit>> =
        flow { emit(api.search(query)) }
            .retryWithBackoff(maxRetries = 2, initialDelayMs = 300)
}

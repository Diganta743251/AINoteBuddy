package com.ainotebuddy.app.search

fun calculateSearchMetrics(results: List<EnhancedSearchResult>): SearchMetrics {
    return SearchMetrics(
        totalResults = results.size,
        semanticResults = results.count { it.semanticSimilarity > 0f },
        contextualResults = results.count { it.contextualRelevance > 0f },
        baseResults = results.count { it.baseRelevance > 0f },
        processingTime = 0L,
        cacheHit = false
    )
}

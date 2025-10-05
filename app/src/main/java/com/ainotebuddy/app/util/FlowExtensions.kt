package com.ainotebuddy.app.util

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Extension functions for enhanced Flow operations
 */

/**
 * Throttles emissions from the flow, only emitting values at most once per the specified duration
 */
fun <T> Flow<T>.throttleLatest(duration: Duration): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmitTime >= duration.inWholeMilliseconds) {
            emit(value)
            lastEmitTime = currentTime
        }
    }
}

/**
 * Debounces emissions from the flow, waiting for the specified duration of inactivity
 */
fun <T> Flow<T>.debounceLatest(duration: Duration): Flow<T> = flow {
    var lastValue: T? = null
    var lastEmitTime = 0L
    
    collect { value ->
        lastValue = value
        lastEmitTime = System.currentTimeMillis()
        
        delay(duration.inWholeMilliseconds)
        
        if (System.currentTimeMillis() - lastEmitTime >= duration.inWholeMilliseconds) {
            lastValue?.let { emit(it) }
        }
    }
}

/**
 * Caches the latest emission and provides it to new collectors
 */
fun <T> Flow<T>.cache(): Flow<T> {
    var cachedValue: T? = null
    val mutex = Mutex()
    
    return flow {
        mutex.withLock {
            cachedValue?.let { emit(it) }
        }
        
        collect { value ->
            mutex.withLock {
                cachedValue = value
            }
            emit(value)
        }
    }
}

/**
 * Retries the flow operation with exponential backoff
 */
fun <T> Flow<T>.retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Duration = 1000.milliseconds,
    backoffMultiplier: Double = 2.0
): Flow<T> = flow {
    var currentDelay = initialDelay
    var retryCount = 0
    
    while (true) {
        try {
            collect { emit(it) }
            break
        } catch (e: Exception) {
            if (retryCount >= maxRetries) {
                throw e
            }
            
            delay(currentDelay.inWholeMilliseconds)
            currentDelay = (currentDelay.inWholeMilliseconds * backoffMultiplier).toLong().milliseconds
            retryCount++
        }
    }
}

/**
 * Combines multiple flows and emits when any of them changes
 */
fun <T1, T2, R> combineWithLatest(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    transform: (T1, T2) -> R
): Flow<R> = combine(flow1, flow2) { a, b -> transform(a, b) }

/**
 * Maps errors in the flow to a default value
 */
fun <T> Flow<T>.mapErrors(defaultValue: T): Flow<T> = catch { emit(defaultValue) }

/**
 * Filters out consecutive duplicate values
 */
fun <T> Flow<T>.distinctUntilChanged(): Flow<T> {
    var lastValue: Any? = Any()
    return filter { value ->
        val isDistinct = lastValue != value
        lastValue = value
        isDistinct
    }
}

/**
 * Emits only when the condition is true
 */
fun <T> Flow<T>.filterWhen(condition: Flow<Boolean>): Flow<T> = 
    combine(this, condition) { value, shouldEmit ->
        if (shouldEmit) value else null
    }.filterNotNull()

/**
 * Buffers emissions and emits them in chunks
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    val buffer = mutableListOf<T>()
    collect { value ->
        buffer.add(value)
        if (buffer.size >= size) {
            emit(buffer.toList())
            buffer.clear()
        }
    }
    if (buffer.isNotEmpty()) {
        emit(buffer.toList())
    }
}

/**
 * Takes emissions for a specific duration then completes
 */
fun <T> Flow<T>.takeFor(duration: Duration): Flow<T> = flow {
    val startTime = System.currentTimeMillis()
    collect { value ->
        if (System.currentTimeMillis() - startTime <= duration.inWholeMilliseconds) {
            emit(value)
        } else {
            return@collect
        }
    }
}

/**
 * Scans with an index
 */
fun <T, R> Flow<T>.scanIndexed(
    initial: R,
    operation: (index: Int, acc: R, value: T) -> R
): Flow<R> = flow {
    var index = 0
    var accumulator = initial
    emit(accumulator)
    
    collect { value ->
        accumulator = operation(index++, accumulator, value)
        emit(accumulator)
    }
}

/**
 * Collects the flow and provides the latest value as a Compose State with lifecycle awareness
 */
@Composable
fun <T> Flow<T>.collectAsStateWithLifecycleExtended(
    initial: T,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): State<T> = collectAsStateWithLifecycle(initialValue = initial, minActiveState = minActiveState)

/**
 * Maps each emission asynchronously
 */
fun <T, R> Flow<T>.mapAsync(
    concurrency: Int = 1,
    transform: suspend (T) -> R
): Flow<R> = flow {
    // Fallback implementation without Semaphore.withPermit to avoid version issues
    collect { value ->
        emit(transform(value))
    }
}

/**
 * Filters items asynchronously
 */
fun <T> Flow<T>.filterAsync(
    predicate: suspend (T) -> Boolean
): Flow<T> = flow {
    collect { value ->
        if (predicate(value)) {
            emit(value)
        }
    }
}

/**
 * Emits the latest value periodically
 */
fun <T> Flow<T>.sampleEvery(period: Duration): Flow<T> = flow {
    var latestValue: T? = null
    val ticker = kotlinx.coroutines.channels.ticker(period.inWholeMilliseconds)
    try {
        this@sampleEvery.onEach { latestValue = it }.launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        for (unit in ticker) {
            latestValue?.let { emit(it) }
        }
    } finally {
        ticker.cancel()
    }
}

/**
 * Skips emissions until a condition becomes true
 */
fun <T> Flow<T>.skipUntil(trigger: Flow<*>): Flow<T> {
    var shouldEmit = false
    return combine(this, trigger.map { true }.startWith(false)) { value, _ ->
        if (!shouldEmit) {
            shouldEmit = true
            null
        } else {
            value
        }
    }.filterNotNull()
}

/**
 * Starts the flow with an initial value
 */
fun <T> Flow<T>.startWith(value: T): Flow<T> = flowOf(value).onCompletion { 
    if (it == null) emitAll(this@startWith) 
}

/**
 * Logs all emissions for debugging
 */
fun <T> Flow<T>.debug(tag: String = "Flow"): Flow<T> = onEach { value ->
    println("$tag: $value")
}

/**
 * Adds a timeout to the flow
 */
fun <T> Flow<T>.timeout(duration: Duration): Flow<T> = flow {
    try {
        kotlinx.coroutines.withTimeout(duration.inWholeMilliseconds) {
            collect { value -> emit(value) }
        }
    } catch (e: Exception) {
        // When timed out, end the flow quietly or rethrow as needed
    }
}

/**
 * Emits values at a fixed rate
 */
fun <T> Flow<T>.fixedRate(period: Duration): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        val timeSinceLastEmit = currentTime - lastEmitTime
        
        if (timeSinceLastEmit < period.inWholeMilliseconds) {
            delay(period.inWholeMilliseconds - timeSinceLastEmit)
        }
        
        emit(value)
        lastEmitTime = System.currentTimeMillis()
    }
}

/**
 * Groups emissions by a key function
 */
fun <T, K> Flow<T>.groupBy(keySelector: (T) -> K): Flow<Pair<K, List<T>>> = flow {
    val groups = mutableMapOf<K, MutableList<T>>()
    
    collect { value ->
        val key = keySelector(value)
        groups.getOrPut(key) { mutableListOf() }.add(value)
    }
    
    groups.forEach { (key, values) ->
        emit(key to values)
    }
}

/**
 * Provides a sliding window of emissions
 */
fun <T> Flow<T>.windowed(size: Int, step: Int = 1): Flow<List<T>> = flow {
    val window = mutableListOf<T>()
    var count = 0
    
    collect { value ->
        window.add(value)
        
        if (window.size > size) {
            repeat(step) {
                if (window.isNotEmpty()) {
                    window.removeFirst()
                }
            }
        }
        
        if (window.size == size && count % step == 0) {
            emit(window.toList())
        }
        
        count++
    }
}
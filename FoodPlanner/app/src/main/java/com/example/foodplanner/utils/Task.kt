package com.example.foodplanner.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T? {
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            if (it.isSuccessful) {
                cont.resume(it.result)
            } else {
                cont.resumeWithException(it.exception!!)
            }
        }
    }
}

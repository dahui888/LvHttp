package com.lvhttp.net.launch

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.lvhttp.net.LvHttp
import com.lvhttp.net.error.CodeException
import com.lvhttp.net.error.ErrorKey
import com.lvhttp.net.response.BaseResponse
import com.lvhttp.net.response.ResultState
import kotlinx.coroutines.*
import java.lang.Exception

/**
 * @name Launch
 * @package com.www.net
 * @author 345 QQ:1831712732
 * @time 2020/6/23 21:33
 * @description
 */

suspend fun <T> launchHttp(
    block: suspend () -> T
): ResultState<T> = tryCatch(block)


suspend fun <T> launchHttpPack(
    block: suspend () -> T,
): ResultState<T> = tryCatch2(block)

fun <T> LifecycleOwner.zipLaunch(
    block: List<suspend () -> T>,
    result: (List<ResultState<T>>) -> Unit
) {
    lifecycleScope.launch {
        val list = arrayListOf<Deferred<ResultState<T>>>()
        block.forEach {
            list.add(
                async {
                    tryCatch(it)
                }
            )
        }
        val data = list.awaitAll()
        launch(Dispatchers.Main) {
            result.invoke(data)
        }
    }
}

private suspend fun <T> tryCatch(block: suspend () -> T): ResultState<T> {
    var t: ResultState<T>
    try {
        val invoke = block.invoke()
        val result = (invoke as BaseResponse<*>).notifyData()
        if (result._code != LvHttp.getCode()) {
            t = ResultState.ErrorState(error = CodeException(result._code, "code 异常"))
            // Code 异常处理
            LvHttp.getErrorDispose(ErrorKey.ErrorCode)?.error?.let {
                it(CodeException(result._code, result._message ?: "code 异常"))
            }
        } else {
            t = ResultState.SuccessState(invoke)
        }
    } catch (e: Exception) {
        t = ResultState.ErrorState(error = e)
        // 自动匹配异常
        ErrorKey.values().forEach {
            if (it.name == e::class.java.simpleName) {
                LvHttp.getErrorDispose(it)?.error?.let { it(e) }
            }
        }
        // 如果全局异常启用
        LvHttp.getErrorDispose(ErrorKey.AllEexeption)?.error?.let {
            it(e)
            return t
        }
    }
    return t
}

private suspend fun <T> tryCatch2(block: suspend () -> T): ResultState<T> {
    var t: ResultState<T>
    try {
        val result = block.invoke()
        t = ResultState.SuccessState(result)
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            t = ResultState.ErrorState(error = e)
            // 如果全局异常启用
            LvHttp.getErrorDispose(ErrorKey.AllEexeption)?.error?.let {
                it(e)
                return@withContext
            }
            // 自动匹配异常
            ErrorKey.values().forEach {
                if (it.name == e::class.java.simpleName) {
                    LvHttp.getErrorDispose(it)?.error?.let { it(e) }
                    return@withContext
                }
            }
        }
    }
    return t
}
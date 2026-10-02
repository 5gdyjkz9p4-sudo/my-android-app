package com.homeinventory.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val currentOnMove by rememberUpdatedState(onMove)
    val scope = rememberCoroutineScope()
    val dragDropState = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            scope = scope,
            onMove = currentOnMove
        )
    }
    SideEffect {
        dragDropState.onMove = currentOnMove
    }
    return dragDropState
}

class DragDropState(
    val state: LazyListState,
    private val scope: CoroutineScope,
    onMove: (Int, Int) -> Unit
) {
    var onMove: (Int, Int) -> Unit = onMove

    var draggedDistance by mutableFloatStateOf(0f)
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    private var currentTouchPositionY by mutableFloatStateOf(0f)
    private var autoScrollJob: Job? = null

    fun onDragStart(offset: Offset) {
        val visibleItems = state.layoutInfo.visibleItemsInfo
        val item = visibleItems.firstOrNull {
            offset.y.toInt() in it.offset..(it.offset + it.size)
        }
        if (item != null) {
            initiallyDraggedElement = item
            currentIndexOfDraggedItem = item.index
            draggedDistance = 0f
            currentTouchPositionY = offset.y
        }
    }

    fun onDragInterrupted() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        initiallyDraggedElement = null
        currentIndexOfDraggedItem = null
        draggedDistance = 0f
        currentTouchPositionY = 0f
    }

    fun onDrag(offset: Offset) {
        if (currentIndexOfDraggedItem == null) return

        draggedDistance += offset.y
        currentTouchPositionY += offset.y

        checkAndPerformSwap()
        checkAutoScroll()
    }

    private fun checkAutoScroll() {
        val layoutInfo = state.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset
        val viewportHeight = viewportEnd - viewportStart
        if (viewportHeight <= 0) return

        val threshold = (viewportHeight * 0.15f).coerceIn(80f, 200f)
        val distanceFromTop = currentTouchPositionY - viewportStart
        val distanceFromBottom = viewportEnd - currentTouchPositionY

        val needScroll = distanceFromTop < threshold || distanceFromBottom < threshold

        if (needScroll) {
            if (autoScrollJob?.isActive != true) {
                autoScrollJob = scope.launch {
                    while (isActive) {
                        val speed = calculateScrollSpeed()
                        if (speed == 0f) break

                        val consumed = state.scrollBy(speed)
                        if (consumed == 0f) break // 到达列表顶部/底部

                        checkAndPerformSwap()
                        delay(16)
                    }
                }
            }
        } else {
            autoScrollJob?.cancel()
            autoScrollJob = null
        }
    }

    private fun calculateScrollSpeed(): Float {
        val layoutInfo = state.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset
        val viewportHeight = viewportEnd - viewportStart
        if (viewportHeight <= 0) return 0f

        val threshold = (viewportHeight * 0.15f).coerceIn(80f, 200f)
        val distanceFromTop = currentTouchPositionY - viewportStart
        val distanceFromBottom = viewportEnd - currentTouchPositionY

        return when {
            distanceFromTop < threshold -> {
                val ratio = ((threshold - distanceFromTop) / threshold).coerceIn(0f, 1f)
                -(6f + ratio * 20f)
            }
            distanceFromBottom < threshold -> {
                val ratio = ((threshold - distanceFromBottom) / threshold).coerceIn(0f, 1f)
                (6f + ratio * 20f)
            }
            else -> 0f
        }
    }

    private fun checkAndPerformSwap() {
        val current = currentIndexOfDraggedItem ?: return
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return

        val touchYInt = currentTouchPositionY.toInt()

        val target = visibleItems.find { item ->
            touchYInt in item.offset..(item.offset + item.size)
        } ?: if (currentTouchPositionY < visibleItems.first().offset) {
            visibleItems.first()
        } else if (currentTouchPositionY > visibleItems.last().offset + visibleItems.last().size) {
            visibleItems.last()
        } else {
            null
        }

        if (target != null && target.index != current) {
            val totalCount = state.layoutInfo.totalItemsCount
            if (target.index in 0 until totalCount) {
                val from = current
                val to = target.index
                currentIndexOfDraggedItem = to
                onMove(from, to)
            }
        }
    }
}

fun Modifier.dragContainer(dragDropState: DragDropState) = pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
        onDrag = { change, offset ->
            change.consume()
            dragDropState.onDrag(offset)
        },
        onDragStart = { offset ->
            dragDropState.onDragStart(offset)
        },
        onDragEnd = {
            dragDropState.onDragInterrupted()
        },
        onDragCancel = {
            dragDropState.onDragInterrupted()
        }
    )
}
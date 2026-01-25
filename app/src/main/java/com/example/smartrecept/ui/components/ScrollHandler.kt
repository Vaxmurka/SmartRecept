// ScrollHandler.kt
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

class ScrollHandler {
    var isBottomBarVisible by mutableStateOf(true)
    private var previousScrollOffset = 0f
    private var scrollThreshold = 10f // Порог для определения направления
    private var lastScrollTime = 0L
    private val scrollDebounce = 100L // Задержка для стабильности

    fun createNestedScrollConnection(
        onScrollDown: () -> Unit = { isBottomBarVisible = false },
        onScrollUp: () -> Unit = { isBottomBarVisible = true }
    ): NestedScrollConnection {
        return object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                when {
                    delta < -scrollThreshold -> onScrollDown()
                    delta > scrollThreshold -> onScrollUp()
                }
                return Offset.Zero
            }
        }
    }

    // Метод для LazyColumn/LazyListState
//    fun handleLazyListScroll(listState: androidx.compose.foundation.lazy.LazyListState) {
//        val firstVisibleItem = listState.firstVisibleItemIndex
//        val scrollDirection = firstVisibleItem > previousScrollOffset
//        previousScrollOffset = firstVisibleItem.toFloat()
//
//        println("ScrollHandler: $scrollDirection")
//
//        if (scrollDirection) {
//            isBottomBarVisible = false // Вниз
//        } else {
//            isBottomBarVisible = true  // Вверх
//        }
//    }

    fun handleLazyListScroll(listState: LazyListState) {
        val currentTime = System.currentTimeMillis()

        // Защита от слишком частых обновлений
        if (currentTime - lastScrollTime < scrollDebounce) return

        val firstVisibleItemIndex = listState.firstVisibleItemIndex
        val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset

        // Комбинируем индекс и offset для более точного определения направления
        val totalScrollOffset = firstVisibleItemIndex * 1000f + firstVisibleItemScrollOffset

        val delta = totalScrollOffset - previousScrollOffset

        if (Math.abs(delta) > scrollThreshold) {
            val isScrollingDown = delta > 0

            if (isScrollingDown && isBottomBarVisible) {
                // Скролл вниз - скрываем
                isBottomBarVisible = false
                lastScrollTime = currentTime
            } else if (!isScrollingDown && !isBottomBarVisible) {
                // Скролл вверх - показываем
                isBottomBarVisible = true
                lastScrollTime = currentTime
            }

            previousScrollOffset = totalScrollOffset
        }
    }

    fun show() { isBottomBarVisible = true }
    fun hide() { isBottomBarVisible = false }
    fun toggle() { isBottomBarVisible = !isBottomBarVisible }
}

@Composable
fun rememberScrollHandler(): ScrollHandler {
    return remember { ScrollHandler() }
}
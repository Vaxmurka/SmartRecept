// ui/screens/RecipeViewModelFactory.kt
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smartrecept.ui.screens.RecipeViewModel

class RecipeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            return RecipeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
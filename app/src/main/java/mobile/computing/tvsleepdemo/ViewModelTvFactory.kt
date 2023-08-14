package mobile.computing.tvsleepdemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelTvFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViewModelTV::class.java)) {
            return ViewModelTV(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filtros disponibles en la barra de chips (Ejercicio 1, característica 3). */
enum class TaskFilter(val etiqueta: String) {
    TODAS("Todas"),
    PENDIENTES("Pendientes"),
    COMPLETADAS("Completadas")
}

/** Estado completo de la pantalla: un solo objeto que viaja del ViewModel a la Vista. */
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val filtro: TaskFilter = TaskFilter.TODAS,
    val pendientes: Int = 0,
    val completadas: Int = 0
)

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    // Único estado mutable que vive en el ViewModel: el filtro elegido por el usuario
    private val _filtro = MutableStateFlow(TaskFilter.TODAS)

    // El estado de la UI se deriva del Flow de Room + el filtro. La Vista solo lo observa.
    val uiState: StateFlow<TaskUiState> =
        combine(dao.observeAllTasks(), _filtro) { todas, filtro ->
            TaskUiState(
                tasks = when (filtro) {
                    TaskFilter.TODAS -> todas
                    TaskFilter.PENDIENTES -> todas.filter { !it.isCompleted }
                    TaskFilter.COMPLETADAS -> todas.filter { it.isCompleted }
                },
                filtro = filtro,
                pendientes = todas.count { !it.isCompleted },
                completadas = todas.count { it.isCompleted }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskUiState()
        )

    // ---- Eventos que suben desde la Vista (UDF) ----

    fun onFiltroChange(filtro: TaskFilter) {
        _filtro.value = filtro
    }

    fun addTask(description: String) {
        viewModelScope.launch {
            dao.insertTask(Task(description = description.trim()))
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    /** Característica 1: editar el texto de una tarea existente. */
    fun editTask(task: Task, nuevaDescripcion: String) {
        viewModelScope.launch {
            dao.updateTask(task.copy(description = nuevaDescripcion.trim()))
        }
    }

    /** Característica 2: eliminar una tarea individual. */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            dao.deleteAllTasks()
        }
    }

    /** Fábrica para que el ViewModel sobreviva a las recomposiciones y a girar la pantalla. */
    companion object {
        fun factory(dao: TaskDao) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TaskViewModel(dao) as T
        }
    }
}

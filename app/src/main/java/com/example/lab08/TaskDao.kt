package com.example.lab08

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Obtener todas las tareas (consulta puntual, usada en la parte guiada)
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    // Observar la tabla: Room emite una lista nueva cada vez que cambia "tasks"
    @Query("SELECT * FROM tasks ORDER BY is_completed ASC, id ASC")
    fun observeAllTasks(): Flow<List<Task>>

    // Insertar una nueva tarea
    @Insert
    suspend fun insertTask(task: Task)

    // Marcar una tarea como completada / no completada, o cambiar su descripción
    @Update
    suspend fun updateTask(task: Task)

    // Eliminar una sola tarea (Ejercicio 1)
    @Delete
    suspend fun deleteTask(task: Task)

    // Eliminar todas las tareas
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

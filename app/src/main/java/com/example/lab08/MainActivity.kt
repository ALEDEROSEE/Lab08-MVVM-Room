package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {

    // La base de datos se construye UNA sola vez, en onCreate, no dentro de setContent.
    private lateinit var db: TaskDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_db"
        ).build()

        setContent {
            Lab08Theme {
                val viewModel: TaskViewModel = viewModel(
                    factory = TaskViewModel.factory(db.taskDao())
                )
                TaskScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Estado local de la UI: qué diálogo está abierto y qué tarea se está editando
    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var tareaEnEdicion by remember { mutableStateOf<Task?>(null) }
    var mostrarConfirmarBorrarTodo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis tareas", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${uiState.pendientes} pendientes - ${uiState.completadas} completadas",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarConfirmarBorrarTodo = true }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Eliminar todas")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoNueva = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva tarea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // --- Caracteristica 3: filtrar tareas ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { filtro ->
                    FilterChip(
                        selected = uiState.filtro == filtro,
                        onClick = { viewModel.onFiltroChange(filtro) },
                        label = { Text(filtro.etiqueta) }
                    )
                }
            }

            if (uiState.tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when (uiState.filtro) {
                            TaskFilter.TODAS -> "Todavia no tienes tareas. Toca + para crear la primera."
                            TaskFilter.PENDIENTES -> "No te queda nada pendiente."
                            TaskFilter.COMPLETADAS -> "Aun no has completado ninguna tarea."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { tareaEnEdicion = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoNueva) {
        TaskDialog(
            titulo = "Nueva tarea",
            textoInicial = "",
            onConfirm = { texto ->
                viewModel.addTask(texto)
                mostrarDialogoNueva = false
            },
            onDismiss = { mostrarDialogoNueva = false }
        )
    }

    // --- Caracteristica 1: editar el texto de una tarea existente ---
    tareaEnEdicion?.let { task ->
        TaskDialog(
            titulo = "Editar tarea",
            textoInicial = task.description,
            onConfirm = { texto ->
                viewModel.editTask(task, texto)
                tareaEnEdicion = null
            },
            onDismiss = { tareaEnEdicion = null }
        )
    }

    if (mostrarConfirmarBorrarTodo) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarBorrarTodo = false },
            title = { Text("Eliminar todas las tareas?") },
            text = { Text("Esta accion borra la tabla completa y no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllTasks()
                    mostrarConfirmarBorrarTodo = false
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarBorrarTodo = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            leadingContent = {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle
                        else Icons.Outlined.CheckCircle,
                        contentDescription = if (task.isCompleted) "Marcar como pendiente"
                        else "Marcar como completada",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            headlineContent = {
                Text(
                    text = task.description,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            },
            trailingContent = {
                // --- Caracteristica 2: eliminar una tarea individual ---
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Eliminar tarea",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onEdit)
        )
    }
}

@Composable
private fun TaskDialog(
    titulo: String,
    textoInicial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by remember { mutableStateOf(textoInicial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            TextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Descripcion") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (texto.isNotBlank()) onConfirm(texto) },
                enabled = texto.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

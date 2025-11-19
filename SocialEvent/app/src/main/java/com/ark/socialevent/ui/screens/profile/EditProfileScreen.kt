package com.ark.socialevent.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.UserProfile
import com.ark.socialevent.network.UserRepository
import androidx.compose.foundation.text.KeyboardOptions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userRepository: UserRepository,
    currentProfile: UserProfile?,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var firstName by remember { mutableStateOf(currentProfile?.firstName ?: "") }
    var lastName by remember { mutableStateOf(currentProfile?.lastName ?: "") }
    var gender by remember { mutableStateOf(currentProfile?.gender ?: "") }
    var age by remember { mutableStateOf(currentProfile?.age?.toString() ?: "") }
    var phone by remember { mutableStateOf(currentProfile?.phone ?: "") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Редактирование профиля") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (validateForm(firstName, lastName, gender, age, phone)) {
                                    isLoading = true
                                    errorMessage = null
                                    successMessage = null

                                    // РЕАЛЬНЫЙ ВЫЗОВ API вместо заглушки
                                    userRepository.updateProfile(
                                        firstName = firstName,
                                        lastName = lastName,
                                        gender = gender,
                                        age = age.toInt(),
                                        phone = phone
                                    ) { success, message, updatedUser ->
                                        isLoading = false
                                        if (success) {
                                            successMessage = message ?: "Профиль успешно обновлен!"
                                        } else {
                                            errorMessage = message ?: "Ошибка обновления профиля"
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Сохранить")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (successMessage != null) {
                ExtendedFloatingActionButton(
                    onClick = onSaveSuccess,
                    icon = { Icon(Icons.Default.Check, contentDescription = "Готово") },
                    text = { Text("Готово") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Сообщения об ошибках/успехе
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Форма редактирования
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Основная информация",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = firstName.isBlank(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Фамилия") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = lastName.isBlank(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Пол") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать пол")
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = age,
                        onValueChange = { newAge ->
                            if (newAge.all { it.isDigit() } && newAge.length <= 3) {
                                age = newAge
                            }
                        },
                        label = { Text("Возраст") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = age.isBlank() || age.toIntOrNull() == null,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            // Социальные сети (заглушка)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Социальные сети",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "Функция редактирования социальных сетей будет добавлена позже",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Для FAB
        }
    }
}

private fun validateForm(
    firstName: String,
    lastName: String,
    gender: String,
    age: String,
    phone: String
): Boolean {
    if (firstName.isBlank()) {
        return false
    }
    if (lastName.isBlank()) {
        return false
    }
    if (gender.isBlank()) {
        return false
    }
    if (age.isBlank() || age.toIntOrNull() == null || age.toInt() < 1 || age.toInt() > 120) {
        return false
    }
    if (phone.isBlank()) {
        return false
    }
    return true
}
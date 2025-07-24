package com.deonvanooijen.loginui.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.deonvanooijen.loginui.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@Composable
fun HomeScreen(auth: FirebaseAuth, navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val user = auth.currentUser
    val email = user?.email ?: "No email"
    val uid = user?.uid.orEmpty()

    // DisplayName states
    var displayName by remember { mutableStateOf(user?.displayName ?: "User") }
    var editingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(TextFieldValue(displayName)) }

    // Age states (String for easy editing)
    var age by remember { mutableStateOf("") }
    var editingAge by remember { mutableStateOf(false) }
    var tempAge by remember { mutableStateOf(TextFieldValue("")) }

    // Location states
    var location by remember { mutableStateOf("") }
    var editingLocation by remember { mutableStateOf(false) }
    var tempLocation by remember { mutableStateOf(TextFieldValue("")) }

    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingImagePick by remember { mutableStateOf(false) }

    // Firestore instance
    val firestore = FirebaseFirestore.getInstance()

    // Load user data from Firestore on start
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    profileImageUrl = doc.getString("profilePictureUrl")
                    age = doc.getString("age") ?: ""
                    tempAge = TextFieldValue(age)
                    location = doc.getString("location") ?: ""
                    tempLocation = TextFieldValue(location)
                }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && uid.isNotEmpty()) {
            val storageRef = FirebaseStorage.getInstance()
                .reference.child("profile_pictures/$uid.jpg")

            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    profileImageUrl = downloadUri.toString() + "?t=${System.currentTimeMillis()}"

                    firestore.collection("users")
                        .document(uid)
                        .set(mapOf("profilePictureUrl" to downloadUri.toString()), SetOptions.merge())
                }
            }.addOnFailureListener {
                Log.e("Upload", "Failed: ${it.message}")
            }
        }
    }

    fun requestPermissionAndLaunchPicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(permission),
                    123
                )
                pendingImagePick = true
                return
            }
        }
        launcher.launch("image/*")
    }

    val currentContext by rememberUpdatedState(context)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingImagePick) {
                pendingImagePick = false
                if (ContextCompat.checkSelfPermission(
                        currentContext,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    launcher.launch("image/*")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_image2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(60.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(16.dp))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hello, $displayName!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable {
                                requestPermissionAndLaunchPicker()
                            }
                    ) {
                        val painter = if (!profileImageUrl.isNullOrEmpty()) {
                            rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(profileImageUrl)
                                    .crossfade(true)
                                    .build()
                            )
                        } else {
                            painterResource(id = R.drawable.profile_placeholder)
                        }

                        Image(
                            painter = painter,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Display Name Edit & View
                    if (editingName) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.White,
                                unfocusedIndicatorColor = Color.LightGray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                tempName = TextFieldValue(displayName)
                                editingName = false
                            }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = {
                                val newName = tempName.text.trim()
                                if (user != null && newName.isNotEmpty()) {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(newName)
                                        .build()
                                    user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            displayName = newName
                                            editingName = false

                                            firestore.collection("users")
                                                .document(uid)
                                                .set(mapOf("displayName" to newName), SetOptions.merge())
                                        }
                                    }
                                } else {
                                    editingName = false
                                }
                            }) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    editingName = true
                                    tempName = TextFieldValue(displayName)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Edit display name",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email (read only)
                    Text("Email: $email", fontSize = 16.sp, color = Color.White)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Age Edit & View
                    if (editingAge) {
                        OutlinedTextField(
                            value = tempAge,
                            onValueChange = { tempAge = it },
                            label = { Text("Age") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.White,
                                unfocusedIndicatorColor = Color.LightGray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                tempAge = TextFieldValue(age)
                                editingAge = false
                            }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = {
                                val newAge = tempAge.text.trim()
                                if (uid.isNotEmpty()) {
                                    firestore.collection("users")
                                        .document(uid)
                                        .set(mapOf("age" to newAge), SetOptions.merge())
                                        .addOnSuccessListener {
                                            age = newAge
                                            editingAge = false
                                        }
                                        .addOnFailureListener {
                                            Log.e("Firestore", "Failed to update age: ${it.message}")
                                        }
                                } else {
                                    editingAge = false
                                }
                            }) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    editingAge = true
                                    tempAge = TextFieldValue(age)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (age.isNotBlank()) "Age: $age" else "Age: -",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Edit age",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location Edit & View
                    if (editingLocation) {
                        OutlinedTextField(
                            value = tempLocation,
                            onValueChange = { tempLocation = it },
                            label = { Text("Location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.White,
                                unfocusedIndicatorColor = Color.LightGray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                tempLocation = TextFieldValue(location)
                                editingLocation = false
                            }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = {
                                val newLocation = tempLocation.text.trim()
                                if (uid.isNotEmpty()) {
                                    firestore.collection("users")
                                        .document(uid)
                                        .set(mapOf("location" to newLocation), SetOptions.merge())
                                        .addOnSuccessListener {
                                            location = newLocation
                                            editingLocation = false
                                        }
                                        .addOnFailureListener {
                                            Log.e("Firestore", "Failed to update location: ${it.message}")
                                        }
                                } else {
                                    editingLocation = false
                                }
                            }) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    editingLocation = true
                                    tempLocation = TextFieldValue(location)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (location.isNotBlank()) "Location: $location" else "Location: -",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Edit location",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                Text("Log Out")
            }
        }
    }
}
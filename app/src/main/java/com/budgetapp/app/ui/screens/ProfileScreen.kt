package com.budgetapp.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.ui.components.ProfileAvatar
import com.budgetapp.app.ui.components.StandardLightHeader
import com.budgetapp.app.ui.theme.ChapBackground
import com.budgetapp.app.ui.theme.ChapMaterialSurface
import com.budgetapp.app.ui.theme.ChapPrimaryAlt
import com.budgetapp.app.ui.theme.ChapSurfacePale
import com.budgetapp.app.ui.theme.ChapSurfaceTintAlt
import com.budgetapp.app.ui.theme.ChapTextLight
import com.budgetapp.app.ui.theme.ChapTextMutedBlue
import com.budgetapp.app.ui.theme.ChapWhite
import com.budgetapp.app.security.LocalTrustedExternalActivityCoordinator
import com.budgetapp.app.security.launchTrustedExternalActivity

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persistedSettings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val externalActivityCoordinator = LocalTrustedExternalActivityCoordinator.current
    var name by rememberSaveable { mutableStateOf(persistedSettings.name) }
    var avatarUri by rememberSaveable { mutableStateOf(persistedSettings.avatarUri) }
    var currency by rememberSaveable { mutableStateOf(persistedSettings.currency) }
    var dateFormat by rememberSaveable { mutableStateOf(persistedSettings.dateFormat) }
    var firstDayOfWeek by rememberSaveable { mutableStateOf(persistedSettings.firstDayOfWeek) }
    var monthStartDay by rememberSaveable { mutableStateOf(persistedSettings.monthStartDay) }
    var isSaved by rememberSaveable { mutableStateOf(false) }
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        externalActivityCoordinator.end()
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            avatarUri = uri.toString()
            isSaved = false
        }
    }

    // No LaunchedEffect syncing the form from persistedSettings here: the repository reads the
    // stored values once at startup, so the fields below are already seeded from them, and an
    // effect keyed on the flow would overwrite in-progress edits (including after a rotation).

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(ChapBackground),
    ) {
        StandardLightHeader(
            title = "Profile",
            heading = "Set your budget basics",
            copy = "Choose the profile settings used across your envelopes.",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 164.dp),
        ) {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(ChapWhite)
                            .border(1.dp, ChapSurfacePale, RoundedCornerShape(24.dp)),
                ) {
                    ProfileInputBlock(
                        label = "Name",
                        value = name,
                        onValueChange = {
                            name = it
                            isSaved = false
                        },
                    )
                    ProfileAvatarBlock(
                        name = name,
                        avatarUri = avatarUri,
                        onChangePicture = {
                            externalActivityCoordinator.launchTrustedExternalActivity {
                                avatarLauncher.launch(arrayOf("image/*"))
                            }
                        },
                    )
                    ProfileSelectBlock(
                        label = "Currency",
                        value = currency,
                        options = listOf("EUR - Euro", "USD - US Dollar", "GBP - British Pound", "PLN - Polish Zloty"),
                        onSelect = {
                            currency = it
                            isSaved = false
                        },
                    )
                    ProfileSelectBlock(
                        label = "Date format",
                        value = dateFormat,
                        options = listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD"),
                        onSelect = {
                            dateFormat = it
                            isSaved = false
                        },
                    )
                    ProfileSelectBlock(
                        label = "First day of week",
                        value = firstDayOfWeek,
                        options = listOf("Monday", "Sunday"),
                        onSelect = {
                            firstDayOfWeek = it
                            isSaved = false
                        },
                    )
                    ProfileSelectBlock(
                        label = "Month start day",
                        value = monthStartDay,
                        options = listOf("1st of the month", "15th of the month", "Custom"),
                        onSelect = {
                            monthStartDay = it
                            isSaved = false
                        },
                        isLast = true,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.save(
                            ProfileSettings(
                                name = name,
                                avatarUri = avatarUri,
                                currency = currency,
                                dateFormat = dateFormat,
                                firstDayOfWeek = firstDayOfWeek,
                                monthStartDay = monthStartDay,
                            ),
                        )
                        isSaved = true
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChapPrimaryAlt, contentColor = ChapWhite),
                ) {
                    Text("Save profile", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                if (isSaved) {
                    Text(
                        text = "Profile saved",
                        color = ChapPrimaryAlt,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSelectBlock(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    isLast: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (!isLast) Modifier.border(0.dp, ChapSurfacePale) else Modifier)
                .padding(16.dp),
    ) {
        Text(label, color = ChapTextMutedBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(9.dp))
        Box {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChapSurfaceTintAlt)
                        .border(1.dp, ChapSurfacePale, RoundedCornerShape(14.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    color = ChapMaterialSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("v", color = ChapTextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier =
                    Modifier
                        .background(ChapWhite)
                        .border(1.dp, ChapSurfacePale, RoundedCornerShape(14.dp)),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = ChapMaterialSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInputBlock(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isLast: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (!isLast) Modifier.border(0.dp, ChapSurfacePale) else Modifier)
                .padding(16.dp),
    ) {
        Text(label, color = ChapTextMutedBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(9.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = ChapMaterialSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChapSurfaceTintAlt)
                    .border(1.dp, ChapSurfacePale, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
            decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() } },
        )
    }
}

@Composable
private fun ProfileAvatarBlock(
    name: String,
    avatarUri: String?,
    onChangePicture: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            name = name,
            avatarUri = avatarUri,
            size = 58.dp,
        )
        Text(
            text = "Change picture",
            color = ChapPrimaryAlt,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onChangePicture),
        )
    }
}

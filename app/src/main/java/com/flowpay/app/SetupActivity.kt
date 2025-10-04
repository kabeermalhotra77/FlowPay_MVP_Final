package com.flowpay.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowpay.app.ui.theme.FlowPayTheme
import com.flowpay.app.helpers.SetupHelper

class SetupActivity : ComponentActivity() {
    private lateinit var setupHelper: SetupHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize setup helper
        setupHelper = SetupHelper(this, object : SetupHelper.UICallback {
            override fun showToast(message: String) {
                runOnUiThread { Toast.makeText(this@SetupActivity, message, Toast.LENGTH_LONG).show() }
            }
            
            override fun navigateToTestConfiguration() {
                val intent = Intent(this@SetupActivity, TestConfigurationActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
        
        setContent {
            FlowPayTheme {
                SetupScreen(setupHelper = setupHelper)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(setupHelper: SetupHelper) {
    var mobileNumber by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("") }
    var selectedPrimarySim by remember { mutableStateOf("") }
    var selectedSecondarySim by remember { mutableStateOf("") }
    var isDualSimEnabled by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Use helper methods for data
    val banks = setupHelper.getBanks()
    val simCarriers = setupHelper.getSimCarriers()
    val secondarySimOptions = setupHelper.getSecondarySimOptions(selectedPrimarySim)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Card
            HeaderCard()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Mobile Number Section
            MobileNumberSection(
                mobileNumber = mobileNumber,
                onMobileNumberChange = { mobileNumber = it },
                setupHelper = setupHelper
            )
            
            Spacer(modifier = Modifier.height(45.dp))
            
            // Bank Selection Section
            BankSelectionSection(
                banks = banks,
                selectedBank = selectedBank,
                onBankSelected = { selectedBank = it }
            )
            
            Spacer(modifier = Modifier.height(45.dp))
            
            // SIM Card Selection Section
            SimCardSelectionSection(
                simCarriers = simCarriers,
                selectedPrimarySim = selectedPrimarySim,
                selectedSecondarySim = selectedSecondarySim,
                isDualSimEnabled = isDualSimEnabled,
                onPrimarySimSelected = { selectedPrimarySim = it },
                onSecondarySimSelected = { selectedSecondarySim = it },
                onDualSimToggled = { isDualSimEnabled = it },
                secondarySimOptions = secondarySimOptions
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Complete Setup Button
            CompleteSetupButton(
                onCompleteSetup = {
                    val setupData = SetupHelper.SetupData(
                        mobileNumber = mobileNumber,
                        selectedBank = selectedBank,
                        selectedPrimarySim = selectedPrimarySim,
                        isDualSimEnabled = isDualSimEnabled,
                        selectedSecondarySim = selectedSecondarySim
                    )
                    setupHelper.completeSetup(setupData)
                }
            )
        }
    }
}

@Composable
fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E8E8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp, 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            Box(
                modifier = Modifier
                    .size(50.dp, 40.dp)
                    .background(
                        color = Color(0xFF2A2A2A),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(35.dp, 3.dp)
                        .background(
                            color = Color(0xFFE8E8E8),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(25.dp))
            
            Text(
                text = stringResource(R.string.setup_flowpay),
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF000000),
                letterSpacing = (-0.5).sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.configure_upi_payments),
                fontSize = 18.sp,
                color = Color(0xFF4A4A4A),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun MobileNumberSection(
    mobileNumber: String,
    onMobileNumberChange: (String) -> Unit,
    setupHelper: SetupHelper
) {
    Column {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = PhoneIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Text(
                text = stringResource(R.string.mobile_number),
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
        }
        
        Spacer(modifier = Modifier.height(15.dp))
        
        Text(
            text = "Mobile Number",
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 44.dp, bottom = 12.dp)
        )
        
        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { newValue ->
                val filtered = setupHelper.formatMobileNumberInput(newValue)
                onMobileNumberChange(filtered)
            },
            placeholder = { 
                Text(
                    text = "10-digit mobile number",
                    color = Color(0xFF666666),
                    fontSize = 17.sp
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF555555),
                unfocusedBorderColor = Color(0xFF333333),
                focusedContainerColor = Color(0xFF222222),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 17.sp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectionSection(
    banks: List<Pair<String, String>>,
    selectedBank: String,
    onBankSelected: (String) -> Unit
) {
    Column {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = CardIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Text(
                text = stringResource(R.string.bank_selection),
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
        }
        
        Text(
            text = stringResource(R.string.choose_primary_bank),
            fontSize = 18.sp,
            color = Color(0xFF888888),
            modifier = Modifier.padding(start = 44.dp, top = 15.dp, bottom = 25.dp)
        )
        
        Text(
            text = "Select Bank",
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 44.dp, bottom = 12.dp)
        )
        
        var expanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = banks.find { it.first == selectedBank }?.second ?: "Choose your bank",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF555555),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedContainerColor = Color(0xFF222222),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedTrailingIconColor = Color.White,
                    unfocusedTrailingIconColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 17.sp),
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                banks.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 17.sp
                            ) 
                        },
                        onClick = {
                            onBankSelected(value)
                            expanded = false
                        },
                        modifier = Modifier.background(Color(0xFF2A2A2A))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimCardSelectionSection(
    simCarriers: List<Pair<String, String>>,
    selectedPrimarySim: String,
    selectedSecondarySim: String,
    isDualSimEnabled: Boolean,
    onPrimarySimSelected: (String) -> Unit,
    onSecondarySimSelected: (String) -> Unit,
    onDualSimToggled: (Boolean) -> Unit,
    secondarySimOptions: List<Pair<String, String>>
) {
    Column {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = SimCardIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Text(
                text = stringResource(R.string.sim_card_selection),
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
        }
        
        Text(
            text = stringResource(R.string.configure_sim_cards),
            fontSize = 18.sp,
            color = Color(0xFF888888),
            modifier = Modifier.padding(start = 44.dp, top = 15.dp, bottom = 25.dp)
        )
        
        Text(
            text = "Primary SIM",
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 44.dp, bottom = 12.dp)
        )
        
        // Primary SIM Selection
        var primaryExpanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = primaryExpanded,
            onExpandedChange = { primaryExpanded = !primaryExpanded }
        ) {
            OutlinedTextField(
                value = simCarriers.find { it.first == selectedPrimarySim }?.second ?: "Select your primary SIM",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF555555),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedContainerColor = Color(0xFF222222),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedTrailingIconColor = Color.White,
                    unfocusedTrailingIconColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 17.sp),
            )
            
            ExposedDropdownMenu(
                expanded = primaryExpanded,
                onDismissRequest = { primaryExpanded = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                simCarriers.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 17.sp
                            ) 
                        },
                        onClick = {
                            onPrimarySimSelected(value)
                            primaryExpanded = false
                        },
                        modifier = Modifier.background(Color(0xFF2A2A2A))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Dual SIM Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDualSimToggled(!isDualSimEnabled) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 2.dp,
                        color = if (isDualSimEnabled) Color(0xFF4A90E2) else Color(0xFF666666),
                        shape = CircleShape
                    )
                    .background(
                        color = if (isDualSimEnabled) Color(0xFF4A90E2) else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDualSimEnabled) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(15.dp))
            
            Text(
                text = stringResource(R.string.enable_dual_sim),
                fontSize = 18.sp,
                color = Color.White
            )
        }
        
        // Secondary SIM Section
        if (isDualSimEnabled) {
            Spacer(modifier = Modifier.height(30.dp))
            
            Divider(
                color = Color(0xFF333333),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(25.dp))
            
            // Secondary SIM Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = SimCardIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Text(
                    text = stringResource(R.string.secondary_sim),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(25.dp))
            
            Text(
                text = "Secondary SIM",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 44.dp, bottom = 12.dp)
            )
            
            // Secondary SIM Selection
            var secondaryExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = secondaryExpanded,
                onExpandedChange = { secondaryExpanded = !secondaryExpanded }
            ) {
                OutlinedTextField(
                    value = secondarySimOptions.find { it.first == selectedSecondarySim }?.second ?: "Select your secondary SIM",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = secondaryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF555555),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedTrailingIconColor = Color.White,
                        unfocusedTrailingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 17.sp),
                )
                
                ExposedDropdownMenu(
                    expanded = secondaryExpanded,
                    onDismissRequest = { secondaryExpanded = false },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    secondarySimOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    label,
                                    color = Color.White,
                                    fontSize = 17.sp
                                ) 
                            },
                            onClick = {
                                onSecondarySimSelected(value)
                                secondaryExpanded = false
                            },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompleteSetupButton(
    onCompleteSetup: () -> Unit
) {
    Button(
        onClick = onCompleteSetup,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        shape = RoundedCornerShape(25.dp)
    ) {
        Text(
            text = stringResource(R.string.complete_setup),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF000000),
            letterSpacing = (-0.3).sp
        )
    }
}


// Custom SVG-style Icons
val PhoneIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Phone body (rectangular)
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(5f, 2f)
                lineTo(19f, 2f)
                arcTo(2f, 2f, 0f, true, true, 21f, 4f)
                lineTo(21f, 20f)
                arcTo(2f, 2f, 0f, true, true, 19f, 22f)
                lineTo(5f, 22f)
                arcTo(2f, 2f, 0f, true, true, 3f, 20f)
                lineTo(3f, 4f)
                arcTo(2f, 2f, 0f, true, true, 5f, 2f)
                close()
            }
            // Home button indicator
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                lineTo(12f, 18f)
            }
        }.build()
    }

val CardIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "card",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Credit card outline
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(1f, 4f)
                lineTo(23f, 4f)
                arcTo(2f, 2f, 0f, true, true, 23f, 6f)
                lineTo(23f, 18f)
                arcTo(2f, 2f, 0f, true, true, 21f, 20f)
                lineTo(3f, 20f)
                arcTo(2f, 2f, 0f, true, true, 1f, 18f)
                lineTo(1f, 6f)
                arcTo(2f, 2f, 0f, true, true, 1f, 4f)
                close()
            }
            // Card stripe line
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(1f, 10f)
                lineTo(23f, 10f)
            }
        }.build()
    }

val SimCardIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "sim",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // SIM card outline with cut corner
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(19.5f, 3f)
                lineTo(8.5f, 3f)
                lineTo(3f, 8.5f)
                lineTo(3f, 21f)
                lineTo(21f, 21f)
                lineTo(21f, 3f)
                close()
            }
            // Cut corner detail
            path(
                fill = null,
                stroke = androidx.compose.ui.graphics.SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(3f, 8.5f)
                lineTo(8.5f, 8.5f)
                lineTo(8.5f, 3f)
            }
        }.build()
    }

// Preview removed due to complex helper dependency
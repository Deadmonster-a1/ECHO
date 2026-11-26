package com.spotify.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.BuildConfig
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoTonalIconBadge
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.legal.GravitLegalCatalog
import com.spotify.clone.core.legal.LegalDocumentTab

/**
 * Full-height Material 3 Expressive modal reader for all GRAVIT legal notices,
 * Privacy Policy, Terms & Conditions, Copyright/DMCA, Permissions, and Open-Source Licenses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentSheet(
    initialTab: LegalDocumentTab = LegalDocumentTab.PRIVACY,
    onDismiss: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetBg = MaterialTheme.colorScheme.surfaceContainerLow
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val clauses = remember(activeTab) { GravitLegalCatalog.clausesFor(activeTab) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = EchoRadius.xl, topEnd = EchoRadius.xl),
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EchoTonalIconBadge(
                    icon = iconForLegalTab(activeTab),
                    size = 44.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTab.fullTitle,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${GravitLegalCatalog.OWNER_NAME} Legal • ${GravitLegalCatalog.EFFECTIVE_DATE}",
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal tab pills so users can review every legal policy in one place
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegalDocumentTab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                activeTab = tab
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab.shortTitle,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else textSecondary,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = activeTab.subtitle,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                clauses.forEach { clause ->
                    EchoInsetCard(
                        shape = RoundedCornerShape(EchoRadius.lg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = clause.heading,
                                color = textPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = clause.body,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                // Reserved copyright & publisher footer inside every legal tab
                EchoInsetCard(
                    shape = RoundedCornerShape(EchoRadius.lg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "${GravitLegalCatalog.APP_BRAND} by ${GravitLegalCatalog.OWNER_NAME}",
                            color = primaryColor,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = GravitLegalCatalog.COPYRIGHT_NOTICE,
                            color = textPrimary,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Text(
                            text = "Legal & DMCA: ${GravitLegalCatalog.LEGAL_EMAIL}  •  Support: ${GravitLegalCatalog.SUPPORT_EMAIL}\nVersion ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun iconForLegalTab(tab: LegalDocumentTab): ImageVector = when (tab) {
    LegalDocumentTab.PRIVACY -> Icons.Outlined.PrivacyTip
    LegalDocumentTab.TERMS -> Icons.Outlined.Description
    LegalDocumentTab.COPYRIGHT -> Icons.Outlined.Copyright
    LegalDocumentTab.PERMISSIONS -> Icons.Outlined.Security
    LegalDocumentTab.LICENSES -> Icons.Outlined.Code
}

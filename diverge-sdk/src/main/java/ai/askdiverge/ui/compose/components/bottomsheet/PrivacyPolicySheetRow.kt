package ai.askdiverge.ui.compose.components.bottomsheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons

@Composable
internal fun PrivacyPolicySheetRow(
    text: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            tint = contentColor,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = trailingIcon,
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPolicySheetRowPreview() {
    PrivacyPolicySheetRow(
        leadingIcon = ChatbotIcons.ShieldCheck,
        text = "Privacy Policy",
        contentColor = MaterialTheme.colorScheme.onBackground,
        trailingIcon = ChatbotIcons.ArrowUpRight,
        onClick = {}
    )
}

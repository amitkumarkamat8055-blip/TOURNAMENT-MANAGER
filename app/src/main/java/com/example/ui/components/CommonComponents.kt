package com.example.ui.components

import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.AdminPanelSettings


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrophyGold

@Composable
fun AppHeader(
  title: String,
  subtitle: String? = null,
  walletBalance: Int? = null,
  onProfileClick: (() -> Unit)? = null,
  onWalletClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  logoIcon: ImageVector = Icons.Default.SportsEsports
) {
  val screenWidth = LocalConfiguration.current.screenWidthDp
  val horizontalPadding = when {
    screenWidth >= 840 -> 24.dp
    screenWidth >= 600 -> 20.dp
    else -> 16.dp
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = horizontalPadding, end = horizontalPadding, top = 10.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            Brush.linearGradient(
              listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
          )
          .clickable { onProfileClick?.invoke() }
          .testTag("app_header_logo"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = logoIcon,
          contentDescription = "App Logo",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
          ),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    if (walletBalance != null) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .defaultMinSize(minHeight = 36.dp)
          .clickable { if (onWalletClick != null) onWalletClick() else onProfileClick?.invoke() }
          .testTag("header_wallet_badge")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = "Wallet Balance",
            tint = TrophyGold,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "₹$walletBalance",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }
  }
}

@Composable
fun AppSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String = "Search matches, games, maps...",
  modifier: Modifier = Modifier,
  onSearch: () -> Unit = {}
) {
  val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  val screenWidth = LocalConfiguration.current.screenWidthDp
  val horizontalPadding = when {
    screenWidth >= 840 -> 24.dp
    screenWidth >= 600 -> 20.dp
    else -> 16.dp
  }

  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = {
      Text(
        text = placeholder,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
    },
    trailingIcon = {
      AnimatedVisibility(
        visible = query.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        IconButton(
          onClick = {
            onQueryChange("")
            keyboardController?.hide()
            focusManager.clearFocus()
          },
          modifier = Modifier.testTag("search_clear_button")
        ) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(24.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    ),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(
      onSearch = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSearch()
      }
    ),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = horizontalPadding)
      .testTag("app_search_bar")
  )
}

@Composable
fun CategoryFilterRow(
  categories: List<String>,
  selectedCategory: String,
  onCategorySelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val screenWidth = LocalConfiguration.current.screenWidthDp
  val horizontalPadding = when {
    screenWidth >= 840 -> 24.dp
    screenWidth >= 600 -> 20.dp
    else -> 16.dp
  }

  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    contentPadding = PaddingValues(horizontal = horizontalPadding),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(categories) { category ->
      val isSelected = category == selectedCategory
      FilterChip(
        selected = isSelected,
        onClick = { onCategorySelected(category) },
        label = {
          Text(
            text = category,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          borderWidth = 1.dp
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("filter_chip_$category")
      )
    }
  }
}

@Composable
fun StatusBadge(
  status: MatchStatus,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor) = when (status) {
    MatchStatus.OPEN -> com.example.ui.theme.BadgeSuccessBg to com.example.ui.theme.BadgeSuccessText
    MatchStatus.FAST_FILLING -> com.example.ui.theme.BadgeHotBg to com.example.ui.theme.BadgeHotText
    MatchStatus.LIVE -> com.example.ui.theme.BadgeHotBg to com.example.ui.theme.BadgeHotText
    MatchStatus.UPCOMING -> com.example.ui.theme.ForestGreenContainer to com.example.ui.theme.ForestGreenOnContainer
    MatchStatus.COMPLETED -> Color(0xFFE5E7EB) to Color(0xFF4B5563)
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    modifier = modifier
  ) {
    Text(
      text = status.label.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
      ),
      color = textColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun MatchNumberBadge(
  number: Int,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    ),
    modifier = modifier
  ) {
    Text(
      text = "MATCH #$number",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
      ),
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}


@Composable
fun BottomNavBar(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit,
  isAdmin: Boolean = false,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 2.dp,
    modifier = modifier.testTag("bottom_nav_bar")
  ) {
    NavigationBarItem(
      selected = selectedTab == 0,
      onClick = { onTabSelected(0) },
      icon = {
        Icon(
          imageVector = if (selectedTab == 0) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports,
          contentDescription = "Matches"
        )
      },
      label = {
        Text(
          text = "Matches",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
          )
        )
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onBackground,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
      ),
      modifier = Modifier.testTag("nav_tab_matches")
    )

    NavigationBarItem(
      selected = selectedTab == 1,
      onClick = { onTabSelected(1) },
      icon = {
        Icon(
          imageVector = if (selectedTab == 1) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
          contentDescription = "Tournaments"
        )
      },
      label = {
        Text(
          text = "Tournaments",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
          )
        )
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onBackground,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
      ),
      modifier = Modifier.testTag("nav_tab_custom")
    )

    NavigationBarItem(
      selected = selectedTab == 2,
      onClick = { onTabSelected(2) },
      icon = {
        Icon(
          imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
          contentDescription = "Player Profiles"
        )
      },
      label = {
        Text(
          text = "Profile",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
          )
        )
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onBackground,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
      ),
      modifier = Modifier.testTag("nav_tab_user")
    )
  }
}

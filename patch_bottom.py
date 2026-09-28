import re

with open("app/src/main/java/com/example/ui/components/CommonComponents.kt", "r") as f:
    content = f.read()

target = """fun BottomNavBar(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {"""

new_decl = """import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.AdminPanelSettings

@Composable
fun BottomNavBar(
  selectedTab: Int,
  onTabSelected: (Int) -> Unit,
  isAdmin: Boolean = false,
  modifier: Modifier = Modifier
) {"""

content = content.replace("@Composable\n" + target, new_decl)

user_tab = """    NavigationBarItem(
      selected = selectedTab == 2,
      onClick = { onTabSelected(2) },
      icon = {
        Icon(
          imageVector = if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.Person,
          contentDescription = "User"
        )
      },
      label = {
        Text(
          text = "User",
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
    )"""

admin_tab = """    if (isAdmin) {
      NavigationBarItem(
        selected = selectedTab == 2,
        onClick = { onTabSelected(2) },
        icon = {
          Icon(
            imageVector = if (selectedTab == 2) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
            contentDescription = "Admin"
          )
        },
        label = {
          Text(
            text = "Admin",
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
        modifier = Modifier.testTag("nav_tab_admin")
      )
    }

    NavigationBarItem(
      selected = selectedTab == (if (isAdmin) 3 else 2),
      onClick = { onTabSelected(if (isAdmin) 3 else 2) },
      icon = {
        Icon(
          imageVector = if (selectedTab == (if (isAdmin) 3 else 2)) Icons.Filled.Person else Icons.Outlined.Person,
          contentDescription = "User"
        )
      },
      label = {
        Text(
          text = "User",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (selectedTab == (if (isAdmin) 3 else 2)) FontWeight.Bold else FontWeight.Normal
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
    )"""

content = content.replace(user_tab, admin_tab)

with open("app/src/main/java/com/example/ui/components/CommonComponents.kt", "w") as f:
    f.write(content)

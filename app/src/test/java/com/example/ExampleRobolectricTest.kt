package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.User
import com.example.ui.screens.EditMemberDesignationDialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("जय हिंद मंडळ अर्जुनवाड", appName)
  }

  @Test
  fun `test EditMemberDesignationDialog renders without crash`() {
    val testUser = User(
      id = "user_123",
      fullName = "संजय पाटील",
      mobileNumber = "9876543210",
      role = "MEMBER",
      designation = "सभासद"
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        EditMemberDesignationDialog(
          member = testUser,
          onDismiss = {},
          onSave = { _, _ -> }
        )
      }
    }
  }
}

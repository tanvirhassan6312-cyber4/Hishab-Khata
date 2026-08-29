package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BengaliFormatters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("দোকান ও QR হিসাব", appName)
  }

  @Test
  fun `bengali formatter test`() {
    assertEquals("১০০০", BengaliFormatters.toBanglaNumber(1000))
    assertEquals("৳ ১০০০", BengaliFormatters.toBanglaCurrency(1000.0))
  }
}

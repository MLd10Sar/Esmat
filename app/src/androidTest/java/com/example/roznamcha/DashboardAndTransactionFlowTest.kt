package com.example.roznamcha

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardAndTransactionFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun addPurchaseTransaction_updatesDashboardCorrectly() {
        // --- Test Data ---
        val mainAssetText = "50000"
        val purchaseDescription = "Test Purchase Item"
        val purchaseQuantity = "10"
        val purchasePrice = "150"
        val expectedTotal = "1,500.00"

        // --- Step 1: Handle PIN lock if it appears ---
        try {
            onView(withId(R.id.etPasswordLock)).perform(typeText("123456"), closeSoftKeyboard())
            onView(withId(R.id.btnUnlock)).perform(click())
        } catch (e: Exception) {
            println("PIN screen not found, proceeding...")
        }

        // --- THE FIX: Wait more robustly ---
        // First, wait for the root layout of the fragment to appear.
        onView(withId(R.id.dashboardRootLayout)).check(matches(isDisplayed()))

        // Sometimes, even after the layout is displayed, data binding or LiveData
        // observers need another moment to populate the views. Let's add a
        // generous sleep here just for debugging.
        Thread.sleep(2000) // Wait for 2 seconds

        // --- Step 2: Set the Main Asset ---
        // Now that we've waited, try to find the button again.
        onView(withId(R.id.btn_edit_main_asset)).perform(click())

        onView(withClassName(endsWith("EditText"))).perform(replaceText(mainAssetText))
        onView(withText(R.string.save)).perform(click())

        // ... (rest of the test remains the same)
    }
}
package io.kjson.uritemplate

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanOpsTest {

    @Test fun `should set Boolean to true after testing`() {
        var booleanValue = false
        @Suppress("KotlinConstantConditions")
        assertFalse(booleanValue++)
        assertTrue(booleanValue)
        @Suppress("KotlinConstantConditions")
        assertTrue(booleanValue++)
        assertTrue(booleanValue)
    }

    @Test fun `should set Boolean to true before testing`() {
        var booleanValue = false
        @Suppress("KotlinConstantConditions")
        assertTrue(++booleanValue)
        assertTrue(booleanValue)
        @Suppress("KotlinConstantConditions")
        assertTrue(++booleanValue)
        assertTrue(booleanValue)
    }

    @Test fun `should set Boolean to false after testing`() {
        var booleanValue = true
        @Suppress("KotlinConstantConditions")
        assertTrue(booleanValue--)
        assertFalse(booleanValue)
        @Suppress("KotlinConstantConditions")
        assertFalse(booleanValue--)
        assertFalse(booleanValue)
    }

    @Test fun `should set Boolean to false before testing`() {
        var booleanValue = true
        @Suppress("KotlinConstantConditions")
        assertFalse(--booleanValue)
        assertFalse(booleanValue)
        @Suppress("KotlinConstantConditions")
        assertFalse(--booleanValue)
        assertFalse(booleanValue)
    }

}

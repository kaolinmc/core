package com.kaolinmc.client.test

import com.kaolinmc.client.VERSION_REGEX
import kotlin.test.Test
import kotlin.test.assertTrue

class RegexTest {
    @Test
    fun `Test version regex`() {
        val test1 = "extframework-1.20.1"
        assertTrue {
            VERSION_REGEX.matches(test1)
        }
        assertTrue {
            VERSION_REGEX.matchEntire(test1)!!.groupValues[1] == "1.20.1"
        }

        val test2 = "extframework-21wa4a"
        assertTrue {
            VERSION_REGEX.matches(test2)
        }
        assertTrue {
            VERSION_REGEX.matchEntire(test2)!!.groupValues[1] == "21wa4a"
        }
    }
}


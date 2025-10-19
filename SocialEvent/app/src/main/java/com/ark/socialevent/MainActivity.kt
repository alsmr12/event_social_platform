package com.ark.socialevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ark.socialevent.navigation.NavGraph
import com.ark.socialevent.ui.theme.SocialEventTheme
import com.ark.socialevent.ui.theme.ThemedCircleBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SocialEventTheme {
                ThemedCircleBackground {
                    NavGraph()
                }
            }
        }
    }
}

package com.esfersoft.imageuploader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.esfersoft.imageuploader.di.ServiceLocator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.esfersoft.imageuploader.ui.theme.ImageUploaderTheme
import androidx.activity.viewModels
import com.esfersoft.imageuploader.presentation.GalleryViewModel
import com.esfersoft.imageuploader.presentation.GalleryViewModelFactory
import com.esfersoft.imageuploader.presentation.ui.GalleryScreen
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            ImageUploaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val vm: GalleryViewModel by viewModels(factoryProducer = { GalleryViewModelFactory() })
                    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
                        vm.setPickedUris(uris)
                    }
                    Box(modifier = Modifier.padding(innerPadding)) {
                        GalleryScreen(
                            viewModel = vm,
                            onChoosePhotos = { picker.launch( PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onRefreshRequested = { vm.viewModelScope.launch { vm.refreshImages() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ImageUploaderTheme {
        Greeting("Android")
    }
}
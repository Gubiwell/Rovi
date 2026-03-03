package com.example.rovi2.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rovi2.ui.theme.Rovi2Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest

class MapsActivity : ComponentActivity() {

    companion object {
        const val REQUEST_SELECT_LOCATION = 1001
    }

    private var requestCode: Int = 0
    private var etapaLocalId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestCode = intent.getIntExtra("REQUEST_CODE", 0)
        etapaLocalId = intent.getStringExtra("ETAPA_LOCAL_ID")

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "SEGREDO!")
        }

        setContent {
            Rovi2Theme {
                MapWithPlacesScreen(
                    isSelectingLocation = (requestCode == REQUEST_SELECT_LOCATION),
                    onBackClick = { finish() },
                    onPlaceSelected = { place, name ->
                        val resultIntent = Intent()
                        resultIntent.putExtra("SELECTED_LATITUDE", place.latLng?.latitude)
                        resultIntent.putExtra("SELECTED_LONGITUDE", place.latLng?.longitude)
                        resultIntent.putExtra("SELECTED_NAME", name)
                        resultIntent.putExtra("ETAPA_LOCAL_ID", etapaLocalId)
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapWithPlacesScreen(
    isSelectingLocation: Boolean,
    onBackClick: () -> Unit,
    onPlaceSelected: (Place, String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val placesClient = remember { Places.createClient(context) }

    //Estados do Mapa e Localização
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }

    //Estados de Busca e Interface
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var searchRadiusKm by remember { mutableStateOf(5f) }
    var placesList by remember { mutableStateOf<List<Place>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    //Função de Busca Usando SearchByText com CircularBounds
    fun buscarLugares(location: Location, query: String, radiusKm: Float) {
        isLoading = true

        val textQuery = if (query.isNotBlank()) query else if (selectedCategory.isNotBlank()) selectedCategory else "Point of Interest"

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.RATING,
            Place.Field.TYPES
        )

        val radiusMeters = (radiusKm * 1000).toDouble()
        val circleBounds = CircularBounds.newInstance(
            LatLng(location.latitude, location.longitude),
            radiusMeters
        )

        //requisição
        val searchRequest = SearchByTextRequest.builder(textQuery, placeFields)
            .setLocationBias(circleBounds)
            .setMaxResultCount(12) //Limita a 12 !!!!
            //.setOpenNow(true) // Trazer só lugares abertos ( adição futura )
            .build()

        placesClient.searchByText(searchRequest)
            .addOnSuccessListener { response ->
                val places = response.places
                placesList = places

                map?.clear()

                val userLatLng = LatLng(location.latitude, location.longitude)
                map?.addMarker(
                    MarkerOptions()
                        .position(userLatLng)
                        .title("Você")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )

                //marcadores para os resultados
                val boundsBuilder = LatLngBounds.Builder().include(userLatLng)
                places.forEach { place ->
                    place.latLng?.let { latLng ->
                        map?.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                                .snippet(place.address)
                        )
                        boundsBuilder.include(latLng)
                    }
                }

                //câmera para mostrar todos os pontos
                if (places.isNotEmpty()) {
                    try {
                        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                    } catch (e: Exception) {
                        // o mapa não estiver pronto
                    }
                } else {
                    Toast.makeText(context, "Nenhum local encontrado neste raio.", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Erro na busca: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    //obter localização e iniciar busca
    fun refreshLocationAndSearch() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        userLocation = loc
                        //não busca automaticamente
                        if (!isSelectingLocation || searchQuery.isNotBlank()) {
                            buscarLugares(loc, searchQuery, searchRadiusKm)
                        } else {
                            //move a câmera para o usuário
                            val userLatLng = LatLng(loc.latitude, loc.longitude)
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                            map?.addMarker(MarkerOptions().position(userLatLng).title("Você"))
                        }
                    }
                }
        } catch (e: SecurityException) {
            //Permissão não garantida
        }
    }

    //Launcher de Permissão
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) refreshLocationAndSearch()
    }

    //Inicialização
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            refreshLocationAndSearch()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    //Dispara busca quando filtros mudam
    LaunchedEffect(selectedCategory, searchRadiusKm) {
        userLocation?.let { buscarLugares(it, searchQuery, searchRadiusKm) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 8.dp)
            ) {
                TopAppBar(
                    title = { Text(if (isSelectingLocation) "Definir Local" else "Explorar") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtros",
                                tint = if(showFilters) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar (ex: Pizza, Hotel...)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                userLocation?.let { buscarLugares(it, searchQuery, searchRadiusKm) }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                        },
                        singleLine = true
                    )
                    if (showFilters || placesList.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        //Slider de Raio
                        Text(
                            text = "Raio de busca: ${searchRadiusKm.toInt()} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = searchRadiusKm,
                            onValueChange = { searchRadiusKm = it },
                            valueRange = 1f..50f, //de 1km a 50km por enquanto !!!!!!
                            steps = 49,
                            modifier = Modifier.height(20.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // CHIPS Categoria
                        val categorias = listOf("Restaurante", "Café", "Hotel", "Parque", "Museu", "Shopping")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categorias) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == cat) "" else cat

                                        if (selectedCategory.isNotBlank()) searchQuery = ""
                                    },
                                    label = { Text(cat) },
                                    leadingIcon = if (selectedCategory == cat) {
                                        { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // O MAPA
            AndroidView(
                factory = { ctx ->
                    val mapView = MapView(ctx)
                    mapView.onCreate(null)
                    mapView.getMapAsync { googleMap ->
                        map = googleMap
                        googleMap.uiSettings.isZoomControlsEnabled = true
                        googleMap.uiSettings.isMyLocationButtonEnabled = true

                        //Listener de clique no mapa
                        if (isSelectingLocation) {
                            googleMap.setOnMapClickListener { latLng ->
                                map?.clear()
                                currentMarker = googleMap.addMarker(
                                    MarkerOptions().position(latLng).title("Local Selecionado")
                                )

                                val pseudoPlace = Place.builder()
                                    .setName("Marcador no Mapa")
                                    .setLatLng(latLng)
                                    .build()
                                placesList = listOf(pseudoPlace)
                            }
                        }
                    }
                    mapView
                },
                modifier = Modifier.fillMaxSize()
            )


            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }

            // LISTA DE RESULTADOS
            if (placesList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(placesList) { place ->
                        PlaceResultCard(
                            place = place,
                            onClick = {
                                if (isSelectingLocation) {
                                    //Retorna o lugar selecionado
                                    onPlaceSelected(place, place.name ?: "Local Desconhecido")
                                } else {
                                    place.latLng?.let { latLng ->
                                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                                        currentMarker?.remove()
                                        currentMarker = map?.addMarker(
                                            MarkerOptions().position(latLng).title(place.name)
                                        )
                                        currentMarker?.showInfoWindow()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceResultCard(place: Place, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(110.dp) // Altura fixa
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                //Nome
                Text(
                    text = place.name ?: "Local sem nome",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                //Endereço curto
                Text(
                    text = place.address ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                //Nota
                place.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rating",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
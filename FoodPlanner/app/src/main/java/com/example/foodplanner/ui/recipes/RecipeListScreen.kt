package com.example.foodplanner.ui.recipes

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodplanner.data.recipes.RecipeDTO
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(authViewModel: AuthViewModel = viewModel()) {
    val userState by authViewModel.user.collectAsState()
    val context = LocalContext.current

    val screenPadding = 16.dp
    val itemSpacing = 12.dp

    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = userState!!  // aquí ya sabemos que no es null

    val factory = PantryViewModel.Factory(context.applicationContext as Application, user.uid)
    val vm: PantryViewModel = viewModel(factory = factory)

    val searchedRecipes by vm.searchedRecipes.collectAsState()
    val savedRecipes by vm.savedRecipes.collectAsState()
    val savedRecipeIds = remember(savedRecipes) { savedRecipes.map { it.id }.toSet() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Search", "Saved")

    var searchText by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        /*topBar = {
            TopAppBar(title = { Text("Recipes") })
        }*/
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = screenPadding, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    vm.searchRecipes(it.text)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search recipes") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )

            Spacer(Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val list = if (selectedTab == 0) searchedRecipes else savedRecipes

            RecipeListView(
                recipes = list,
                savedRecipeIds = savedRecipeIds,
                onSaveClick = { vm.saveRecipe(it) },
                onDeleteClick = { vm.deleteSavedRecipe(it.id) },
                onAddToCartClick = { vm.addRecipeMissingToCart(it) },
                contentPadding = PaddingValues(bottom = screenPadding),
                itemSpacing = itemSpacing
            )
        }
    }
}

@Composable
fun RecipeListView(
    recipes: List<RecipeDTO>,
    savedRecipeIds: Set<String>,
    onSaveClick: (RecipeDTO) -> Unit,
    onDeleteClick: (RecipeDTO) -> Unit,
    onAddToCartClick: (RecipeDTO) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemSpacing: Dp = 12.dp
) {
    if (recipes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recipes here yet.\nTry searching or save some recipes!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeCard(
                recipe = recipe,
                isSaved = savedRecipeIds.contains(recipe.id),
                onSaveClick = onSaveClick,
                onDeleteClick = onDeleteClick,
                onAddToCartClick = onAddToCartClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCard(
    recipe: RecipeDTO,
    isSaved: Boolean,
    onSaveClick: (RecipeDTO) -> Unit,
    onDeleteClick: (RecipeDTO) -> Unit,
    onAddToCartClick: (RecipeDTO) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (expanded) "Tap to collapse" else "Tap to view details",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { if (isSaved) onDeleteClick(recipe) else onSaveClick(recipe) }
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "Unsave Recipe" else "Save Recipe",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (expanded) {
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))

                    Text("Ingredients", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))

                    val ingredientsText = recipe.ingredients.joinToString(separator = "\n") {
                        "- ${it.name}: ${it.quantity} ${it.unit}"
                    }
                    Text(
                        text = ingredientsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Instructions", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = recipe.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { onAddToCartClick(recipe) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add missing to cart")
                    }
                }
            }
        }
    }
}
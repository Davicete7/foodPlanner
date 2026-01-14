package com.example.foodplanner.ui.recipes

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
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

    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        userState?.let { user ->
            val factory = PantryViewModel.Factory(context.applicationContext as Application, user.uid)
            val vm: PantryViewModel = viewModel(factory = factory)

            val searchedRecipes by vm.searchedRecipes.collectAsState()
            val savedRecipes by vm.savedRecipes.collectAsState()
            val savedRecipeIds = remember(savedRecipes) { savedRecipes.map { it.id }.toSet() }

            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("Search Recipes", "My Saved Recipes")

            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                var searchText by remember { mutableStateOf(TextFieldValue("")) }
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        vm.searchRecipes(it.text)
                    },
                    label = { Text("Search for recipes...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> RecipeListView(
                        recipes = searchedRecipes,
                        savedRecipeIds = savedRecipeIds,
                        onSaveClick = { vm.saveRecipe(it) },
                        onDeleteClick = { vm.deleteSavedRecipe(it.id) },
                        onAddToCartClick = { vm.addRecipeMissingToCart(it) }
                    )
                    1 -> RecipeListView(
                        recipes = savedRecipes,
                        savedRecipeIds = savedRecipeIds,
                        onSaveClick = { vm.saveRecipe(it) },
                        onDeleteClick = { vm.deleteSavedRecipe(it.id) },
                        onAddToCartClick = { vm.addRecipeMissingToCart(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeListView(
    recipes: List<RecipeDTO>,
    savedRecipeIds: Set<String>,
    onSaveClick: (RecipeDTO) -> Unit,
    onDeleteClick: (RecipeDTO) -> Unit,
    onAddToCartClick: (RecipeDTO) -> Unit
) {
    if (recipes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No recipes found. Try another search or save some recipes!")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
        onClick = { expanded = !expanded }
    ) {
        Column {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                // Save/Unsave Button
                IconButton(onClick = { if (isSaved) onDeleteClick(recipe) else onSaveClick(recipe) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isSaved) "Unsave Recipe" else "Save Recipe",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (expanded) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Ingredients", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    
                    val ingredientsText = recipe.ingredients.joinToString(separator = "\n") {
                        "- ${it.name}: ${it.quantity} ${it.unit}"
                    }
                    Text(text = ingredientsText, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(8.dp))
                    Text("Instructions", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(text = recipe.instructions, style = MaterialTheme.typography.bodyMedium)

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

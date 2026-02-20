package com.example.snapandcook.ui.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.snapandcook.R
import com.example.snapandcook.data.local.SavedRecipe
import com.example.snapandcook.databinding.ItemSavedRecipeBinding
import com.example.snapandcook.util.formatMinutes

/**
 * Adapter for the Saved Recipes screen list.
 *
 * Each item displays the recipe thumbnail, title, time, servings, and a delete button.
 */
class SavedRecipeAdapter(
    private val onItemClick: (SavedRecipe) -> Unit,
    private val onDelete: (SavedRecipe) -> Unit
) : ListAdapter<SavedRecipe, SavedRecipeAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemSavedRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: SavedRecipe) {
            binding.tvTitle.text = recipe.title
            binding.tvTime.text = "⏱ ${recipe.readyInMinutes.formatMinutes()}"
            binding.tvServings.text = "🍽 ${recipe.servings}"

            Glide.with(binding.ivThumb)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_placeholder_food)
                .error(R.drawable.ic_placeholder_food)
                .transform(RoundedCorners(20))
                .into(binding.ivThumb)

            binding.root.setOnClickListener { onItemClick(recipe) }
            binding.btnDelete.setOnClickListener { onDelete(recipe) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSavedRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SavedRecipe>() {
            override fun areItemsTheSame(a: SavedRecipe, b: SavedRecipe) = a.recipeId == b.recipeId
            override fun areContentsTheSame(a: SavedRecipe, b: SavedRecipe) = a == b
        }
    }
}

package com.example.snapandcook.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.snapandcook.R
import com.example.snapandcook.data.local.SavedRecipe
import com.example.snapandcook.databinding.ItemRecentRecipeBinding
import com.example.snapandcook.util.formatMinutes

/**
 * Adapter for the "Recently Saved" strip on the home screen.
 */
class RecentRecipeAdapter(
    private val onClick: (SavedRecipe) -> Unit
) : ListAdapter<SavedRecipe, RecentRecipeAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRecentRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: SavedRecipe) {
            binding.tvTitle.text = recipe.title
            binding.tvMeta.text = buildString {
                append(recipe.readyInMinutes.formatMinutes())
                recipe.calories?.let { append(" · ${it} kcal") }
            }

            Glide.with(binding.ivThumb)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_placeholder_food)
                .error(R.drawable.ic_placeholder_food)
                .transform(RoundedCorners(24))
                .into(binding.ivThumb)

            binding.root.setOnClickListener { onClick(recipe) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecentRecipeBinding.inflate(
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

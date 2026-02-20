package com.example.snapandcook.ui.verification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.snapandcook.data.model.Ingredient
import com.example.snapandcook.databinding.ItemIngredientBinding
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import kotlin.math.roundToInt

/**
 * RecyclerView adapter for the ingredient list on the Verification screen.
 *
 * Each item shows the ingredient name, an optional confidence percentage badge
 * (for ML-detected items), an "Added" badge (for manually typed items), and a
 * delete button.
 */
class IngredientAdapter(
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<IngredientAdapter.VH>() {

    private val items = mutableListOf<Ingredient>()

    fun submitList(list: List<Ingredient>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: Ingredient, position: Int) {
            binding.tvName.text = ingredient.name

            // Confidence badge (ML-detected only)
            val conf = ingredient.confidence
            if (conf != null && !ingredient.isManual) {
                val pct = (conf * 100).roundToInt()
                binding.tvConfidence.text = "$pct%"
                binding.tvConfidence.show()
            } else {
                binding.tvConfidence.gone()
            }

            // "Added" badge for manual entries
            if (ingredient.isManual) binding.tvManualBadge.show()
            else binding.tvManualBadge.gone()

            binding.btnDelete.setOnClickListener { onDelete(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemIngredientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position], position)

    override fun getItemCount() = items.size
}

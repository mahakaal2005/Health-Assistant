package com.example.health_assistant.features.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.home.models.WellnessTip

/**
 * Adapter for displaying wellness tips in a horizontal carousel
 */
class WellnessTipsAdapter(
    private val wellnessTips: List<WellnessTip>,
    private val onTipClicked: (WellnessTip) -> Unit
) : RecyclerView.Adapter<WellnessTipsAdapter.WellnessTipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WellnessTipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wellness_tip, parent, false)
        return WellnessTipViewHolder(view)
    }

    override fun onBindViewHolder(holder: WellnessTipViewHolder, position: Int) {
        holder.bind(wellnessTips[position])
    }

    override fun getItemCount() = wellnessTips.size

    inner class WellnessTipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tipImage: ImageView = itemView.findViewById(R.id.tip_image)
        private val tipTitle: TextView = itemView.findViewById(R.id.tip_title)
        private val tipDescription: TextView = itemView.findViewById(R.id.tip_description)

        fun bind(tip: WellnessTip) {
            tipTitle.text = tip.title
            tipDescription.text = tip.shortDescription

            // If using a real image loading library like Glide or Coil,
            // you would load the image here
            tipImage.setImageResource(tip.imageResId ?: R.drawable.card_gradient_background)

            itemView.setOnClickListener {
                onTipClicked(tip)
            }
        }
    }
}
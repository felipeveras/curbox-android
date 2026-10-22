package neth.iecal.curbox.ui.fragments.main.reducers.blockertools.uiHider

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import neth.iecal.curbox.data.models.UiHiderScript
import neth.iecal.curbox.R
import neth.iecal.curbox.databinding.ItemUiHiderScriptBinding

class UiHiderScriptAdapter(
    private val onClick: (UiHiderScript) -> Unit,
    private val onToggle: (String, Boolean) -> Unit
) : ListAdapter<UiHiderScript, UiHiderScriptAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(val binding: ItemUiHiderScriptBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUiHiderScriptBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val script = getItem(position)
        holder.binding.run {
            tvScriptTitle.text = script.localizedLabel(holder.itemView.context).ifBlank {
                script.packageName.substringAfterLast('.')
            }
            tvScriptSubtitle.text = script.packageName

            switchScriptEnabled.setOnCheckedChangeListener(null)
            switchScriptEnabled.isChecked = script.isEnabled
            switchScriptEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(script.id, checked)
            }

            root.setOnClickListener { onClick(script) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<UiHiderScript>() {
        override fun areItemsTheSame(oldItem: UiHiderScript, newItem: UiHiderScript) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UiHiderScript, newItem: UiHiderScript) =
            oldItem == newItem
    }
}

internal fun UiHiderScript.localizedLabel(context: Context): String {
    val resource = when (id) {
        "ig_feed" -> R.string.uihider_preset_ig_feed
        "ig_explore" -> R.string.uihider_preset_ig_explore
        "yt_video_thingies" -> R.string.uihider_preset_yt_video
        "yt_video_everything_except_results" -> R.string.uihider_preset_yt_results
        "x_feed_but_allow_following" -> R.string.uihider_preset_x_feed
        "snapchat_stories" -> R.string.uihider_preset_snapchat
        "pinterest_home_feed" -> R.string.uihider_preset_pinterest
        else -> null
    }
    return resource?.let(context::getString) ?: label
}

package neth.iecal.curbox.ui.activity

import android.content.Context
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import neth.iecal.curbox.R
import neth.iecal.curbox.databinding.DialogRedirectPickerBinding
import neth.iecal.curbox.databinding.ItemRedirectChoiceBinding

object RedirectAppPicker {
    fun show(context: Context, apps: List<ResolveInfo>, selected: String?, onSelected: (String?) -> Unit) {
        val pm = context.packageManager
        val choices = listOf(Choice(context.getString(R.string.redirect_off), null, null)) + apps.map {
            Choice(it.loadLabel(pm).toString().takeIf(String::isNotBlank) ?: context.getString(R.string.redirect_selected_app), it.activityInfo.packageName, it)
        }
        val binding = DialogRedirectPickerBinding.inflate(LayoutInflater.from(context))
        var visible = choices
        val adapter = object : BaseAdapter() {
            override fun getCount() = visible.size
            override fun getItem(position: Int): Any = visible[position].name
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = if (convertView == null) ItemRedirectChoiceBinding.inflate(LayoutInflater.from(context), parent, false)
                    else ItemRedirectChoiceBinding.bind(convertView)
                val choice = visible[position]
                row.redirectChoiceName.text = choice.name
                row.redirectChoicePackage.text = choice.pkg.orEmpty()
                row.redirectChoicePackage.visibility = if (choice.pkg == null) View.GONE else View.VISIBLE
                row.redirectChoiceIcon.setImageDrawable(choice.info?.loadIcon(pm))
                row.redirectChoiceChecked.isChecked = choice.pkg == selected
                return row.root
            }
        }
        binding.redirectApps.adapter = adapter
        binding.redirectApps.emptyView = binding.redirectEmpty
        val dialog = MaterialAlertDialogBuilder(context).setTitle(R.string.redirect_picker_title)
            .setView(binding.root).setNegativeButton(R.string.cancel, null).create()
        binding.redirectApps.setOnItemClickListener { _, _, position, _ ->
            onSelected(visible[position].pkg)
            dialog.dismiss()
        }
        binding.redirectSearch.doAfterTextChanged {
            val query = it.toString().trim()
            visible = choices.filter { choice -> query.isEmpty() || choice.name.contains(query, true) || choice.pkg?.contains(query, true) == true }
            adapter.notifyDataSetChanged()
        }
        dialog.show()
    }

    private data class Choice(val name: String, val pkg: String?, val info: ResolveInfo?)
}

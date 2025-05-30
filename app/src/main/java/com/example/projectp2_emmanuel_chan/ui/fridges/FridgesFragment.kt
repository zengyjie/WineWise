package com.example.projectp2_emmanuel_chan.ui.fridges

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuel_chan.FridgeActivity
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.origSelectedFridge
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedFridge
import com.example.projectp2_emmanuel_chan.R
import com.example.projectp2_emmanuel_chan.databinding.FragmentFridgesBinding
import com.example.projectp2_emmanuel_chan.ui.custom.CustomSpinner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.Serializable

class FridgesFragment : Fragment() {

    private var _binding: FragmentFridgesBinding? = null
    private val binding get() = _binding!!

    companion object {
        lateinit var fridgeRecyclerView: RecyclerView
        var fridgeToOpen: Fridge? = null
        var itemLayer: Int = 3

        fun openFridge(fridge: Fridge, context: Context) {
            selectedFridge = fridge
            val intent = Intent(context, FridgeActivity::class.java)
            intent.putExtra("itemLayer", itemLayer)
            context.startActivity(intent)
        }

        fun saveFridges(context: Context) {
            try {
                val file = File(context.filesDir, "fridges.json")
                val json = Gson().toJson(fridges)
                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadFridges(context: Context) {
            try {
                val file = File(context.filesDir, "fridges.json")
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : TypeToken<MutableList<Fridge>>() {}.type
                    fridges = Gson().fromJson(json, type) ?: mutableListOf()
                } else {
                    fridges = mutableListOf()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fridges = mutableListOf()
            }
        }

        fun resizeWinesArray(fridge: Fridge, newDepth: Int, newSections: Int, newRows: Int, newColumns: Int) {
            val oldWines = fridge.wines

            val newWines = MutableList(newDepth) { layer -> MutableList(newSections) { section -> MutableList(newRows) { row -> MutableList(newColumns) { column ->
                if (layer < oldWines.size &&
                    section < oldWines[layer].size &&
                    row < oldWines[layer][section].size &&
                    column < oldWines[layer][section][row].size)
                { oldWines[layer][section][row][column] } else { Wine() }
            } } } }

            fridge.wines = newWines
        }
    }

    private lateinit var fridgeAdapter: FridgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFridgesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        loadFridges(requireContext())

        fridgeRecyclerView = binding.fridgeRecyclerView
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { 2 } else { 3 }
        fridgeRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        fridgeAdapter = FridgeAdapter(fridges.filter { it.name != "drunk" && it.name != "database" }, { fridge ->
            origSelectedFridge = fridge
            openFridge(fridge, requireContext()) },
            { fridge -> showEditFridgePopup(fridge) })
        fridgeRecyclerView.adapter = fridgeAdapter

        val floatingActionButton: FloatingActionButton = binding.root.findViewById(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener { showAddFridgePopup() }

        if (fridgeToOpen != null) {
            val position = fridges.indexOfFirst { it.name == fridgeToOpen!!.name }
            println(position)
            if (position != -1) {
                fridgeRecyclerView.post {
                    fridgeRecyclerView.findViewHolderForAdapterPosition(position - 1)?.itemView?.performClick()
                }
            }
            fridgeToOpen = null
        }

        addFridge(Fridge(
            name = "drunk",
            icon = R.drawable.ic_add,
            sections = 1,
            columns = 1,
            rps = 1,
            depth = 1
        ))
        if (fridges.size == 1) { binding.noFridgesTextView.visibility = View.VISIBLE }
        else { binding.noFridgesTextView.visibility = View.GONE }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //functions
    private fun showAddFridgePopup() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_fridge, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialogView.findViewById<Button>(R.id.deleteButton).visibility = View.GONE

        dialogView.findViewById<CustomSpinner>(R.id.sectionsCustomSpinner)?.updateCount(4)
        dialogView.findViewById<CustomSpinner>(R.id.rpsCustomSpinner)?.updateCount(1)
        dialogView.findViewById<CustomSpinner>(R.id.columnsCustomSpinner)?.updateCount(5)
        dialogView.findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            val fridge = readFridgeData(dialogView)
            if (!(fridge.name == "InvalidName" || fridge.name == "drunk")) { addFridge(fridge) } else { return@setOnClickListener }
            dialog.dismiss()
        }

        dialogView.setOnTouchListener { _, _ ->
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
            false
        }

        dialog.show()
    }

    private fun showEditFridgePopup(fridge: Fridge) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_fridge, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<TextView>(R.id.titleTextView)?.text = "Edit Fridge"
        dialogView.findViewById<TextView>(R.id.fridgeNameEditText).text = fridge.name
        dialogView.findViewById<CustomSpinner>(R.id.sectionsCustomSpinner).count(fridge.sections)
        dialogView.findViewById<CustomSpinner>(R.id.columnsCustomSpinner).count(fridge.columns)
        dialogView.findViewById<CustomSpinner>(R.id.rpsCustomSpinner).count(fridge.rps)
        dialogView.findViewById<ToggleButton>(R.id.depthToggleButton).isChecked = fridge.depth != 1

        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
            val confirmDialogBuilder = AlertDialog.Builder(requireContext())
                .setView(confirmDeleteView)
            val deleteDialog = confirmDialogBuilder.create()

            confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Delete ${fridge.name}? All wines in this fridge will be lost."

            confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener {
                removeFridge(fridge)
                deleteDialog.dismiss()
                dialog.dismiss()
                toast("Deleted ${fridge.name}")
            }

            confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
                deleteDialog.dismiss()
            }

            deleteDialog.show()
        }

        dialogView.findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            val newFridge = readFridgeData(dialogView, edit = true)
            if (newFridge.name == "InvalidName") { newFridge.name = fridge.name }
            editFridge(fridge, newFridge)
            toast("Edit successful")
            dialog.dismiss()
        }

        dialogView.setOnTouchListener { _, _ ->
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
            false
        }

        dialog.show()
    }

    private fun toast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    //Fridge class
    data class Fridge (
        var name: String = "New fridge",
        var icon: Int = R.drawable.default_fridge,
        var sections: Int = 1,
        var columns: Int = 1,
        var rps: Int = 1,
        var depth: Int = 1,
        var capacity: Int = sections * rps * columns * depth,
        var wines: MutableList<MutableList<MutableList<MutableList<Wine>>>> = mutableListOf(),
        var counter: Int = 0
    ) : Serializable

    private fun initWineArray(depth: Int, sections: Int, rps: Int, columns: Int): MutableList<MutableList<MutableList<MutableList<Wine>>>> {
        return MutableList(depth) { MutableList(sections) { MutableList(rps) { MutableList(columns) { Wine() } } } }
    }

    private fun addFridge(fridge: Fridge) {
        if (fridges.contains(fridge)) { return }
        if (fridges.any { it.name.equals(fridge.name, ignoreCase = true) }) { return }
        fridges.add(fridge)
        fridge.wines = initWineArray(fridge.depth, fridge.sections, fridge.rps, fridge.columns)
        binding.noFridgesTextView.visibility = View.GONE
        fridgeAdapter.updateList(fridges.filter { it.name != "drunk" })
    }

    private fun removeFridge(fridge: Fridge) {
        fridges.remove(fridge)
        fridgeAdapter.updateList(fridges.filter { it.name != "drunk" })
        if (fridges.size == 1) { binding.noFridgesTextView.visibility = View.VISIBLE }
    }

    private fun editFridge(fridge: Fridge, newFridge: Fridge) {
        fridge.name = newFridge.name
        fridge.icon = newFridge.icon
        fridge.sections = newFridge.sections
        fridge.columns = newFridge.columns
        fridge.rps = newFridge.rps
        fridge.depth = newFridge.depth
        fridge.capacity = newFridge.capacity
        resizeWinesArray(fridge, newFridge.depth, newFridge.sections, newFridge.rps, newFridge.columns)
        fridgeAdapter.notifyDataSetChanged()
    }

    private fun readFridgeData(dialogView: View, edit: Boolean = false): Fridge {
        val nameEditText = dialogView.findViewById<EditText>(R.id.fridgeNameEditText)
        var name = nameEditText?.text.toString().trim()

        if (name.isEmpty()) {
            toast("Please choose a name")
            name = "InvalidName"
        }
        if (name.equals("null", ignoreCase = true) || name.contains("\\")) {
            toast("Invalid name")
            name = "InvalidName"
        }
        if (fridges.any { it.name.equals(name, ignoreCase = true) && !edit }) {
            toast("Name already in use")
            name = "InvalidName"
        }

        var icon: Int = R.drawable.default_fridge
        if (dialogView.findViewById<RadioButton>(R.id.fridge1RadioButton)!!.isChecked) { icon = R.drawable.fridge_1 }
        if (dialogView.findViewById<RadioButton>(R.id.fridge2RadioButton)!!.isChecked) { icon = R.drawable.fridge_2 }
        val depthToggle = dialogView.findViewById<ToggleButton>(R.id.depthToggleButton)
        val depth = if (depthToggle?.isChecked == true) 2 else 1

        return Fridge(
            name = name,
            icon = icon,
            sections = dialogView.findViewById<CustomSpinner>(R.id.sectionsCustomSpinner).count,
            columns = dialogView.findViewById<CustomSpinner>(R.id.columnsCustomSpinner).count,
            rps = dialogView.findViewById<CustomSpinner>(R.id.rpsCustomSpinner).count,
            depth = depth
        )
    }

    //Wine class
    data class Wine(
        var name: String = "null",
        var price: Int = 10,
        var year: Int = 2000,
        var type: String = "Type",
        var vineyard: String = "Vineyard",
        var region: String = "Region",
        var country: String = "Country",
        var grapeVariety: String = "Variety",
        var rating: Double = 50.0,
        var pairings: String = "",
        var drinkBy: Int = 2050,
        var description: String = "desc",
        var imagePath: String = "null",
        var parentFridge: String = "New Fridge",
    )

    //adapter
    class FridgeAdapter(
        private var fridges: List<Fridge>,
        private val onFridgeClick: (Fridge) -> Unit,
        private val onFridgeLongClick: (Fridge) -> Unit
    ) : RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder>() {

        class FridgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fridgeImageView: ImageView = view.findViewById(R.id.wineImageView)
            val fridgeNameTextView: TextView = view.findViewById(R.id.fridgeNameTextView)
            val fridgeWineCountTextView: TextView = view.findViewById(R.id.wineDescTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridgeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fridge_card, parent, false)
            return FridgeViewHolder(view)
        }

        override fun onBindViewHolder(holder: FridgeViewHolder, i: Int) {
            val fridge = fridges[i]
            holder.fridgeImageView.setImageResource(fridge.icon)
            holder.fridgeNameTextView.text = fridge.name

            val totalWines = fridge.depth * fridge.sections * fridge.rps * fridge.columns
            val filledSlots = fridge.wines.sumOf { layer ->
                layer.sumOf { section ->
                    section.sumOf { row ->
                        row.count { it.name != "null" }
                    }
                }
            }

            holder.fridgeWineCountTextView.text = "Wines: $filledSlots/$totalWines"
            holder.itemView.setOnClickListener { onFridgeClick(fridge) }
            holder.itemView.setOnLongClickListener { onFridgeLongClick(fridge); true }
            holder.itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }
                }
                false
            }
        }

        fun updateList(newFridges: List<Fridge>) {
            fridges = newFridges
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = fridges.size
    }

    //save/load
    override fun onResume() {
        super.onResume()
        fridgeRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        saveFridges(requireContext())
    }

    override fun onStop() {
        super.onStop()
        saveFridges(requireContext())
    }
}

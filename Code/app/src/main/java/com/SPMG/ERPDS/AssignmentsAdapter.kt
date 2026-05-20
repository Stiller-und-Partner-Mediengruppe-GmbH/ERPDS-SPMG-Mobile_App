package com.SPMG.ERPDS

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

enum class AssignmentStatus { NEW, ONGOING, COMPLETED }

data class Assignment(
    val id: String,
    val title: String,
    val details: String,
    var status: AssignmentStatus = AssignmentStatus.NEW,
    var creationTime: String = "",
    var acceptanceTime: String = "",
    var completionTime: String = ""
)

sealed class AdapterItem {
    data class Header(val title: String, val status: AssignmentStatus, var isExpanded: Boolean = true) : AdapterItem()
    data class AssignmentItem(val assignment: Assignment) : AdapterItem()
}

class AssignmentsAdapter(
    private var allItems: List<AdapterItem>,
    private val onOpenClick: (Assignment) -> Unit,
    private val onDismissClick: (Assignment) -> Unit,
    private val onReportClick: (Assignment) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var visibleItems: MutableList<AdapterItem> = mutableListOf()

    init {
        updateVisibleItems()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NEW = 1
        private const val TYPE_PAST = 2
    }

    private fun updateVisibleItems() {
        visibleItems.clear()
        var currentHeaderExpanded = true
        for (item in allItems) {
            if (item is AdapterItem.Header) {
                visibleItems.add(item)
                currentHeaderExpanded = item.isExpanded
            } else if (item is AdapterItem.AssignmentItem && currentHeaderExpanded) {
                visibleItems.add(item)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = visibleItems[position]) {
            is AdapterItem.Header -> TYPE_HEADER
            is AdapterItem.AssignmentItem -> {
                if (item.assignment.status == AssignmentStatus.COMPLETED) TYPE_PAST else TYPE_NEW
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_assignment_header, parent, false))
            TYPE_NEW -> CurrentViewHolder(inflater.inflate(R.layout.item_assignment, parent, false))
            else -> PastViewHolder(inflater.inflate(R.layout.item_assignment_past, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = visibleItems[position]) {
            is AdapterItem.Header -> (holder as HeaderViewHolder).bind(item) {
                item.isExpanded = !item.isExpanded
                updateVisibleItems()
                notifyDataSetChanged()
            }
            is AdapterItem.AssignmentItem -> {
                when (holder) {
                    is CurrentViewHolder -> holder.bind(item.assignment, onOpenClick, onDismissClick)
                    is PastViewHolder -> holder.bind(item.assignment, onReportClick)
                }
            }
        }
    }

    override fun getItemCount() = visibleItems.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.headerTitle)
        private val arrowImage: ImageView = view.findViewById(R.id.headerArrow)

        fun bind(header: AdapterItem.Header, onClick: () -> Unit) {
            titleTextView.text = header.title
            arrowImage.rotation = if (header.isExpanded) 180f else 0f
            itemView.setOnClickListener { onClick() }
        }
    }

    class CurrentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.assignmentTitle)
        private val detailsTextView: TextView = view.findViewById(R.id.assignmentDetails)
        private val buttonOpen: View = view.findViewById(R.id.buttonOpen)
        private val buttonDismiss: View = view.findViewById(R.id.buttonDismiss)

        fun bind(assignment: Assignment, onOpen: (Assignment) -> Unit, onDismiss: (Assignment) -> Unit) {
            titleTextView.text = assignment.title
            detailsTextView.text = assignment.details
            buttonOpen.setOnClickListener { onOpen(assignment) }
            buttonDismiss.setOnClickListener { onDismiss(assignment) }
        }
    }

    class PastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.assignmentTitle)
        private val detailsTextView: TextView = view.findViewById(R.id.assignmentDetails)
        private val buttonReport: View = view.findViewById(R.id.buttonShowReport)

        fun bind(assignment: Assignment, onReport: (Assignment) -> Unit) {
            titleTextView.text = assignment.title
            detailsTextView.text = assignment.details
            buttonReport.setOnClickListener { onReport(assignment) }
        }
    }

    fun setData(items: List<AdapterItem>) {
        allItems = items
        updateVisibleItems()
        notifyDataSetChanged()
    }
}
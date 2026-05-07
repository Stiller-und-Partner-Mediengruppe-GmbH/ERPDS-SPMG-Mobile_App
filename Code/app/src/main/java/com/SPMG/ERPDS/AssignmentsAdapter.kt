package com.SPMG.ERPDS

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Assignment(val title: String, val details: String, val isCurrent: Boolean = false)

class AssignmentsAdapter(
    private var assignments: List<Assignment>,
    private val onOpenClick: (Assignment) -> Unit,
    private val onDismissClick: (Assignment) -> Unit,
    private val onReportClick: (Assignment) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CURRENT = 0
        private const val TYPE_PAST = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (assignments[position].isCurrent) TYPE_CURRENT else TYPE_PAST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CURRENT) {
            val view = inflater.inflate(R.layout.item_assignment, parent, false)
            CurrentViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_assignment_past, parent, false)
            PastViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val assignment = assignments[position]
        if (holder is CurrentViewHolder) {
            holder.bind(assignment, onOpenClick, onDismissClick)
        } else if (holder is PastViewHolder) {
            holder.bind(assignment, onReportClick)
        }
    }

    override fun getItemCount() = assignments.size

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

    fun updateData(newAssignments: List<Assignment>) {
        assignments = newAssignments
        notifyDataSetChanged()
    }
}
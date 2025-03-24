package com.example.testpdflibrary

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.powerpdflibrary.PowerPdfLibChecker

interface PdfDownloadCallback {
    fun onStartDownload(pdf: PdfModel)
    fun onProgressUpdate(pdfId: String, progress: Int)
    fun onDownloadComplete(pdfId: String, htmlFilePath: String)
    fun onDownloadFailed(pdfId: String)
    fun onOpenFile(pdf: PdfModel)
}

class PdfDownloadAdapter(
    private val pdfList: List<PdfModel>,
    private val downloadCallback: PdfDownloadCallback,
    private val context: Context
) : RecyclerView.Adapter<PdfDownloadAdapter.PdfViewHolder>() {

    private val progressMap = mutableMapOf<String, Int>()
    private val completedSet = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty()) {
            val progress = payloads[0] as Int
            holder.updateProgress(progress)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val pdf = pdfList[position]
        holder.bind(pdf)

        val progress = progressMap[pdf.pdfId] ?: 0

        if (completedSet.contains(pdf.pdfId)) {
            holder.showCompletedState()
        } else if (progress in 1..99) {
            holder.updateProgress(progress)
        } else {
            holder.showInitialState()
        }

        holder.downloadButton.setOnClickListener {
            downloadCallback.onStartDownload(pdf)
        }

        holder.llOpenPdf.setOnClickListener {
            if (PowerPdfLibChecker.checkPdfExits(context, pdf.pdfName, pdf.pdfId)) {
                downloadCallback.onOpenFile(pdf)
            }
        }

        holder.deleteIcon.setOnClickListener {
            PowerPdfLibChecker.deletePdfFile(context, pdf.pdfName, pdf.pdfId)
            completedSet.remove(pdf.pdfId)
            holder.showInitialState()
        }
    }

    override fun getItemCount(): Int = pdfList.size

    fun updateProgress(pdfId: String, progress: Int) {
        progressMap[pdfId] = progress
        val position = pdfList.indexOfFirst { it.pdfId == pdfId }
        if (position != -1) {
            notifyItemChanged(position, progress)
        }
    }

    fun downloadComplete(pdfId: String, pdfName: String) {
        if (PowerPdfLibChecker.checkPdfExits(context, pdfName, pdfId)) {
            completedSet.add(pdfId)
            progressMap[pdfId] = 100
            val position = pdfList.indexOfFirst { it.pdfId == pdfId }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
    }

    fun downloadFailed(pdfId: String) {
        progressMap[pdfId] = -1
        val position = pdfList.indexOfFirst { it.pdfId == pdfId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    inner class PdfViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.pdfTitle)
        val downloadButton: ImageView = view.findViewById(R.id.downloadButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val doneImage: ImageView = view.findViewById(R.id.doneImage)
        val statusText: TextView = view.findViewById(R.id.statusText)
        val deleteIcon: ImageView = view.findViewById(R.id.deleteIcon)
        val llOpenPdf: ConstraintLayout = view.findViewById(R.id.llOpenPdf)

        fun bind(pdf: PdfModel) {
            title.text = pdf.pdfName
            if (PowerPdfLibChecker.checkPdfExits(context, pdf.pdfName, pdf.pdfId)) {
                showCompletedState()
            } else {
                showInitialState()
            }
        }

        fun updateProgress(progress: Int) {
            progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
            progressText.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
            statusText.visibility = if (progress in 0..99) View.VISIBLE else View.GONE
            progressBar.progress = progress
            progressText.text = "$progress%"

            if (progress == 100) {
                showCompletedState()
            } else if (progress >= 0) {
                statusText.text = "Downloading..."
                downloadButton.visibility = View.GONE
            } else {
                showInitialState()
            }
        }

        fun showCompletedState() {
            doneImage.visibility = View.VISIBLE
            downloadButton.visibility = View.GONE
            deleteIcon.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
            statusText.visibility = View.GONE
        }

        fun showInitialState() {
            doneImage.visibility = View.GONE
            downloadButton.visibility = View.VISIBLE
            deleteIcon.visibility = View.GONE
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
            statusText.visibility = View.GONE
        }
    }
}

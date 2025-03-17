package com.example.testpdflibrary

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

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
    context: Context
) : RecyclerView.Adapter<PdfDownloadAdapter.PdfViewHolder>() {

    private val progressMap = mutableMapOf<String, Int>() // Track progress per item

    private val filesDirPath = context.filesDir.absolutePath

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val pdf = pdfList[position]
        holder.bind(pdf)

        val progress = progressMap[pdf.pdfId] ?: 0
        holder.updateProgress(progress)


        holder.downloadButton.setOnClickListener {
            downloadCallback.onStartDownload(pdf) // Notify MainActivity to start download
        }

        holder.openButton.setOnClickListener {
            downloadCallback.onOpenFile(pdf)
        }

    }

    override fun getItemCount(): Int = pdfList.size

    fun updateProgress(pdfId: String, progress: Int) {
        progressMap[pdfId] = progress
//        Log.e("jj", progress.toString())
        val position = pdfList.indexOfFirst { it.pdfId == pdfId }
        if (position != -1) {
            notifyItemChanged(position) // Update only the specific item
        }
    }
    inner class PdfViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.pdfTitle)
        val downloadButton: Button = view.findViewById(R.id.downloadButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val openButton: Button = view.findViewById(R.id.openButton)

        fun bind(pdf: PdfModel) {
            val pdfID = pdf.pdfId
            val pdfName = pdf.pdfName
            val htmlFilePath = "$filesDirPath/$pdfName$pdfID.html"
            title.text = pdf.pdfName
            progressBar.progress = 0
            progressText.text = "0%"
            if (File(htmlFilePath).exists()) {
                openButton.visibility = View.VISIBLE
                downloadButton.visibility = View.GONE
            }
        }

        fun updateProgress(progress: Int) {
            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
            progressBar.progress = progress
            progressText.text = "$progress%"

            if (progress == 100) {
                openButton.visibility = View.VISIBLE
                downloadButton.visibility = View.GONE
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
            } else if (progress > 0) {
                downloadButton.text = "Downloading..."
            } else {
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                downloadButton.text = "Download"
            }
        }
    }
}

package com.example.testpdflibrary

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.pdfdownloadersandip.PdfModel
import com.example.powerpdflibrary.PdfViewerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), PdfDownloadCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PdfDownloadAdapter
    private val pdfList = listOf(
        PdfModel("1", "Sample PDF 1", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf"),
        PdfModel("2", "Sample PDF 2", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PdfDownloadAdapter(pdfList, this, this)
        recyclerView.adapter = adapter


    }

    override fun onStartDownload(pdf: PdfModel) {
        startPdfDownloadWorker(pdf)
    }

    override fun onProgressUpdate(pdfId: String, progress: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.updateProgress(pdfId, progress)
        }
    }

    override fun onDownloadComplete(pdfId: String, htmlFilePath: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Download complete: $htmlFilePath", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDownloadFailed(pdfId: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Download failed for PDF ID: $pdfId", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOpenFile(pdf: PdfModel) {
        PdfViewerActivity.openPdfViewer(this, pdf.pdfUrl, pdf.pdfName, pdf.pdfId)
    }

    private fun startPdfDownloadWorker(pdf: PdfModel) {
        val workManager = WorkManager.getInstance(this)
        val data = Data.Builder()
            .putString("PDF_ID", pdf.pdfId)
            .putString("PDF_URL", pdf.pdfUrl)
            .putString("PDF_NAME", pdf.pdfName)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Prevents running on low battery
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<PdfDownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(pdf.pdfId, ExistingWorkPolicy.KEEP, downloadRequest)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            workManager.getWorkInfoByIdLiveData(downloadRequest.id).observeForever { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getInt("PROGRESS", 0)
                            onProgressUpdate(pdf.pdfId, progress)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val outputData = workInfo.outputData
                            val htmlFilePath = outputData.getString("HTML_FILE_PATH") ?: ""
                            onDownloadComplete(pdf.pdfId, htmlFilePath)
                        }
                        WorkInfo.State.FAILED -> {
                            onDownloadFailed(pdf.pdfId)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
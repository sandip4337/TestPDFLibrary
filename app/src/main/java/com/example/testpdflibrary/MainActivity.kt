package com.example.testpdflibrary

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.powerpdflibrary.PdfDownloadWorker
import com.example.powerpdflibrary.PdfViewerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), PdfDownloadCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PdfDownloadAdapter
    private val pdfList = listOf(
        PdfModel("1", "Sample PDF 1", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf"),
        PdfModel("2", "Sample PDF 2", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf"),
        PdfModel("3", "Sample PDF 3", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf"),
        PdfModel("4", "Sample PDF 4", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf"),
        PdfModel("5", "Sample PDF 5", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf"),
        PdfModel("6", "Sample PDF 6", "https://history.nasa.gov/alsj/a17/A17_FlightPlan.pdf")
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
            .setRequiresBatteryNotLow(true)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<PdfDownloadWorker>()
            .addTag(pdf.pdfId) // Tagging the worker
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(pdf.pdfId, ExistingWorkPolicy.KEEP, downloadRequest)

        observeWorkInfo(pdf)
    }

    private fun observeWorkInfo(pdf: PdfModel) {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosByTagLiveData(pdf.pdfId).observe(this) { workInfos ->
            workInfos?.forEach { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("PROGRESS", 0)
                        Log.e("PdfDownloadWorker", "Progress: $progress for PDF ${pdf.pdfId}")
                        runOnUiThread { adapter.updateProgress(pdf.pdfId, progress) }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        runOnUiThread { adapter.downloadComplete(pdf.pdfId) }
                    }
                    WorkInfo.State.FAILED -> {
                        onDownloadFailed(pdf.pdfId)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val workManager = WorkManager.getInstance(this)
        lifecycleScope.launch(Dispatchers.IO) {
            pdfList.forEach { pdf ->
                val workInfos = workManager.getWorkInfosByTag(pdf.pdfId).get()
                workInfos?.forEach { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getInt("PROGRESS", 0)
                            runOnUiThread { adapter.updateProgress(pdf.pdfId, progress) }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            runOnUiThread { adapter.downloadComplete(pdf.pdfId) }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

}

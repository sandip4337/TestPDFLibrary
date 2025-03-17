package com.example.pdfdownloadersandip

data class PdfModel(
    val pdfId: String,  // Unique identifier for each PDF
    val pdfName: String, // Name of the PDF
    val pdfUrl: String   // URL from where the PDF will be downloaded
)

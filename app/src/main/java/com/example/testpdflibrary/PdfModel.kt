package com.example.testpdflibrary

data class PdfModel(
    val pdfId: String,  // Unique identifier for each PDF
    val pdfName: String, // Name of the PDF
    val pdfUrl: String  , // URL from where the PDF will be downloaded
    val baseUrl: String // Local file path of the downloaded PDF
)

package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.local.ProductEntity
import com.example.data.local.ShopProfileEntity

object QrPrintHelper {

    /**
     * Prints a single product QR code sticker formatted for standard thermal label printers (58mm/80mm) or A4 cutouts.
     */
    fun printSingleProductQr(
        context: Context,
        product: ProductEntity,
        shopProfile: ShopProfileEntity?
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "প্রিন্ট সার্ভিস পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                return
            }

            val qrData = product.qrCode?.ifBlank { product.id.toString() } ?: product.id.toString()
            val qrBase64 = QrCodeGenerator.generateQrBase64(qrData, 300)
            val shopName = shopProfile?.shopName?.ifBlank { "দোকানের হিসাব" } ?: "দোকানের হিসাব"

            val html = """
            <!DOCTYPE html>
            <html lang="bn">
            <head>
                <meta charset="UTF-8">
                <style>
                    @page {
                        size: auto;
                        margin: 4mm;
                    }
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        margin: 0;
                        padding: 10px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        background-color: #ffffff;
                    }
                    .sticker-card {
                        width: 240px;
                        border: 2px dashed #0f172a;
                        border-radius: 8px;
                        padding: 12px;
                        text-align: center;
                        background: #ffffff;
                        box-sizing: border-box;
                    }
                    .shop-name {
                        font-size: 13px;
                        font-weight: bold;
                        color: #1e3a8a;
                        margin-bottom: 4px;
                        text-transform: uppercase;
                    }
                    .product-name {
                        font-size: 16px;
                        font-weight: 800;
                        color: #0f172a;
                        margin: 4px 0;
                        line-height: 1.2;
                    }
                    .qr-img {
                        width: 140px;
                        height: 140px;
                        margin: 6px auto;
                        display: block;
                    }
                    .price-tag {
                        font-size: 18px;
                        font-weight: 900;
                        color: #047857;
                        background-color: #ecfdf5;
                        border: 1px solid #10b981;
                        padding: 4px 8px;
                        border-radius: 6px;
                        margin-top: 6px;
                        display: inline-block;
                    }
                    .code-text {
                        font-size: 10px;
                        color: #64748b;
                        margin-top: 4px;
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="sticker-card">
                    <div class="shop-name">$shopName</div>
                    <div class="product-name">${escapeHtml(product.name)}</div>
                    ${if (qrBase64.isNotBlank()) "<img class='qr-img' src='data:image/png;base64,$qrBase64' alt='QR' />" else ""}
                    <div class="price-tag">মূল্য: ${BengaliFormatters.toBanglaCurrency(product.sellPrice)} / ${product.unit}</div>
                    <div class="code-text">QR: ${escapeHtml(qrData)}</div>
                </div>
            </body>
            </html>
            """.trimIndent()

            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false
            webView.settings.defaultTextEncodingName = "utf-8"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    val jobName = "${product.name}_QR_Sticker"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                        .setResolution(PrintAttributes.Resolution("qr_sticker", "QR Sticker Print", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(jobName, printAdapter, printAttributes)
                }
            }

            webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null)
            Toast.makeText(context, "QR স্টিকার প্রিন্ট চালু হচ্ছে...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "QR প্রিন্ট এরর: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Prints an A4 Sheet Grid containing multiple product QR stickers ready for batch printing and cutting.
     */
    fun printMultipleQrSheet(
        context: Context,
        products: List<ProductEntity>,
        shopProfile: ShopProfileEntity?
    ) {
        if (products.isEmpty()) {
            Toast.makeText(context, "প্রিন্ট করার জন্য কোনো QR যুক্ত পণ্য নেই", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "প্রিন্ট সার্ভিস পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                return
            }

            val shopName = shopProfile?.shopName?.ifBlank { "দোকানের হিসাব" } ?: "দোকানের হিসাব"
            val stickersHtml = StringBuilder()

            products.forEach { prod ->
                val qrData = prod.qrCode?.ifBlank { prod.id.toString() } ?: prod.id.toString()
                val qrBase64 = QrCodeGenerator.generateQrBase64(qrData, 220)
                stickersHtml.append(
                    """
                    <div class="grid-sticker">
                        <div class="s-shop">$shopName</div>
                        <div class="s-name">${escapeHtml(prod.name)}</div>
                        ${if (qrBase64.isNotBlank()) "<img class='s-qr' src='data:image/png;base64,$qrBase64' alt='QR' />" else ""}
                        <div class="s-price">${BengaliFormatters.toBanglaCurrency(prod.sellPrice)}</div>
                        <div class="s-code">${escapeHtml(qrData)}</div>
                    </div>
                    """.trimIndent()
                )
            }

            val fullHtml = """
            <!DOCTYPE html>
            <html lang="bn">
            <head>
                <meta charset="UTF-8">
                <style>
                    @page {
                        size: A4;
                        margin: 10mm;
                    }
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        margin: 0;
                        padding: 10px;
                        background: #ffffff;
                    }
                    .header-title {
                        text-align: center;
                        font-size: 18px;
                        font-weight: bold;
                        color: #1e3a8a;
                        margin-bottom: 12px;
                        border-bottom: 2px solid #cbd5e1;
                        padding-bottom: 6px;
                    }
                    .grid-container {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 12px;
                        justify-content: flex-start;
                    }
                    .grid-sticker {
                        width: 170px;
                        border: 1.5px dashed #475569;
                        border-radius: 6px;
                        padding: 8px;
                        text-align: center;
                        box-sizing: border-box;
                        page-break-inside: avoid;
                    }
                    .s-shop {
                        font-size: 10px;
                        color: #1e3a8a;
                        font-weight: bold;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }
                    .s-name {
                        font-size: 12px;
                        font-weight: bold;
                        color: #0f172a;
                        margin: 2px 0;
                        height: 28px;
                        overflow: hidden;
                        line-height: 1.2;
                    }
                    .s-qr {
                        width: 100px;
                        height: 100px;
                        margin: 4px auto;
                        display: block;
                    }
                    .s-price {
                        font-size: 13px;
                        font-weight: bold;
                        color: #047857;
                        background: #ecfdf5;
                        border-radius: 4px;
                        padding: 2px 4px;
                    }
                    .s-code {
                        font-size: 9px;
                        color: #64748b;
                        margin-top: 2px;
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="header-title">🏷️ $shopName - পণ্যের QR কোড স্টিকার শিট (${BengaliFormatters.toBanglaNumber(products.size)} টি পণ্য)</div>
                <div class="grid-container">
                    $stickersHtml
                </div>
            </body>
            </html>
            """.trimIndent()

            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false
            webView.settings.defaultTextEncodingName = "utf-8"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    val jobName = "${shopName}_QR_Sheet"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("qr_sheet", "QR Sheet Print", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(jobName, printAdapter, printAttributes)
                }
            }

            webView.loadDataWithBaseURL(null, fullHtml, "text/html; charset=utf-8", "UTF-8", null)
            Toast.makeText(context, "QR শিট প্রিন্ট শুরু হচ্ছে...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "প্রিন্ট এরর: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printProductQrSheet(
        context: Context,
        products: List<ProductEntity>,
        shopProfile: ShopProfileEntity?
    ) {
        printMultipleQrSheet(context, products, shopProfile)
    }

    fun printPaymentStandee(
        context: Context,
        shopProfile: ShopProfileEntity?,
        qrDataCustom: String? = null
    ) {
        val prof = shopProfile ?: ShopProfileEntity()
        printShopPaymentQrPoster(context, prof, qrDataCustom)
    }

    /**
     * Prints a Shop Counter Payment Standee / Poster (bKash, Nagad, Rocket, or Shop QR)
     */
    fun printShopPaymentQrPoster(
        context: Context,
        shopProfile: ShopProfileEntity,
        qrDataCustom: String? = null
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "প্রিন্ট সার্ভিস পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                return
            }

            val qrData = qrDataCustom ?: "SHOP-PAY:${shopProfile.shopName}:${shopProfile.phone}"
            val qrBase64 = QrCodeGenerator.generateQrBase64(qrData, 350)

            val html = """
            <!DOCTYPE html>
            <html lang="bn">
            <head>
                <meta charset="UTF-8">
                <style>
                    @page {
                        size: A4;
                        margin: 15mm;
                    }
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        text-align: center;
                        background: #ffffff;
                    }
                    .poster-card {
                        max-width: 450px;
                        margin: auto;
                        border: 3px solid #1e3a8a;
                        border-radius: 16px;
                        padding: 30px 20px;
                        background: #ffffff;
                        box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
                    }
                    .title {
                        font-size: 26px;
                        font-weight: 800;
                        color: #1e3a8a;
                        margin-bottom: 6px;
                    }
                    .subtitle {
                        font-size: 14px;
                        color: #475569;
                        margin-bottom: 20px;
                    }
                    .qr-box {
                        padding: 16px;
                        background: #f8fafc;
                        border: 2px solid #e2e8f0;
                        border-radius: 12px;
                        display: inline-block;
                        margin-bottom: 16px;
                    }
                    .qr-img {
                        width: 220px;
                        height: 220px;
                        display: block;
                    }
                    .scan-text {
                        font-size: 16px;
                        font-weight: bold;
                        color: #047857;
                        margin: 10px 0;
                    }
                    .number-tag {
                        font-size: 18px;
                        font-weight: bold;
                        color: #0f172a;
                        background: #f1f5f9;
                        padding: 8px 16px;
                        border-radius: 8px;
                        display: inline-block;
                        margin-top: 8px;
                    }
                    .footer-note {
                        font-size: 12px;
                        color: #94a3b8;
                        margin-top: 24px;
                    }
                </style>
            </head>
            <body>
                <div class="poster-card">
                    <div class="title">${escapeHtml(shopProfile.shopName.ifBlank { "আমার দোকান" })}</div>
                    <div class="subtitle">${escapeHtml(shopProfile.address.ifBlank { "ডিজিটাল পেমেন্ট ও হিসাব কাউন্টার" })}</div>
                    
                    <div class="qr-box">
                        ${if (qrBase64.isNotBlank()) "<img class='qr-img' src='data:image/png;base64,$qrBase64' alt='QR' />" else ""}
                    </div>

                    <div class="scan-text">📱 যেকোনো অ্যাপ দিয়ে স্ক্যান করে পেমেন্ট করুন</div>
                    <div class="number-tag">📞 মোবাইল / বিকাশ / নগদ: ${escapeHtml(shopProfile.phone.ifBlank { "০১৭XXXXXXXX" })}</div>
                    
                    <div class="footer-note">
                        ক্যাশলেস থাকুন, নিরাপদে কেনাকাটা করুন | ডিজিটাল হিসাব খাতা
                    </div>
                </div>
            </body>
            </html>
            """.trimIndent()

            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false
            webView.settings.defaultTextEncodingName = "utf-8"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    val jobName = "${shopProfile.shopName}_Payment_Poster"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("payment_poster", "Payment Poster Print", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(jobName, printAdapter, printAttributes)
                }
            }

            webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null)
            Toast.makeText(context, "দোকানের পেমেন্ট পোস্টার প্রিন্ট হচ্ছে...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "পোস্টার প্রিন্ট এরর: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

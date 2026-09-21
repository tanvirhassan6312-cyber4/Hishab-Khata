package com.example.util

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.local.MemoEntity
import com.example.data.local.MemoItem

object MemoPrintHelper {

    /**
     * Prints the memo using Android's native PrintManager and a hidden WebView.
     * This opens the standard Android Print Preview dialog, allowing:
     * 1. Direct printing to any WiFi, Bluetooth, or Network printer.
     * 2. Direct export / Save as PDF on device.
     */
    fun printMemo(context: Context, memo: MemoEntity, items: List<MemoItem>) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "প্রিন্ট সার্ভিস পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                return
            }

            val htmlDocument = generateMemoHtml(memo, items)

            // Create a WebView on the main UI thread to render the HTML for printing
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false
            webView.settings.defaultTextEncodingName = "utf-8"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val jobName = "${memo.shopName}_মেমো_${memo.memoNumber}"
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("dokan_memo", "Memo Print", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()

                        printManager.print(jobName, printAdapter, printAttributes)
                    } catch (e: Exception) {
                        Toast.makeText(context, "প্রিন্ট শুরু করা যায়নি: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            webView.loadDataWithBaseURL(null, htmlDocument, "text/html; charset=utf-8", "UTF-8", null)
            Toast.makeText(context, "প্রিন্টার ও পিডিএফ প্রিভিউ চালু হচ্ছে...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "প্রিন্ট এরর: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a beautifully formatted Bengali Invoice HTML document.
     */
    fun generateMemoHtml(memo: MemoEntity, items: List<MemoItem>): String {
        val banglaDate = BengaliFormatters.formatDateBangla(memo.date)
        val inWords = MemoUtils.numberToBanglaWords(memo.grandTotal)

        val itemsRows = StringBuilder()
        items.forEachIndexed { index, item ->
            val sl = BengaliFormatters.toBanglaNumber(index + 1)
            val qty = "${BengaliFormatters.toBanglaNumber(item.quantity)} ${item.unit}"
            val rate = BengaliFormatters.toBanglaCurrency(item.unitPrice)
            val total = BengaliFormatters.toBanglaCurrency(item.total)

            itemsRows.append(
                """
                <tr>
                    <td style="text-align: center; width: 40px;">$sl</td>
                    <td><strong>${escapeHtml(item.itemName)}</strong></td>
                    <td style="text-align: center; width: 90px;">$qty</td>
                    <td style="text-align: right; width: 100px;">$rate</td>
                    <td style="text-align: right; width: 110px;"><strong>$total</strong></td>
                </tr>
                """.trimIndent()
            )
        }

        val discountRow = if (memo.discountAmount > 0) {
            """
            <tr>
                <td colspan="4" style="text-align: right; color: #dc2626;">ছাড় / ডিসকাউন্ট:</td>
                <td style="text-align: right; color: #dc2626;"><strong>-${BengaliFormatters.toBanglaCurrency(memo.discountAmount)}</strong></td>
            </tr>
            """.trimIndent()
        } else ""

        val vatRow = if (memo.vatAmount > 0) {
            """
            <tr>
                <td colspan="4" style="text-align: right;">ভ্যাট (${BengaliFormatters.toBanglaNumber(memo.vatPercent)}%):</td>
                <td style="text-align: right;">+${BengaliFormatters.toBanglaCurrency(memo.vatAmount)}</td>
            </tr>
            """.trimIndent()
        } else ""

        val dueRow = if (memo.dueAmount > 0) {
            """
            <tr style="background-color: #fff7ed;">
                <td colspan="4" style="text-align: right; color: #ea580c; font-weight: bold;">অবশিষ্ট বাকি:</td>
                <td style="text-align: right; color: #ea580c; font-weight: bold; font-size: 15px;">${BengaliFormatters.toBanglaCurrency(memo.dueAmount)}</td>
            </tr>
            """.trimIndent()
        } else {
            """
            <tr style="background-color: #f0fdf4;">
                <td colspan="4" style="text-align: right; color: #16a34a; font-weight: bold;">পরিশোধের অবস্থা:</td>
                <td style="text-align: right; color: #16a34a; font-weight: bold;">সম্পূর্ণ পরিশোধিত</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="bn">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>মেমো - ${memo.memoNumber}</title>
            <style>
                @page {
                    size: A4;
                    margin: 15mm 15mm 15mm 15mm;
                }
                body {
                    font-family: 'Segoe UI', Arial, sans-serif;
                    color: #1e293b;
                    background-color: #ffffff;
                    margin: 0;
                    padding: 20px;
                    font-size: 13px;
                    line-height: 1.4;
                }
                .invoice-box {
                    max-width: 800px;
                    margin: auto;
                    border: 1px solid #cbd5e1;
                    border-radius: 8px;
                    padding: 24px;
                    background: #ffffff;
                }
                .header-table {
                    width: 100%;
                    margin-bottom: 20px;
                    border-bottom: 2px solid #0f172a;
                    padding-bottom: 12px;
                }
                .shop-title {
                    font-size: 24px;
                    font-weight: 800;
                    color: #1e3a8a;
                    margin: 0 0 4px 0;
                }
                .shop-meta {
                    font-size: 12px;
                    color: #475569;
                    margin: 2px 0;
                }
                .memo-badge {
                    display: inline-block;
                    background-color: #1e3a8a;
                    color: #ffffff;
                    padding: 4px 14px;
                    border-radius: 4px;
                    font-weight: bold;
                    font-size: 12px;
                    text-transform: uppercase;
                    margin-top: 4px;
                }
                .customer-info-table {
                    width: 100%;
                    margin-bottom: 16px;
                    background-color: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    padding: 10px 14px;
                }
                .items-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 16px;
                }
                .items-table th {
                    background-color: #0f172a;
                    color: #ffffff;
                    font-weight: bold;
                    padding: 8px 10px;
                    font-size: 12px;
                    border: 1px solid #0f172a;
                }
                .items-table td {
                    padding: 8px 10px;
                    border: 1px solid #e2e8f0;
                    font-size: 13px;
                }
                .items-table tr:nth-child(even) {
                    background-color: #f8fafc;
                }
                .totals-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 10px;
                }
                .totals-table td {
                    padding: 6px 10px;
                    font-size: 13px;
                }
                .grand-total-row {
                    background-color: #eff6ff;
                    font-size: 16px !important;
                    font-weight: bold;
                    color: #1e3a8a;
                    border-top: 2px solid #1e3a8a;
                    border-bottom: 2px solid #1e3a8a;
                }
                .in-words-box {
                    background-color: #f1f5f9;
                    border-left: 4px solid #1e3a8a;
                    padding: 10px 14px;
                    margin: 14px 0;
                    font-size: 12px;
                    font-weight: 600;
                    color: #1e293b;
                }
                .signatures {
                    margin-top: 50px;
                    width: 100%;
                }
                .sig-line {
                    border-top: 1px dashed #64748b;
                    display: inline-block;
                    width: 160px;
                    text-align: center;
                    padding-top: 6px;
                    font-size: 11px;
                    color: #475569;
                }
                .footer-notice {
                    text-align: center;
                    margin-top: 24px;
                    font-size: 11px;
                    color: #64748b;
                    border-top: 1px solid #e2e8f0;
                    padding-top: 10px;
                }
                @media print {
                    body {
                        padding: 0;
                    }
                    .invoice-box {
                        border: none;
                        padding: 0;
                    }
                }
            </style>
        </head>
        <body>
            <div class="invoice-box">
                <!-- Shop Header -->
                <table class="header-table" cellpadding="0" cellspacing="0">
                    <tr>
                        <td style="vertical-align: top;">
                            <div class="shop-title">${escapeHtml(memo.shopName)}</div>
                            ${if (memo.shopAddress.isNotBlank()) "<div class='shop-meta'>📍 ঠিকানা: ${escapeHtml(memo.shopAddress)}</div>" else ""}
                            ${if (memo.shopPhone.isNotBlank()) "<div class='shop-meta'>📞 মোবাইল: ${escapeHtml(memo.shopPhone)}</div>" else ""}
                        </td>
                        <td style="text-align: right; vertical-align: top;">
                            <div class="memo-badge">${escapeHtml(memo.memoType)}</div>
                            <div style="font-size: 14px; font-weight: bold; margin-top: 6px; color: #0f172a;">মেমো নং: ${escapeHtml(memo.memoNumber)}</div>
                            <div class="shop-meta">তারিখ: <strong>$banglaDate</strong></div>
                            ${if (memo.preparedBy.isNotBlank()) "<div class='shop-meta'>ম্যানেজার / প্রস্তুতকারক: ${escapeHtml(memo.preparedBy)}</div>" else ""}
                        </td>
                    </tr>
                </table>

                <!-- Customer Details -->
                <table class="customer-info-table" cellpadding="0" cellspacing="0">
                    <tr>
                        <td style="width: 50%; vertical-align: top;">
                            <div style="font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: bold;">ক্রেতা / প্রাপকের নাম:</div>
                            <div style="font-size: 15px; font-weight: bold; color: #0f172a; margin-top: 2px;">${escapeHtml(memo.customerName)}</div>
                            ${if (!memo.customerPhone.isNullOrBlank()) "<div style='font-size: 12px; color: #334155; margin-top: 2px;'>ফোন: ${escapeHtml(memo.customerPhone)}</div>" else ""}
                        </td>
                        <td style="width: 50%; vertical-align: top; text-align: right;">
                            ${if (!memo.customerAddress.isNullOrBlank()) "<div style='font-size: 11px; color: #64748b; font-weight: bold;'>ঠিকানা:</div><div style='font-size: 12px; color: #334155;'>${escapeHtml(memo.customerAddress)}</div>" else ""}
                        </td>
                    </tr>
                </table>

                <!-- Items Table -->
                <table class="items-table" cellpadding="0" cellspacing="0">
                    <thead>
                        <tr>
                            <th style="text-align: center;">নং</th>
                            <th style="text-align: left;">পণ্যের নাম ও বিবরণ</th>
                            <th style="text-align: center;">পরিমাণ</th>
                            <th style="text-align: right;">দর (৳)</th>
                            <th style="text-align: right;">মোট টাকা (৳)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsRows
                    </tbody>
                </table>

                <!-- Calculations Table -->
                <table class="totals-table" cellpadding="0" cellspacing="0">
                    <tr>
                        <td colspan="4" style="text-align: right; color: #475569;">উপমোট (Subtotal):</td>
                        <td style="text-align: right; width: 120px;"><strong>${BengaliFormatters.toBanglaCurrency(memo.subtotal)}</strong></td>
                    </tr>
                    $discountRow
                    $vatRow
                    <tr class="grand-total-row">
                        <td colspan="4" style="text-align: right; color: #1e3a8a;">সর্বমোট বিল (Grand Total):</td>
                        <td style="text-align: right; color: #1e3a8a; font-size: 16px;"><strong>${BengaliFormatters.toBanglaCurrency(memo.grandTotal)}</strong></td>
                    </tr>
                    <tr>
                        <td colspan="4" style="text-align: right; color: #047857;">জমা / পরিশোধ (${escapeHtml(memo.paymentMethod)}):</td>
                        <td style="text-align: right; color: #047857;"><strong>${BengaliFormatters.toBanglaCurrency(memo.paidAmount)}</strong></td>
                    </tr>
                    $dueRow
                </table>

                <!-- In Words -->
                <div class="in-words-box">
                    🗣️ কথায়: <strong>$inWords</strong>
                </div>

                ${if (!memo.notes.isNullOrBlank()) "<div style='font-size: 11px; color: #475569; margin-top: 6px;'><strong>নোট:</strong> ${escapeHtml(memo.notes)}</div>" else ""}
                <div style="font-size: 11px; color: #64748b; margin-top: 4px;"><strong>শর্তাবলী:</strong> বিক্রিত মাল ফেরত নেওয়া হয় না।</div>

                <!-- Signatures -->
                <table class="signatures" cellpadding="0" cellspacing="0">
                    <tr>
                        <td style="text-align: left;">
                            <div class="sig-line">ক্রেতার স্বাক্ষর</div>
                        </td>
                        <td style="text-align: right;">
                            <div class="sig-line">অনুমোদিত স্বাক্ষর / ক্যাশিয়ার</div>
                        </td>
                    </tr>
                </table>

                <!-- Footer -->
                <div class="footer-notice">
                    ✨ আমাদের সাথে কেনাকাটা করার জন্য ধন্যবাদ! | ডিজিটাল হিসাব ও মেমো
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
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

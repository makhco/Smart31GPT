package ir.vrhouse.icodeslix2

import android.app.Activity
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity(), NfcAdapter.ReaderCallback {
    private enum class Operation { PROGRAM_AND_LOCK, RECOVERY }

    private var nfcAdapter: NfcAdapter? = null
    private var selectedOperation: Operation = Operation.PROGRAM_AND_LOCK
    private var armedOperation: Operation? = null
    private val isRunning = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var titleView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var programTabButton: Button
    private lateinit var recoveryTabButton: Button
    private lateinit var armButton: Button
    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        buildUi()
        updateModeUi()
        if (nfcAdapter == null) {
            status("این دستگاه NFC ندارد.")
            armButton.isEnabled = false
        } else if (nfcAdapter?.isEnabled != true) {
            status("NFC خاموش است. ابتدا NFC گوشی را فعال کنید.")
        } else {
            status("آماده. یک تب را انتخاب کنید و دکمه اجرا را بزنید.")
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val operation = armedOperation ?: return
        if (!isRunning.compareAndSet(false, true)) return
        armedOperation = null
        setArmButtonEnabled(false)
        log("تگ شناسایی شد. شروع عملیات ...")

        try {
            Slix2NfcV.connect(tag) { log(it) }.use { session ->
                when (operation) {
                    Operation.PROGRAM_AND_LOCK -> TagWorkflows.programAndLock(session) { log(it) }
                    Operation.RECOVERY -> TagWorkflows.recoverPasswordsToDefault(session) { log(it) }
                }
            }
            status("✅ عملیات با موفقیت تمام شد.")
        } catch (t: Throwable) {
            status("❌ خطا در عملیات.")
            log("❌ ${t.javaClass.simpleName}: ${t.message ?: "خطای نامشخص"}")
            log("نکته: اگر Present Password اشتباه باشد، SLIX2 ممکن است تا برداشتن و نزدیک‌کردن مجدد تگ، دستورهای بعدی را اجرا نکند.")
        } finally {
            isRunning.set(false)
            setArmButtonEnabled(true)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(12))
            setBackgroundColor(Color.rgb(250, 250, 250))
        }

        titleView = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.rgb(30, 30, 30))
            gravity = Gravity.CENTER_VERTICAL
            text = "ICODE SLIX2 Programmer"
        }
        root.addView(titleView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val warning = TextView(this).apply {
            text = "فقط روی تگ‌های مجاز خودتان اجرا کنید. مرحله قفل نهایی قابل برگشت طراحی نشده است."
            textSize = 14f
            setTextColor(Color.rgb(120, 75, 0))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(255, 244, 214))
        }
        root.addView(warning, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(8))
        }
        programTabButton = Button(this).apply {
            text = "برنامه‌ریزی و قفل"
            setOnClickListener {
                selectedOperation = Operation.PROGRAM_AND_LOCK
                armedOperation = null
                updateModeUi()
            }
        }
        recoveryTabButton = Button(this).apply {
            text = "ریکاوری کلیدها"
            setOnClickListener {
                selectedOperation = Operation.RECOVERY
                armedOperation = null
                updateModeUi()
            }
        }
        tabRow.addView(programTabButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tabRow.addView(recoveryTabButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(tabRow, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        descriptionView = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.rgb(55, 55, 55))
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(descriptionView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        armButton = Button(this).apply {
            textSize = 16f
            setOnClickListener {
                if (nfcAdapter?.isEnabled != true) {
                    status("NFC خاموش است. ابتدا NFC گوشی را فعال کنید.")
                    return@setOnClickListener
                }
                armedOperation = selectedOperation
                clearLog()
                status("گوشی را به تگ ICODE SLIX2 نزدیک کنید ...")
                log("حالت آماده‌باش فعال شد: ${operationTitle(selectedOperation)}")
            }
        }
        root.addView(armButton, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val clearButton = Button(this).apply {
            text = "پاک کردن لاگ"
            setOnClickListener { clearLog() }
        }
        root.addView(clearButton, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        statusView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(40, 90, 40))
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(statusView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(30, 30, 30))
        }
        logView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(dp(10), dp(10), dp(10), dp(10))
            text = ""
        }
        scrollView.addView(
            logView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
    }

    private fun updateModeUi() {
        when (selectedOperation) {
            Operation.PROGRAM_AND_LOCK -> {
                descriptionView.text = "این تب فقط بلاک‌های 00 تا 04 و 48 تا 63 را می‌نویسد، همان بلاک‌ها را verify می‌کند، سپس Read/Write Password را تغییر می‌دهد و در پایان access condition، password protect و DSFID را می‌نویسد."
                armButton.text = "اسکن و اجرای برنامه‌ریزی کامل"
                programTabButton.isEnabled = false
                recoveryTabButton.isEnabled = true
            }
            Operation.RECOVERY -> {
                descriptionView.text = "این تب مخصوص تگ‌هایی است که داده‌ها و کلیدهای جدید روی آن‌ها نوشته شده، اما فیوزبیت‌ها هنوز ست نشده‌اند. با کلیدهای جدید احراز هویت می‌کند و Read/Write Password را به 00000000 برمی‌گرداند."
                armButton.text = "اسکن و ریکاوری کلیدها"
                programTabButton.isEnabled = true
                recoveryTabButton.isEnabled = false
            }
        }
        status("حالت انتخاب‌شده: ${operationTitle(selectedOperation)}")
    }

    private fun operationTitle(operation: Operation): String = when (operation) {
        Operation.PROGRAM_AND_LOCK -> "برنامه‌ریزی و قفل"
        Operation.RECOVERY -> "ریکاوری کلیدها"
    }

    private fun clearLog() {
        mainHandler.post { logView.text = "" }
    }

    private fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        mainHandler.post {
            logView.append("[$time] $message\n")
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun status(message: String) {
        mainHandler.post { statusView.text = message }
    }

    private fun setArmButtonEnabled(enabled: Boolean) {
        mainHandler.post { armButton.isEnabled = enabled }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

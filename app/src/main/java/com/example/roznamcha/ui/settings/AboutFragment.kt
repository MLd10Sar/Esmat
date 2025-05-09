package com.example.roznamcha.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // <<< ADD THIS IMPORT
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.roznamcha.R
import com.example.roznamcha.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "درباره برنامه"

        // Set the version name dynamically
        setAppVersion()

        // Set the formatted description text from strings.xml
        setFormattedDescription()

        // Set click listener for WhatsApp support
        binding.tvSupportWhatsapp.setOnClickListener {
            openWhatsAppSupport()
        }

        // Add any other listeners here, e.g., for promotional cards
        // binding.cardInsighdeed.setOnClickListener { ... }
    }

    private fun setAppVersion() {
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val version = pInfo.versionName
            binding.tvAppVersion.text = "Version $version"
        } catch (e: Exception) {
            Log.e("AboutFragment", "Could not get package info", e)
            binding.tvAppVersion.visibility = View.GONE
        }
    }

    private fun setFormattedDescription() {
        val htmlFormattedString = getString(R.string.app_description_long)
        val styledText = HtmlCompat.fromHtml(htmlFormattedString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.tvAppDescription.text = styledText
    }

    /**
     * Shows a dialog to the user, then opens WhatsApp with a pre-filled or blank message.
     */
    private fun openWhatsAppSupport() {
        val whatsappNumber = "+93785444401" // Your WhatsApp number
        val context = this.context ?: return

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) { "N/A" }

        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        val messageTemplate = """
        *سلام تیم روزنامچه!* 👋
        من در مورد برنامه سوال داشتم و ضرورت به کمک دارم.

        *معلومات دستگاه من برای کمک بهتر شما:*
        - *نسخه برنامه:* `$appVersion`
        - *نسخه اندروید:* `$androidVersion`
        - *مودل موبایل:* `$deviceModel`
        --------------------
        
        *سوال من:*
        
        """.trimIndent()

        // Now that AlertDialog is imported, this will work correctly.
        AlertDialog.Builder(context)
            .setTitle("پشتیبانی از طریق واتساپ")
            .setMessage("میخواهید یک پیام نمونه با معلومات دستگاه تان آماده شود تا ما بهتر شما را کمک کنیم؟")
            .setPositiveButton("بلی، پیام نمونه") { _, _ ->
                launchWhatsApp(whatsappNumber, messageTemplate)
            }
            .setNeutralButton("نخیر، پیام خالی") { _, _ ->
                launchWhatsApp(whatsappNumber, "")
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    /**
     * Helper function to launch the WhatsApp intent.
     */
    private fun launchWhatsApp(number: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val url = "https://api.whatsapp.com/send?phone=$number&text=${Uri.encode(message)}"
                data = Uri.parse(url)
                setPackage("com.whatsapp")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "واتساپ در موبایل شما نصب نیست.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
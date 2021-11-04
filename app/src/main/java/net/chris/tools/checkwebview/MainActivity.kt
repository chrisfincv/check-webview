package net.chris.tools.checkwebview

import android.content.pm.PackageInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.webkit.WebViewCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

  private lateinit var disposable: Disposable

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val loadingView = findViewById<View>(R.id.loading)
    val button = findViewById<AppCompatButton>(R.id.check)
    val systemWebView = findViewById<TextView>(R.id.system_webview_value)
    val systemWebViewVersion = findViewById<TextView>(R.id.system_webview_version)
    val checkAndroidWebView = findViewById<CheckBox>(R.id.android_webview)
    val checkGoogleWebView = findViewById<CheckBox>(R.id.google_webview)
    val checkOtherWebView = findViewById<CheckBox>(R.id.other_webview_label)
    val otherWebViews = findViewById<RecyclerView>(R.id.other_webviews).also {
      it.layoutManager = LinearLayoutManager(this)
    }

    button.setOnClickListener {
      dispose()
      disposable = Single.zip(
        Single.fromCallable { getWebViewRelevantPackageInfos() },
        Single.fromCallable { WebViewCompat.getCurrentWebViewPackage(this)?.packageName },
        Single.fromCallable { WebSettings.getDefaultUserAgent(this) }
          .map {
            it.split(" ")
              .find { it.contains("chrome", true) }
              ?.split('/')
              ?.getOrNull(1)
          },
        { list, packageName, userAgent ->
          WebViewInfor(
            packageInforList = list,
            systemWebViewPackageName = packageName,
            userAgent = userAgent
          )
        }
      )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
          loadingView.visibility = VISIBLE
          systemWebView.text = null
          checkAndroidWebView.isChecked = false
          checkGoogleWebView.isChecked = false
          checkOtherWebView.isChecked = false
        }
        .doOnTerminate {
          button.isEnabled = true
          loadingView.visibility = GONE
        }
        .subscribeBy(
          onSuccess = { (list, packageName, version) ->
            systemWebView.text = packageName
            systemWebViewVersion.text = version
            val otherPackages = mutableListOf<String>()
            for (packageInfo in list) {
              when (packageInfo.packageName) {
                ANDROID_WEBVIEW -> checkAndroidWebView.isChecked = true
                GOOGLE_WEBVIEW  -> checkGoogleWebView.isChecked = true
                else            -> otherPackages.add(packageInfo.packageName)
              }
            }
            checkOtherWebView.isChecked = otherPackages.isNotEmpty()
            updateOtherPackages(otherWebViews, otherPackages)
          },
          onError = {
            Log.e("get webview packages", "failed", it)
            Toast.makeText(this, "check webview packages failed", Toast.LENGTH_SHORT).show()
          }
        )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    dispose()
  }

  override fun onBackPressed() {
    super.onBackPressed()
    finish()
  }

  private fun dispose() {
    if (::disposable.isInitialized) {
      disposable.dispose()
    }
  }

  private fun updateOtherPackages(otherWebViews: RecyclerView, otherPackages: List<String>) {
    otherWebViews.adapter = object : RecyclerView.Adapter<ViewHolder>() {

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        OtherPackageViewHolder(
          LayoutInflater.from(parent.context).inflate(R.layout.item_other_package, parent, false)
        )

      override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.position)?.text = "${position + 1}"
        holder.itemView.findViewById<TextView>(R.id.content)?.text = otherPackages[position]
      }

      override fun getItemCount(): Int = otherPackages.size
    }
  }

  private fun getWebViewRelevantPackageInfos() = packageManager.getInstalledPackages(0)
    .filter { it.packageName.contains(".webview", ignoreCase = true) }

  companion object {
    private const val ANDROID_WEBVIEW = "com.android.webview"
    private const val GOOGLE_WEBVIEW = "com.google.android.webview"
  }

  private data class WebViewInfor(
    val packageInforList: List<PackageInfo>,
    val systemWebViewPackageName: String?,
    val userAgent: String?
  )
}

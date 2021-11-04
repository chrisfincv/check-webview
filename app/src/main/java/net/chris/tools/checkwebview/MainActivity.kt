package net.chris.tools.checkwebview

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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

    val checkAndroidWebView = findViewById<CheckBox>(R.id.android_webview)
    val checkGoogleWebView = findViewById<CheckBox>(R.id.google_webview)
    val loadingView = findViewById<View>(R.id.loading)

    findViewById<AppCompatButton>(R.id.check).setOnClickListener {
      if (::disposable.isInitialized) {
        disposable.dispose()
      }
      disposable = Single.fromCallable {
        packageManager.getInstalledPackages(0)
          .filter { it.packageName.contains(".webview", ignoreCase = true) }
      }
        .doOnSubscribe {
          loadingView.visibility = VISIBLE
          checkAndroidWebView.isChecked = false
          checkGoogleWebView.isChecked = false
        }
        .subscribeOn(Schedulers.trampoline())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnTerminate { loadingView.visibility = GONE }
        .subscribeBy(
          onSuccess = {
            for (packageInfo in it) {
              when (packageInfo.packageName) {
                ANDROID_WEBVIEW -> checkAndroidWebView.isChecked = true
                GOOGLE_WEBVIEW  -> checkGoogleWebView.isChecked = true
              }
            }
          },
          onError = {
            Toast.makeText(this, "check webview packages failed", Toast.LENGTH_SHORT).show()
          }
        )
    }
  }

  companion object {
    private const val ANDROID_WEBVIEW = "com.android.webview"
    private const val GOOGLE_WEBVIEW = "com.google.android.webview"
  }
}

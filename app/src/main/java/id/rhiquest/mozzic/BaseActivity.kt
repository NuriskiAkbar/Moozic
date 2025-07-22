package id.rhiquest.mozzic

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<viewBinding: ViewBinding> : AppCompatActivity() {
    protected lateinit var viewBinding: viewBinding

    abstract val inflateBinding: (LayoutInflater) -> viewBinding

    protected abstract fun getContext(): Context

    private val sharedPreferences by lazy {
        getContext().getSharedPreferences("spLogin", MODE_PRIVATE)
    }

    protected open val initToolbar: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = inflateBinding.invoke(layoutInflater)
        setContentView(viewBinding.root)
        initToolbar?.invoke()
        initView()
        setUpEdgeToEdge()
    }

    protected abstract fun initView()

    private fun setUpEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }
}
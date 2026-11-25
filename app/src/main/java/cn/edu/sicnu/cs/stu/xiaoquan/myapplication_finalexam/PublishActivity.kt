package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PublishActivity : BaseActivity() {

    private lateinit var cardCover: View
    private lateinit var ivCover: ImageView
    private lateinit var tvCoverHint: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSubmit: MaterialButton
    private val categories = Category.values().toList()

    /** 非 0 表示编辑已有商品，否则为发布新商品。 */
    private var editingId = 0
    private var editingGoods: Goods? = null

    /** 编辑模式下原有的封面相对路径（未改图时保持不变）。 */
    private var originalCoverPath: String? = null
    /** 当前选中的封面图文件（可能在 cacheDir，也可能是已存在的 filesDir 文件）。 */
    private var coverFile: File? = null
    /** 用户是否重新选过图。 */
    private var coverChanged = false

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) copyChosenCover(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        cardCover = findViewById(R.id.card_cover)
        ivCover = findViewById(R.id.iv_cover)
        tvCoverHint = findViewById(R.id.tv_cover_hint)
        etName = findViewById(R.id.et_goods_name)
        etPrice = findViewById(R.id.et_goods_price)
        etDesc = findViewById(R.id.et_goods_desc)
        spinnerCategory = findViewById(R.id.spinner_category)
        btnSubmit = findViewById(R.id.btn_submit)

        cardCover.setOnClickListener { pickCover.launch("image/*") }

        val labels = categories.map { getString(it.labelRes) }
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        btnSubmit.setOnClickListener { submit() }

        editingId = intent.getIntExtra(EXTRA_GOODS_ID, 0)
        if (editingId != 0) {
            toolbar.title = getString(R.string.edit_title)
            btnSubmit.setText(R.string.action_save)
            loadForEdit(editingId)
        }
    }

    private fun loadForEdit(goodsId: Int) {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { Repository.getGoodsById(goodsId) }
            if (data == null || data.sellerId != Session.uid || data.isSold) {
                toast(R.string.edit_failed)
                finish()
                return@launch
            }
            editingGoods = data
            originalCoverPath = data.coverLocalPath
            data.coverLocalPath?.takeIf { it.isNotEmpty() }?.let { rel ->
                val file = File(filesDir, rel)
                if (file.exists()) {
                    coverFile = file
                    showCover(file)
                }
            }
            etName.setText(data.name)
            etPrice.setText(formatPrice(data.price))
            etDesc.setText(data.desc)
            spinnerCategory.setSelection(categories.indexOf(data.category))
        }
    }

    private fun copyChosenCover(uri: Uri) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val dest = File(cacheDir, "editing_cover.jpg")
                try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { input.copyTo(it) }
                    } ?: return@withContext false
                    dest.exists() && dest.length() > 0
                } catch (e: Exception) {
                    false
                }
            }
            if (ok) {
                coverFile = File(cacheDir, "editing_cover.jpg")
                coverChanged = true
                showCover(coverFile!!)
            } else {
                toast(R.string.pick_cover_failed)
            }
        }
    }

    private fun showCover(file: File) {
        tvCoverHint.setText(R.string.action_change_cover)
        ImageLoader.loadFile(file, ivCover)
        ivCover.visibility = View.VISIBLE
    }

    /** 把当前封面固化到 filesDir/images，返回相对路径（或保留原路径 / null）。 */
    private suspend fun persistCover(): String? = withContext(Dispatchers.IO) {
        val appFilesDir = filesDir
        if (!coverChanged) return@withContext originalCoverPath
        val src = coverFile ?: return@withContext originalCoverPath
        val imagesDir = File(appFilesDir, "images").apply { mkdirs() }
        val dest = File(imagesDir, "goods_${System.currentTimeMillis()}.jpg")
        val copied = try {
            src.inputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
            dest.exists() && dest.length() > 0
        } catch (e: Exception) {
            false
        }
        if (!copied) return@withContext originalCoverPath
        // 覆盖新图后删除旧图
        originalCoverPath?.let { rel ->
            File(appFilesDir, rel).takeIf { it.exists() && it.absolutePath != dest.absolutePath }?.delete()
        }
        dest.relativeTo(appFilesDir).invariantSeparatorsPath
    }

    private fun submit() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val priceText = etPrice.text?.toString()?.trim().orEmpty()
        val desc = etDesc.text?.toString()?.trim().orEmpty()

        if (name.isEmpty() || priceText.isEmpty() || desc.isEmpty()) {
            toast(R.string.publish_invalid)
            return
        }
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            toast(R.string.publish_invalid_price)
            return
        }
        val category = categories[spinnerCategory.selectedItemPosition]

        lifecycleScope.launch {
            val coverPath = persistCover()
            if (editingId != 0) {
                val base = editingGoods ?: return@launch
                val updated = base.copy(
                    name = name,
                    price = price,
                    desc = desc,
                    category = category,
                    coverLocalPath = coverPath
                )
                val ok = withContext(Dispatchers.IO) {
                    Repository.updateGoods(updated, Session.uid)
                }
                toast(if (ok) R.string.edit_success else R.string.edit_failed)
                if (ok) finish()
                return@launch
            }

            val goods = Goods(
                id = 0,
                name = name,
                price = price,
                desc = desc,
                category = category,
                sellerId = Session.uid,
                sellerName = Session.username,
                coverLocalPath = coverPath
            )

            withContext(Dispatchers.IO) { Repository.publishGoods(goods) }
            toast(R.string.publish_success)
            finish()
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        /** 传入商品 id 表示编辑该商品，不传则为发布新商品。 */
        const val EXTRA_GOODS_ID = "editGoodsId"
    }
}

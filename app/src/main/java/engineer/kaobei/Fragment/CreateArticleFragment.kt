package engineer.kaobei.Fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import engineer.kaobei.Database.AuthStateManager
import engineer.kaobei.Database.FontManager
import engineer.kaobei.Database.ThemeManager
import engineer.kaobei.Model.Fonts.Font
import engineer.kaobei.Model.Themes.Theme
import engineer.kaobei.R
import engineer.kaobei.Util.ViewUtil.addGapController
import engineer.kaobei.View.AnimatedGap
import engineer.kaobei.View.KaobeiArticleViewer
import kotlinx.android.synthetic.main.fragment_create_article.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import kotlin.jvm.internal.Ref


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreateArticleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateArticleFragment : Fragment() {

    private var content = ""
    private lateinit var currentFont: Font
    private lateinit var currentTheme: Theme
    private lateinit var themeManager: ThemeManager
    private lateinit var fontManager: FontManager
    private lateinit var file: File
    private lateinit var imageUri: Uri
    private lateinit var img_ArticleImg: ImageView
    private lateinit var authStateManager: AuthStateManager

    lateinit var reInTop: Ref.BooleanRef

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_create_article, container,
            false
        )
        authStateManager = AuthStateManager.getInstance(view.context)
        themeManager = ThemeManager.getInstance(view.context)
        fontManager = FontManager.getInstance(view.context)
        currentTheme = themeManager.getThemes()[0]
        currentFont = fontManager.getFonts()[0]
        imageUri = Uri.EMPTY

        val kaobeiArticleViewer = view.findViewById<KaobeiArticleViewer>(R.id.viewer)
        kaobeiArticleViewer.setTextContent(content)
        kaobeiArticleViewer.setTheme(view.context, currentTheme)

        val btn_edit_content = view.findViewById<Button>(R.id.btn_edit_content)
        val tv_edit_content = view.findViewById<TextView>(R.id.tv_edit_content)
        btn_edit_content.setOnClickListener {
            val bt_sheet = BottomSheetDialog(view.context)
            val mView =
                LayoutInflater.from(view.context).inflate(R.layout.bottom_sheet_edit_content, null)
            val text_input = mView.findViewById<TextInputEditText>(R.id.text_input)
            text_input.setText(content)
            bt_sheet.setContentView(mView)
            bt_sheet.show()
            bt_sheet.setOnCancelListener {
                content = text_input.text.toString()
                kaobeiArticleViewer.setTextContent(content)
                tv_edit_content.text = content
            }
        }

        val btn_select_theme = view.findViewById<Button>(R.id.btn_select_theme)
        btn_select_theme.setOnClickListener {
            val bt_sheet = BottomSheetDialog(view.context)
            val mView =
                LayoutInflater.from(view.context).inflate(R.layout.bottom_sheet_select_theme, null)
            val rv_theme = mView.findViewById<RecyclerView>(R.id.rv_theme)
            val mLayoutManager = LinearLayoutManager(mView.context)
            val adapter = SelectThemeAdapter(mView.context, themeManager.getThemes())
            val gap = mView.findViewById<AnimatedGap>(R.id.gap)
            reInTop = Ref.BooleanRef()
            reInTop.element = false
            addGapController(rv_theme, gap, reInTop)
            adapter.setOnItemClickListener(object : SelectThemeAdapter.OnItemClickListener {
                override fun onItemClick(t: Theme) {
                    currentTheme = t
                    kaobeiArticleViewer.setTheme(view.context, t)
                    tv_select_theme.setTextColor(Color.parseColor(t.textColor))
                    tv_select_theme.setText(t.name)
                    cardview_select_theme.setCardBackgroundColor(Color.parseColor(t.backgroundColor))
                    bt_sheet.dismiss()
                }
            })
            rv_theme.layoutManager = mLayoutManager
            rv_theme.adapter = adapter
            bt_sheet.setContentView(mView)
            bt_sheet.show()
        }

        val btn_select_font = view.findViewById<Button>(R.id.btn_select_font)
        val tv_select_font = view.findViewById<TextView>(R.id.tv_select_font)
        btn_select_font.setOnClickListener {
            val bt_sheet = BottomSheetDialog(view.context)
            val mView =
                LayoutInflater.from(view.context).inflate(R.layout.bottom_sheet_select_font, null)
            val rv_font = mView.findViewById<RecyclerView>(R.id.rv_font)
            val mLayoutManager = LinearLayoutManager(mView.context)
            val adapter = SelectFontAdapter(mView.context, fontManager.getFonts())
            val gap = mView.findViewById<AnimatedGap>(R.id.gap)
            reInTop = Ref.BooleanRef()
            reInTop.element = false
            addGapController(rv_font, gap, reInTop)
            adapter.setOnItemClickListener(object : SelectFontAdapter.OnItemClickListener {
                override fun onItemClick(t: Font) {
                    currentFont = t
                    tv_select_font.setText(t.name)
                    bt_sheet.dismiss()
                }
            })
            rv_font.layoutManager = mLayoutManager
            rv_font.adapter = adapter
            bt_sheet.setContentView(mView)
            bt_sheet.show()
        }

        img_ArticleImg = view.findViewById<ImageView>(R.id.img_ArticleImg)
        val btn_select_image = view.findViewById<Button>(R.id.btn_select_image)
        btn_select_image.setOnClickListener {
            checkPermission(view.context)
        }

        val cardview_submit = view.findViewById<CardView>(R.id.cardview_submit)
        cardview_submit.setOnClickListener {
            val bt_sheet = BottomSheetDialog(view.context)
            val mView = LayoutInflater.from(view.context)
                .inflate(R.layout.bottom_sheet_submit_article, null)
            val cardview_submit: CardView = mView.findViewById(R.id.cardview_submit)
            val cardview_cancel: CardView = mView.findViewById(R.id.cardview_cancel)
            cardview_submit.setOnClickListener {
                submitArticle(
                    content,
                    currentTheme,
                    currentFont,
                    imageUri,
                    authStateManager.getCurrent().accessToken
                )
                bt_sheet.dismiss()
            }
            cardview_cancel.setOnClickListener {
                bt_sheet.dismiss()
            }
            bt_sheet.setContentView(mView)
            bt_sheet.show()
        }

        return view
    }


    fun submitArticle(content: String, theme: Theme, font: Font, img: Uri, accessToken: String?) {
        if (accessToken == null) {
            Toast.makeText(
                context,
                "尚未登入",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("content", content)
            .addFormDataPart("themeStyle", theme.value)
            .addFormDataPart("fontStyle", font.value)
        if (img!=Uri.EMPTY) {
            val path = context?.let { getRealPathFromUri(it, img) }
            val mFile = File(path)
            val fileRequestBody = mFile.asRequestBody("image/jpeg".toMediaType())
            requestBody
                .addFormDataPart("avatar", "img", fileRequestBody)
        }

        val request: Request = Request.Builder()
            .url("https://kaobei.engineer/api/frontend/social/cards/api/publish")
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept","*/*")
            .post(requestBody.build())
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Looper.prepare()
                Toast.makeText(
                    context,
                    "失敗",
                    Toast.LENGTH_SHORT
                ).show()
                Looper.loop()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.code != 200) {
                    return
                }
                Looper.prepare()
                Toast.makeText(
                    context,
                    "還真的發送成功了ㄚ",
                    Toast.LENGTH_SHORT
                ).show()
                Looper.loop()
            }
        })

    }

    fun checkPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED
            ) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                //show popup to request runtime permission
                requestPermissions(permissions, PERMISSION_CODE);
            } else {
                //permission already granted
                pickImageFromGallery();
            }
        } else {
            //system OS is < Marshmallow
            pickImageFromGallery();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            if (data != null) {
                imageUri = data.data
                file = File(imageUri.toString())
                img_ArticleImg.setImageURI(imageUri)
            }
        }
    }

    fun getRealPathFromUri(
        context: Context,
        contentUri: Uri
    ): String? {
        var cursor: Cursor? = null
        return try {
            val proj =
                arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } finally {
            cursor?.close()
        }
    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;

        //Permission code
        private val PERMISSION_CODE = 1001;

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment CreateArticleFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateArticleFragment().apply {

            }
    }
}

class SelectThemeAdapter(private val context: Context, private val themes: List<Theme>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mOnItemClickListener: OnItemClickListener

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardview_theme = itemView.findViewById<CardView>(R.id.cardview_theme)
        private val tv_theme = itemView.findViewById<TextView>(R.id.tv_theme)

        fun bind(theme: Theme) {
            cardview_theme.setCardBackgroundColor(Color.parseColor(theme.backgroundColor))
            tv_theme.setTextColor(Color.parseColor(theme.textColor))
            tv_theme.setText(theme.name)
            itemView.setOnClickListener {
                mOnItemClickListener.onItemClick(theme)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cardview_style_theme, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return this.themes.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(themes[position])
        }
    }

    fun setOnItemClickListener(mOnItemClickListener: OnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(theme: Theme)
    }
}

class SelectFontAdapter(private val context: Context, private val fonts: List<Font>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mOnItemClickListener: OnItemClickListener


    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tv_font = itemView.findViewById<TextView>(R.id.tv_font)

        fun bind(font: Font) {
            tv_font.setText(font.name)
            itemView.setOnClickListener {
                mOnItemClickListener.onItemClick(font)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.cardview_style_font, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return this.fonts.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(fonts[position])
        }
    }

    fun setOnItemClickListener(mOnItemClickListener: OnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(font: Font)
    }

}
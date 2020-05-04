package engineer.kaobei.Viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.squareup.okhttp.Callback
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import engineer.kaobei.Model.Article.Article
import engineer.kaobei.Model.Article.KaobeiArticleList
import java.io.IOException

/**
 * A ViewModel used for the {@link ArticleListFragment}.
 */
class ArticleListViewModel : ViewModel() {

    private lateinit var mOnReceiveDataListener : OnReceiveDataListener;
    private var page = 1
    private val  mArticles = ArrayList<Article>()
    private val mArticlesLiveData: MutableLiveData<ArrayList<Article>> by lazy {
        MutableLiveData<ArrayList<Article>>().also {
            loadArticles()
        }
    }

    fun getArticles(): LiveData<ArrayList<Article>> {
        return mArticlesLiveData
    }

    fun loadArticles() {
        // Do an asynchronous operation to fetch articles.
        loadMoreArticles(page++)
    }

    private fun loadMoreArticles(page: Int) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://kaobei.engineer/api/frontend/social/cards?page="+ page)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException?) {

            }

            override fun onResponse(response: Response?) {
                val responseData = response?.body()?.string()
                val bean = Gson().fromJson(responseData, KaobeiArticleList::class.javaObjectType)
                mOnReceiveDataListener.onReceiveData(bean.data)
                addArticles(bean.data)
            }
        })
    }

    fun addArticles(articles: List<Article>) {
        mArticles.addAll(articles)
        mArticlesLiveData.postValue(mArticles)
    }

    fun addArticle(article: Article) {
        mArticles.add(article)
        mArticlesLiveData.postValue(mArticles)
    }

    fun addArticleAt(index : Int,article:Article) {
        mArticles.add(index,article)
        mArticlesLiveData.postValue(mArticles)
    }

    fun removeAt(index : Int) {
        mArticles.removeAt(index)
        mArticlesLiveData.postValue(mArticles)
    }

    fun addOnReceiveDataListener(mOnReceiveDataListener: OnReceiveDataListener){
        this.mOnReceiveDataListener = mOnReceiveDataListener
    }

    interface OnReceiveDataListener {
        fun onReceiveData(list:List<Article>)
    }


}
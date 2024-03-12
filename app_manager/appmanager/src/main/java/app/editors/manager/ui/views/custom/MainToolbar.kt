package app.editors.manager.ui.views.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import app.documents.core.network.common.contracts.ApiContract
import app.documents.core.storage.account.CloudAccount
import app.editors.manager.R
import app.editors.manager.app.accountOnline
import app.editors.manager.managers.utils.GlideUtils
import app.editors.manager.managers.utils.ManagerUiUtils
import com.bumptech.glide.Glide
import lib.toolkit.base.managers.utils.AccountUtils


class MainToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Toolbar(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.include_toolbar, this)
    }

    val toolbar: Toolbar = findViewById(R.id.toolbar)

    private val accountContainer by lazy { findViewById<ConstraintLayout>(R.id.accountContainer) }
    private val arrowIcon by lazy { findViewById<ImageView>(R.id.toolbarArrowIcon) }
    private val toolbarIcon by lazy { findViewById<ImageView>(R.id.toolbarIcon) }
    private val title by lazy { findViewById<AppCompatTextView>(R.id.toolbarTitle) }
    private val subtitle by lazy { findViewById<AppCompatTextView>(R.id.toolbarSubTitle) }


    private val cloudAccount: CloudAccount? = context.accountOnline

    var accountListener: ((view: View) -> Unit)? = null
        set(value) {
            field = value
            accountContainer.setOnClickListener { value?.invoke(it) }
        }

    fun showAccount(isShow: Boolean) {
        accountContainer.isVisible = isShow
        arrowIcon.isVisible = isShow
    }

    fun bind() {
        cloudAccount?.let {
            title.text = cloudAccount.name
            subtitle.text = cloudAccount.portal
            if (cloudAccount.isWebDav) {
                setWebDavAvatar(cloudAccount.webDavProvider ?: "")
            } else if (cloudAccount.isOneDrive) {
                setOneDriveAvatar()
            } else if(cloudAccount.isDropbox) {
                if(it.avatarUrl == null || it.avatarUrl?.isEmpty() == true) {
                    setDropboxAvatar()
                } else {
                    loadAvatar(it)
                }
            } else {
                loadAvatar(it)
            }
        } ?: run {
            showAccount(false)
        }
    }

    private fun loadAvatar(cloudAccount: CloudAccount) {
        AccountUtils.getToken(
            context,
            cloudAccount.getAccountName()
        )?.let {
            val url = if (
                cloudAccount.avatarUrl?.contains(ApiContract.SCHEME_HTTP) == true ||
                cloudAccount.avatarUrl?.contains(ApiContract.SCHEME_HTTPS) == true ||
                cloudAccount.isDropbox ||
                cloudAccount.isGoogleDrive) {
                cloudAccount.avatarUrl
            } else {
                cloudAccount.scheme + cloudAccount.portal + cloudAccount.avatarUrl
            }
            Glide.with(context)
                .load(GlideUtils.getCorrectLoad(url ?: "", it))
                .apply(GlideUtils.avatarOptions)
                .into(toolbarIcon)
        } ?: run {
            Glide.with(context).load(R.drawable.ic_account_placeholder)
                .into(toolbarIcon)
        }
    }

    private fun setOneDriveAvatar() {
        toolbarIcon.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_storage_onedrive
            )
        )
    }

    private fun setDropboxAvatar() {
        toolbarIcon.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_storage_dropbox
            )
        )
    }

    private fun setWebDavAvatar(provider: String) {
        ManagerUiUtils.setWebDavImage(provider, toolbarIcon)
    }

}
package app.editors.manager.managers.utils

import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import app.documents.core.network.ApiContract
import app.documents.core.webdav.WebDavApi
import app.editors.manager.R
import lib.toolkit.base.managers.utils.StringUtils
import lib.toolkit.base.managers.utils.UiUtils

object UiUtils {

    @JvmStatic
    fun setWebDavImage(providerName: String?, image: ImageView) {
        when (WebDavApi.Providers.valueOf(providerName ?: "")) {
            WebDavApi.Providers.NextCloud -> image.setImageDrawable(
                ContextCompat.getDrawable(
                    image.context,
                    R.drawable.ic_storage_nextcloud
                )
            )
            WebDavApi.Providers.OwnCloud -> image.setImageDrawable(
                ContextCompat.getDrawable(
                    image.context,
                    R.drawable.ic_storage_owncloud
                )
            )
            WebDavApi.Providers.Yandex -> image.setImageDrawable(
                ContextCompat.getDrawable(
                    image.context,
                    R.drawable.ic_storage_yandex
                )
            )
            WebDavApi.Providers.KDrive -> image.setImageDrawable(
                ContextCompat.getDrawable(
                    image.context,
                    R.drawable.ic_storage_kdrive
                )
            )
            WebDavApi.Providers.WebDav -> image.setImageDrawable(
                ContextCompat.getDrawable(
                    image.context,
                    R.drawable.ic_storage_webdav
                )
            )
        }
    }

    @JvmStatic
    fun setFileIcon(view: AppCompatImageView, ext: String) {
        val extension = StringUtils.getExtension(ext)
        @DrawableRes var resId = R.drawable.ic_type_file
        @ColorRes var colorId = R.color.colorGrey
        when (extension) {
            StringUtils.Extension.DOC -> {
                resId = R.drawable.ic_type_text_document
                colorId = R.color.colorDocTint
            }
            StringUtils.Extension.SHEET -> {
                resId = R.drawable.ic_type_spreadsheet
                colorId = R.color.colorSheetTint
            }
            StringUtils.Extension.PRESENTATION -> {
                resId = R.drawable.ic_type_presentation
                colorId = R.color.colorPresentationTint
            }
            StringUtils.Extension.IMAGE, StringUtils.Extension.IMAGE_GIF -> {
                resId = R.drawable.ic_type_image
                colorId = R.color.colorPicTint
            }
            StringUtils.Extension.HTML, StringUtils.Extension.EBOOK, StringUtils.Extension.PDF -> {
                resId = R.drawable.ic_type_pdf
                colorId = R.color.colorLightRed
            }
            StringUtils.Extension.VIDEO_SUPPORT -> {
                resId = R.drawable.ic_type_video
                colorId = R.color.colorBlack
            }
            StringUtils.Extension.VIDEO -> {
                setAlphaIcon(view, R.drawable.ic_type_video)
                return
            }
            StringUtils.Extension.ARCH -> {
                setAlphaIcon(view, R.drawable.ic_type_archive)
                return
            }
            StringUtils.Extension.UNKNOWN -> {
                setAlphaIcon(view, R.drawable.ic_type_file)
                return
            }
        }
        view.setImageResource(resId)
        view.alpha = 1.0f
        view.setColorFilter(ContextCompat.getColor(view.context, colorId))
    }

    private fun setAlphaIcon(view: AppCompatImageView, @DrawableRes resId: Int) {
        view.setImageResource(resId)
        view.alpha = UiUtils.getFloatResource(view.context, R.dimen.alpha_medium)
        view.clearColorFilter()
    }


    fun setAccessIcon(imageView: ImageView, accessCode: Int) {
        when (accessCode) {
            ApiContract.ShareCode.NONE, ApiContract.ShareCode.RESTRICT -> {
                imageView.setImageResource(R.drawable.ic_access_deny)
                return
            }
            ApiContract.ShareCode.REVIEW -> imageView.setImageResource(R.drawable.ic_access_review)
            ApiContract.ShareCode.READ -> imageView.setImageResource(R.drawable.ic_access_read)
            ApiContract.ShareCode.READ_WRITE -> imageView.setImageResource(R.drawable.ic_access_full)
            ApiContract.ShareCode.COMMENT -> imageView.setImageResource(R.drawable.ic_access_comment)
            ApiContract.ShareCode.FILL_FORMS -> imageView.setImageResource(R.drawable.ic_access_fill_form)
        }
    }

    fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val layoutParams = this.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.leftMargin = left
        layoutParams.topMargin = top
        layoutParams.rightMargin = right
        layoutParams.bottomMargin = bottom
        this.layoutParams = layoutParams
    }


}
package app.editors.manager.ui.fragments.main

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import app.documents.core.network.ApiContract
import app.editors.manager.R
import app.editors.manager.app.App
import app.editors.manager.managers.tools.PreferenceTool
import app.editors.manager.mvp.models.base.Entity
import app.editors.manager.mvp.models.explorer.Explorer
import app.editors.manager.mvp.models.explorer.Item
import app.editors.manager.mvp.presenters.main.DocsBasePresenter
import app.editors.manager.mvp.presenters.main.DocsOnDevicePresenter
import app.editors.manager.mvp.views.main.DocsBaseView
import app.editors.manager.mvp.views.main.DocsOnDeviceView
import app.editors.manager.ui.activities.main.ActionButtonFragment
import app.editors.manager.ui.activities.main.IMainActivity
import app.editors.manager.ui.activities.main.MediaActivity
import app.editors.manager.ui.dialogs.ActionBottomDialog
import app.editors.manager.ui.dialogs.ContextBottomDialog
import app.editors.manager.ui.views.custom.PlaceholderViews
import lib.toolkit.base.managers.tools.LocalContentTools
import lib.toolkit.base.managers.utils.ActivitiesUtils
import lib.toolkit.base.managers.utils.UiUtils
import lib.toolkit.base.ui.activities.base.BaseActivity
import lib.toolkit.base.ui.dialogs.common.CommonDialog.Dialogs
import moxy.presenter.InjectPresenter
import java.util.*

class DocsOnDeviceFragment : DocsBaseFragment(), DocsOnDeviceView, ActionButtonFragment {

    companion object {
        val TAG: String = DocsOnDeviceFragment::class.java.simpleName

        private const val TAG_STORAGE_ACCESS = "TAG_STORAGE_ACCESS"

        fun newInstance(): DocsOnDeviceFragment {
            return DocsOnDeviceFragment()
        }
    }

    @InjectPresenter
    lateinit var presenter: DocsOnDevicePresenter
    private var activity: IMainActivity? = null
    private var operation: Operation? = null

    internal enum class Operation {
        COPY, MOVE
    }

    private var preferenceTool: PreferenceTool? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            activity = context as IMainActivity
            preferenceTool = App.getApp().appComponent.preference
        } catch (e: ClassCastException) {
            throw RuntimeException(
                DocsOnDeviceFragment::class.java.simpleName + " - must implement - " +
                        IMainActivity::class.java.simpleName
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                BaseActivity.REQUEST_ACTIVITY_CAMERA -> {
                    presenter.refresh()
                }
                BaseActivity.REQUEST_SELECT_FOLDER -> {
                    if (operation != null && data != null && data.data != null) {
                        if (operation == Operation.MOVE) {
                            presenter.moveFile(data.data, false)
                        } else if (operation == Operation.COPY) {
                            presenter.moveFile(data.data, true)
                        }
                    }
                }
                REQUEST_OPEN_FILE -> {
                    data?.data?.let {
                        presenter.openFromChooser(it)
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            when (requestCode) {
                BaseActivity.REQUEST_ACTIVITY_CAMERA -> presenter.deletePhoto()
                REQUEST_STORAGE_ACCESS -> {
                    preferenceTool?.isShowStorageAccess = false
                    presenter.recreateStack()
                    presenter.getItemsById(LocalContentTools.getDir(requireContext()))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoto()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkStorage()
        init()
    }

    override fun onStateMenuDefault(sortBy: String, isAsc: Boolean) {
        super.onStateMenuDefault(sortBy, isAsc)
        mMenu?.let {
            mOpenItem.isVisible = true
            it.findItem(R.id.toolbar_sort_item_owner).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_item_search, R.id.toolbar_item_sort -> item.isChecked = true
            R.id.toolbar_sort_item_date_update -> {
                presenter.sortBy(ApiContract.Parameters.VAL_SORT_BY_UPDATED, item.isChecked)
                item.isChecked = true
            }
            R.id.toolbar_sort_item_title -> {
                presenter.sortBy(ApiContract.Parameters.VAL_SORT_BY_TITLE, item.isChecked)
                item.isChecked = true
            }
            R.id.toolbar_sort_item_type -> {
                presenter.sortBy(ApiContract.Parameters.VAL_SORT_BY_TYPE, item.isChecked)
                item.isChecked = true
            }
            R.id.toolbar_sort_item_size -> {
                presenter.sortBy(ApiContract.Parameters.VAL_SORT_BY_SIZE, item.isChecked)
                item.isChecked = true
            }
            R.id.toolbar_sort_item_asc -> {
                presenter.orderBy(ApiContract.Parameters.VAL_SORT_ORDER_ASC)
                item.isChecked = true
            }
            R.id.toolbar_sort_item_desc -> {
                presenter.orderBy(ApiContract.Parameters.VAL_SORT_ORDER_DESC)
                item.isChecked = true
            }
            R.id.toolbar_main_item_select -> presenter.setSelection(true)
            R.id.toolbar_main_item_select_all -> presenter.setSelectionAll()
            R.id.toolbar_selection_delete -> presenter.delete()
            R.id.toolbar_selection_move -> {
                operation = Operation.MOVE
                presenter.checkSelectedFiles()
            }
            R.id.toolbar_selection_copy -> {
                operation = Operation.COPY
                presenter.checkSelectedFiles()
            }
            R.id.toolbar_selection_deselect -> presenter.deselectAll()
            R.id.toolbar_selection_select_all -> presenter.selectAll()
            R.id.toolbar_item_open -> showSingleFragmentFilePicker()
        }
        return true
    }

    override fun onSwipeRefresh(): Boolean {
        if (!super.onSwipeRefresh()) {
            presenter.getItemsById(LocalContentTools.getDir(requireContext()))
            return true
        }
        return false
    }

    override fun onStateUpdateRoot(isRoot: Boolean) {
        activity?.apply {
            setAppBarStates(false)
            showNavigationButton(!isRoot)
            showAccount(false)
        }
    }

    override fun onStateMenuSelection() {
        if (mMenu != null && mMenuInflater != null && context != null) {
            mMenuInflater?.inflate(R.menu.docs_select, mMenu)
            mDeleteItem = mMenu?.findItem(R.id.toolbar_selection_delete)?.setVisible(true)
            mMoveItem = mMenu?.findItem(R.id.toolbar_selection_move)?.setVisible(true)
            mCopyItem = mMenu?.findItem(R.id.toolbar_selection_copy)?.setVisible(true)
            mDownloadItem = mMenu?.findItem(R.id.toolbar_selection_download)?.setVisible(false)
            UiUtils.setMenuItemTint(requireContext(), mDeleteItem, R.color.colorWhite)
            activity?.showNavigationButton(true)
        }
    }

    override fun onStateEmptyBackStack() {
        super.onStateEmptyBackStack()
        if (mSwipeRefresh != null) {
            mSwipeRefresh.isRefreshing = true
        }
        presenter.getItemsById(LocalContentTools.getDir(requireContext()))
    }

    override fun onStateUpdateFilter(isFilter: Boolean, value: String?) {
        super.onStateUpdateFilter(isFilter, value)
        if (isFilter) {
            activity?.showNavigationButton(true)
        }
    }

    override fun onListEnd() {
        // Stub to local
    }

    override fun onActionBarTitle(title: String) {
        setActionBarTitle(title)
    }

    override fun onRemoveItemFromFavorites() {}

    override fun onActionButtonClick(buttons: ActionBottomDialog.Buttons) {
        super.onActionButtonClick(buttons)
        if (buttons == ActionBottomDialog.Buttons.PHOTO) {
            if (checkCameraPermission()) {
                makePhoto()
            }
        }
    }

    override fun onAcceptClick(dialogs: Dialogs?, value: String?, tag: String?) {
        var string = value
        if (tag != null) {
            if (string != null) {
                string = string.trim { it <= ' ' }
            }
            when (tag) {
                TAG_STORAGE_ACCESS -> requestManage()
                DocsBasePresenter.TAG_DIALOG_BATCH_DELETE_SELECTED -> presenter.deleteItems()
                DocsBasePresenter.TAG_DIALOG_CONTEXT_RENAME -> if (string != null) {
                    presenter.rename(string)
                }
                DocsBasePresenter.TAG_DIALOG_ACTION_SHEET -> presenter.createDocs(
                    "$string." + ApiContract.Extension.XLSX.toLowerCase(
                        Locale.ROOT
                    )
                )
                DocsBasePresenter.TAG_DIALOG_ACTION_PRESENTATION -> presenter.createDocs(
                    "$string." + ApiContract.Extension.PPTX.toLowerCase(
                        Locale.ROOT
                    )
                )
                DocsBasePresenter.TAG_DIALOG_ACTION_DOC -> presenter.createDocs(
                    "$string." + ApiContract.Extension.DOCX.toLowerCase(
                        Locale.ROOT
                    )
                )
                DocsBasePresenter.TAG_DIALOG_ACTION_FOLDER -> if (string != null) {
                    presenter.createFolder(string)
                }
                DocsBasePresenter.TAG_DIALOG_DELETE_CONTEXT -> presenter.deleteFile()
            }
        }
        hideDialog()
    }

    override fun onCancelClick(dialogs: Dialogs?, tag: String?) {
        super.onCancelClick(dialogs, tag)
        if (tag != null && tag == TAG_STORAGE_ACCESS) {
            preferenceTool?.isShowStorageAccess = false
            presenter.recreateStack()
            presenter.getItemsById(LocalContentTools.getDir(requireContext()))
        }
    }

    override fun onContextButtonClick(buttons: ContextBottomDialog.Buttons) {
        when (buttons) {
            ContextBottomDialog.Buttons.DOWNLOAD -> presenter.upload()
            ContextBottomDialog.Buttons.DELETE -> presenter.showDeleteDialog()
            ContextBottomDialog.Buttons.COPY -> {
                operation = Operation.COPY
                showFolderChooser(BaseActivity.REQUEST_SELECT_FOLDER)
            }
            ContextBottomDialog.Buttons.MOVE -> {
                operation = Operation.MOVE
                showFolderChooser(BaseActivity.REQUEST_SELECT_FOLDER)
            }
            ContextBottomDialog.Buttons.RENAME -> showEditDialogRename(
                getString(R.string.dialogs_edit_rename_title), presenter.itemTitle,
                getString(R.string.dialogs_edit_hint), DocsBasePresenter.TAG_DIALOG_CONTEXT_RENAME,
                getString(R.string.dialogs_edit_accept_rename), getString(R.string.dialogs_common_cancel_button)
            )
        }
        mContextBottomDialog.dismiss()
    }

    override fun isActivePage(): Boolean {
        return isAdded
    }

    override fun onActionDialog() {
        mActionBottomDialog.setOnClickListener(this)
        mActionBottomDialog.setLocal(true)
        mActionBottomDialog.show(requireFragmentManager(), ActionBottomDialog.TAG)
    }

    override fun onRemoveItem(item: Item) {
        mExplorerAdapter.removeItem(item)
        mExplorerAdapter.checkHeaders()
    }

    override fun onRemoveItems(items: List<Item>) {
        mExplorerAdapter.removeItems(ArrayList<Entity>(items))
        mExplorerAdapter.checkHeaders()
    }

    override fun onShowFolderChooser() {
        showFolderChooser(BaseActivity.REQUEST_SELECT_FOLDER)
    }

    override fun onShowCamera(photoUri: Uri) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        this.startActivityForResult(intent, BaseActivity.REQUEST_ACTIVITY_CAMERA)
    }

    override fun onShowDocs(uri: Uri) {
        showEditors(uri, EditorsType.DOCS)
    }

    override fun onShowCells(uri: Uri) {
        showEditors(uri, EditorsType.CELLS)
    }

    override fun onShowSlides(uri: Uri) {
        showEditors(uri, EditorsType.PRESENTATION)
    }

    override fun onShowPdf(uri: Uri) {
        showEditors(uri, EditorsType.PDF)
    }

    override fun onOpenMedia(mediaFiles: Explorer) {
        MediaActivity.show(this, mediaFiles, false)
    }

    override fun isWebDav(): Boolean {
        return false
    }

    override fun getPresenter(): DocsBasePresenter<out DocsBaseView?> {
        return presenter
    }

    override fun setVisibilityActionButton(isShow: Boolean) {
        activity?.showActionButton(isShow)
    }

    private fun init() {
        presenter.checkBackStack()
    }

    private fun makePhoto() {
        presenter.createPhoto()
    }

    private fun showSingleFragmentFilePicker() {
        try {
            ActivitiesUtils.showSingleFilePicker(this, REQUEST_OPEN_FILE)
        } catch (e: ActivityNotFoundException) {
            onError(e.message)
        }
    }

    private fun checkStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager() &&
            preferenceTool?.isShowStorageAccess == true
        ) {

            //TODO удалить когда будет доступно разрешение
//            preferenceTool?.isShowStorageAccess = false
//            presenter.recreateStack()
//            presenter.getItemsById(LocalContentTools.getDir(requireContext()))

            //TODO раскоментировать когда будет доступно разрешение
            showQuestionDialog(getString(R.string.app_manage_files_title),
                    getString(R.string.app_manage_files_description),
                    getString(R.string.dialogs_common_ok_button),
                    getString(R.string.dialogs_common_cancel_button),
                    TAG_STORAGE_ACCESS);
        }
    }

    private fun requestManage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + requireContext().packageName)
                )
                startActivityForResult(intent, REQUEST_STORAGE_ACCESS)
            } catch (e: ActivityNotFoundException) {
                showSnackBar("Not found")
                mPlaceholderViews.setTemplatePlaceholder(PlaceholderViews.Type.ACCESS)
            }
        }
    }
}
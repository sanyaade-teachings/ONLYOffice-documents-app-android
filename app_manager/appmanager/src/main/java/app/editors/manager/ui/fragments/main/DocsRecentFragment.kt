package app.editors.manager.ui.fragments.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.View
import app.documents.core.account.Recent
import app.editors.manager.R
import app.editors.manager.mvp.models.explorer.CloudFile
import app.editors.manager.mvp.models.explorer.Explorer
import app.editors.manager.mvp.presenters.main.DocsRecentPresenter
import app.editors.manager.mvp.presenters.main.OpenState
import app.editors.manager.mvp.presenters.main.RecentState
import app.editors.manager.mvp.views.main.DocsRecentView
import app.editors.manager.ui.activities.main.IMainActivity
import app.editors.manager.ui.activities.main.MainActivity
import app.editors.manager.ui.activities.main.MediaActivity
import app.editors.manager.ui.activities.main.WebViewerActivity
import app.editors.manager.ui.adapters.RecentAdapter
import app.editors.manager.ui.adapters.holders.factory.RecentHolderFactory
import app.editors.manager.ui.dialogs.ContextBottomDialog
import app.editors.manager.ui.views.custom.PlaceholderViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lib.toolkit.base.managers.utils.StringUtils
import lib.toolkit.base.ui.activities.base.BaseActivity
import moxy.presenter.InjectPresenter

class DocsRecentFragment : DocsBaseFragment(), DocsRecentView {

    @InjectPresenter
    override lateinit var presenter: DocsRecentPresenter

    private var activity: IMainActivity? = null
    private var adapter: RecentAdapter? = null
    private var filterValue: CharSequence? = null

    private val recentListener: (recent: Recent, position: Int) -> Unit = { recent, position ->
        Debounce.perform(1000L) { presenter.fileClick(recent, position) }
    }

    private val contextListener: (recent: Recent, position: Int) -> Unit = { recent, position ->
        presenter.contextClick(recent, position)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_READ_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.getRecentFiles()
            } else {
                placeholderViews?.setTemplatePlaceholder(PlaceholderViews.Type.ACCESS)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = if (context is IMainActivity) {
            context
        } else {
            throw RuntimeException(
                DocsRecentFragment::class.java.simpleName + " - must implement - " +
                        MainActivity::class.java.simpleName
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.containsKey(KEY_FILTER) == true) {
            filterValue = savedInstanceState.getCharSequence(KEY_FILTER)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(KEY_FILTER, searchView?.query)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        setMenuSearchEnabled(true)
        mainItem?.isVisible = false
        sortItem?.isVisible = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BaseActivity.REQUEST_ACTIVITY_WEB_VIEWER -> presenter.getRecentFiles()
            REQUEST_DOCS, REQUEST_SHEETS, REQUEST_PRESENTATION, REQUEST_PDF ->
                if (resultCode == Activity.RESULT_CANCELED) {
                    presenter.deleteTempFile()
                } else if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        if (data.getBooleanExtra("EXTRA_IS_MODIFIED", false)) {
                            presenter.upload(it, null)
                        }
                    }
                }
        }
    }

    override fun onStateUpdateFilter(isFilter: Boolean, value: String?) {
        super.onStateUpdateFilter(isFilter, value)
        if (isFilter) {
            activity?.setAppBarStates(false)
            searchView?.setQuery(filterValue, true)
            filterValue = ""
        } else {
            activity?.setAppBarStates(false)
            activity?.showNavigationButton(false)
        }
    }

    private fun init() {
        activity?.let { activity ->
            activity.setAppBarStates(false)
            activity.showNavigationButton(false)
            activity.showActionButton(false)
            activity.showAccount(false)
        }
        adapter = RecentAdapter(requireContext(), RecentHolderFactory(recentListener, contextListener))
        recyclerView?.let {
            it.adapter = adapter
            it.setPadding(
                resources.getDimensionPixelSize(lib.toolkit.base.R.dimen.screen_left_right_padding),
                resources.getDimensionPixelSize(lib.toolkit.base.R.dimen.screen_top_bottom_padding),
                resources.getDimensionPixelSize(lib.toolkit.base.R.dimen.screen_left_right_padding),
                resources.getDimensionPixelSize(lib.toolkit.base.R.dimen.screen_bottom_padding)
            )
        }
        swipeRefreshLayout?.isEnabled = false
        if (checkReadPermission()) {
            presenter.getRecentFiles()
        }
        setActionBarTitle(getString(R.string.fragment_recent_title))
    }

    override fun onRecentGet(list: List<Recent>) {
        adapter?.setRecent(list)
    }

    override fun onListEnd() {
        presenter.loadMore(adapter?.itemCount)
    }

    override fun updateFiles(files: List<Recent>) {
        if (files.isNotEmpty()) {
            adapter?.itemsList?.let {
                adapter?.setRecent(files)
                recyclerView?.scrollToPosition(0)
            } ?: run {
                adapter?.setRecent(files)
            }
            placeholderViews?.setVisibility(false)
            updateMenu(true)
        } else {
            placeholderViews?.setTemplatePlaceholder(PlaceholderViews.Type.SEARCH)
            updateMenu(false)
        }
    }

    private fun updateMenu(isEnable: Boolean) {
        if (menu != null && searchItem != null && deleteItem != null) {
            searchItem?.isEnabled = isEnable
            deleteItem?.isVisible = isEnable
        }
    }

    override fun openFile(file: CloudFile) {
        val ext = file.fileExst
        if (StringUtils.isVideoSupport(ext) || StringUtils.isImage(ext)) {
            MediaActivity.show(this, getExplorer(file), false)
        } else if (StringUtils.isDocument(ext)) {
            WebViewerActivity.show(requireActivity(), file)
        } else {
            onError(getString(R.string.error_unsupported_format))
        }
    }

    private fun getExplorer(file: CloudFile): Explorer {
        return Explorer().apply {
            this.files = mutableListOf(file)
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        searchCloseButton?.isEnabled = newText.isNotEmpty()
        presenter.searchRecent(newText)
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onContextShow(state: ContextBottomDialog.State) {
        parentFragmentManager.let {
            contextBottomDialog?.state = state
            contextBottomDialog?.onClickListener = this
            contextBottomDialog?.show(it, ContextBottomDialog.TAG)
        }
    }

    override fun onDeleteItem(position: Int) {
        adapter?.let { recentAdapter ->
            recentAdapter.removeItem(position)
            if (recentAdapter.isEmpty()) setEmpty()
        }
    }

    override fun onContextButtonClick(buttons: ContextBottomDialog.Buttons?) {
        if (buttons == ContextBottomDialog.Buttons.DELETE) {
            presenter.deleteRecent()
        }
        contextBottomDialog?.dismiss()
    }

    override fun onRender(state: RecentState) {
        when (state) {
            is RecentState.RenderList -> {
                if (state.recents.isEmpty()) {
                    setEmpty()
                } else {
                    setRecents(state.recents)
                }
            }
        }
    }

    private fun setRecents(recents: List<Recent>) {
        setMenuVisibility(true)
        adapter?.setRecent(recents)
    }

    private fun setEmpty() {
        setMenuVisibility(false)
        placeholderViews?.setTemplatePlaceholder(PlaceholderViews.Type.EMPTY)
    }

    override fun onOpenFile(state: OpenState) {
        when (state) {
            is OpenState.Docs -> {
                showEditors(state.uri, EditorsType.DOCS)
            }
            is OpenState.Cells -> {
                showEditors(state.uri, EditorsType.CELLS)
            }
            is OpenState.Slide -> {
                showEditors(state.uri, EditorsType.PRESENTATION)
            }
            is OpenState.Pdf -> {
                showEditors(state.uri, EditorsType.PDF)
            }
            is OpenState.Media -> {
                MediaActivity.show(this, state.explorer, state.isWebDav)
            }
        }
    }

    override fun onUpdateItemFavorites() { }

    override val isWebDav: Boolean
        get() = false

    object Debounce {
        var isClickable = true

        fun perform(timeMillis: Long, func: () -> Unit) {
            if (isClickable) {
                CoroutineScope(Dispatchers.Main).launch {
                    func.invoke()
                    isClickable = false
                    delay(timeMillis)
                    isClickable = true
                }
            }
        }
    }

    companion object {
        var TAG: String = DocsRecentFragment::class.java.simpleName

        fun newInstance(): DocsRecentFragment {
            return DocsRecentFragment()
        }

        private const val KEY_FILTER = "KEY_FILTER"
    }

}
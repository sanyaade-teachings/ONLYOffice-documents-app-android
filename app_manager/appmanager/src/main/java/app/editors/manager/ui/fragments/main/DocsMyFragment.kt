package app.editors.manager.ui.fragments.main

import android.os.Bundle
import android.view.View
import app.editors.manager.managers.providers.CloudFileProvider

class DocsMyFragment : DocsCloudFragment() {

    companion object {
        val ID = CloudFileProvider.Section.My.path

        fun newInstance(stringAccount: String): DocsMyFragment {
            return DocsMyFragment().apply {
                arguments = Bundle(1).apply {
                    putString(KEY_ACCOUNT, stringAccount)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onSwipeRefresh(): Boolean {
        if (!super.onSwipeRefresh() && mCloudPresenter != null) {
            mCloudPresenter.getItemsById(ID)
            return true
        }
        return false
    }

    override fun onScrollPage() {
        super.onScrollPage()
        if (mCloudPresenter.stack == null) {
            mCloudPresenter.getItemsById(ID)
        }
    }

    override fun onStateEmptyBackStack() {
        super.onStateEmptyBackStack()
        if (mSwipeRefresh != null) {
            mSwipeRefresh.isRefreshing = true
        }
        mCloudPresenter.getItemsById(ID)
    }

    override fun onRemoveItemFromFavorites() {}
    private fun init() {
        mExplorerAdapter.isSectionMy = true
        mCloudPresenter.checkBackStack()
        getArgs()
    }


}
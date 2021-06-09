package app.editors.manager.mvp.presenters.main

import android.accounts.Account
import android.os.Bundle
import app.documents.core.account.CloudAccount
import app.documents.core.login.LoginResponse
import app.documents.core.network.ApiContract
import app.documents.core.network.models.login.Capabilities
import app.documents.core.network.models.login.response.ResponseCapabilities
import app.documents.core.network.models.login.response.ResponseSettings
import app.documents.core.network.models.login.response.ResponseUser
import app.documents.core.settings.NetworkSettings
import app.documents.core.webdav.WebDavApi
import app.editors.manager.R
import app.editors.manager.app.App
import app.editors.manager.mvp.presenters.login.BaseLoginPresenter
import app.editors.manager.mvp.views.main.CloudAccountView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.toolkit.base.managers.utils.AccountUtils
import moxy.InjectViewState
import okhttp3.Credentials
import retrofit2.HttpException
import java.util.*

sealed class CloudAccountState {
    class AccountLoadedState(val account: List<CloudAccount>, val state: Bundle?) : CloudAccountState()
}

data class RestoreSettingsModel(
    val portal: String,
    val scheme: String,
    val isSsl: Boolean,
    val isCipher: Boolean
) {
    companion object {
        fun getInstance(networkSettings: NetworkSettings): RestoreSettingsModel {
            return RestoreSettingsModel(
                networkSettings.getPortal(),
                networkSettings.getScheme(),
                networkSettings.getSslState(),
                networkSettings.getCipher()
            )
        }
    }
}

@InjectViewState
class CloudAccountPresenter : BaseLoginPresenter<CloudAccountView>() {

    companion object {
        val TAG: String = CloudAccountPresenter::class.java.simpleName
    }

    init {
        App.getApp().appComponent.inject(this)
    }

    var contextAccount: CloudAccount? = null
    private var restoreState: RestoreSettingsModel? = null
    private var disposable: Disposable? = null

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    fun getAccounts(saveState: Bundle? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            getTokens(accountDao.getAccounts(), saveState)
        }
    }

    private suspend fun getTokens(accounts: List<CloudAccount>, saveState: Bundle?) {
        val accountsWithToken = accounts.map { account ->
            AccountUtils.getToken(
                context,
                Account(account.getAccountName(), context.getString(R.string.account_type))
            )?.let { token ->
                account.token = token
                return@map account
            } ?: run {
                return@map account
            }
        }
        withContext(Dispatchers.Main) {
            viewState.onRender(CloudAccountState.AccountLoadedState(accountsWithToken, saveState))
        }
    }

    fun logOut() {
        CoroutineScope(Dispatchers.Default).launch {
            contextAccount?.let { account ->
                if (account.isWebDav) {
                    AccountUtils.setToken(context, account.getAccountName(), null)
                } else {
                    AccountUtils.setPassword(context, account.getAccountName(), null)
                }
                accountDao.updateAccount(account.copy(isOnline = false))
                accountDao.getAccounts().let {
                    withContext(Dispatchers.Main) {
                        viewState.onRender(CloudAccountState.AccountLoadedState(it, null))
                    }
                }
            }
        }
    }

    fun deleteAccount() {
        CoroutineScope(Dispatchers.Default).launch {
            contextAccount?.let { account ->
                if (AccountUtils.removeAccount(context, account.getAccountName())) {
                    accountDao.deleteAccount(account)
                    accountDao.getAccounts().let {
                        withContext(Dispatchers.Main) {
                            viewState.onRender(CloudAccountState.AccountLoadedState(it, null))
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        viewState.onError("Error delete")
                    }
                }
            }
        }
    }

    fun deleteSelected(selection: List<String>?) {
        CoroutineScope(Dispatchers.Default).launch {
            selection?.forEach {
                accountDao.getAccount(it)?.let { account ->
                    if (AccountUtils.removeAccount(context, account.getAccountName())) {
                        accountDao.deleteAccount(account)
                    }
                }
            }
            accountDao.getAccounts().let {
                withContext(Dispatchers.Main) {
                    viewState.onRender(CloudAccountState.AccountLoadedState(it, null))
                }
            }
        }
    }

    fun checkLogin(account: CloudAccount) {
        if (!account.isOnline) {
            if (account.isWebDav) {
                AccountUtils.getPassword(context, account.getAccountName())?.let { password ->
                    if (password.isNotEmpty()) {
                        webDavLogin(account, password)
                    } else {
                        viewState.onWebDavLogin(
                            Json.encodeToString(account),
                            WebDavApi.Providers.valueOf(account.webDavProvider ?: "")
                        )
                    }
                } ?: run {
                    viewState.onWebDavLogin(
                        Json.encodeToString(account),
                        WebDavApi.Providers.valueOf(account.webDavProvider ?: "")
                    )
                }
            } else {
                AccountUtils.getToken(context, account.getAccountName())?.let { token ->
                    if (token.isNotEmpty()) {
                        login(account, token)
                    } else {
                        viewState.onAccountLogin(account.portal ?: "", account.login ?: "")
                    }
                } ?: run {
                    networkSettings.setBaseUrl(account.portal ?: "")
                    disposable = App.getApp().loginComponent.loginService.capabilities().subscribe({response ->
                        if (response is LoginResponse.Success) {
                            if (response.response is ResponseCapabilities) {
                                val capability = (response.response as ResponseCapabilities).response
                                setSettings(capability)
                                viewState.onAccountLogin(account.portal ?: "", account.login ?: "")
                            } else {
                                networkSettings.serverVersion =
                                    (response.response as ResponseSettings).response.communityServer ?: ""
                            }
                        } else {
                            fetchError((response as LoginResponse.Error).error)
                        }
                    }) {throwable: Throwable -> checkError(throwable, account)}

                }
            }
        } else {
            viewState.onError("Account online")
        }
    }
    fun checkContextLogin() {
        contextAccount?.let {
            checkLogin(it)
        } ?: run {
            viewState.onError("Error login")
        }
    }

    private fun login(account: CloudAccount, token: String) {
        setSettings(account)
        disposable = App.getApp().loginComponent
            .loginService
            .getUserInfo(token)
            .map {
                when (it) {
                    is LoginResponse.Success -> return@map it.response as ResponseUser
                    is LoginResponse.Error -> throw throw it.error
                }
            }
            .subscribe({
                loginSuccess(account)
            }, {
                checkError(it, account)
            })
    }

    private fun webDavLogin(account: CloudAccount, password: String) {
        setSettings(account)
        disposable = App.getApp().getWebDavApi(account.login, password)
            .capabilities(Credentials.basic(account.login ?: "", password), account.webDavPath)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.code() == 207 && it.body() != null) {
                    loginSuccess(account)
                } else {
                    restoreSettings()
                    viewState.onWebDavLogin(
                        Json.encodeToString(account),
                        WebDavApi.Providers.valueOf(account.webDavProvider ?: "")
                    )
                }
            }, {
                checkError(it, account)
            })
    }

    private fun loginSuccess(account: CloudAccount) {
        CoroutineScope(Dispatchers.Default).launch {
            accountDao.getAccountOnline()?.let { onlineAccount ->
                accountDao.updateAccount(onlineAccount.copy(isOnline = false))
            }
            accountDao.updateAccount(account.copy(isOnline = true))
            withContext(Dispatchers.Main) {
                viewState.onSuccessLogin()
            }
        }
    }

    private fun setSettings(account: CloudAccount) {
        restoreState = RestoreSettingsModel.getInstance(networkSettings)
        networkSettings.setBaseUrl(account.portal ?: "")
        networkSettings.setScheme(account.scheme ?: "")
        networkSettings.setSslState(account.isSslState)
        networkSettings.setCipher(account.isSslCiphers)
    }

    private fun setSettings(capabilities: Capabilities) {
        networkSettings.ldap = capabilities.ldapEnabled
        networkSettings.ssoUrl = capabilities.ssoUrl
        networkSettings.ssoLabel = capabilities.ssoLabel
    }

    private fun restoreSettings() {
        restoreState?.let { state ->
            networkSettings.setBaseUrl(state.portal)
            networkSettings.setScheme(state.scheme)
            networkSettings.setSslState(state.isSsl)
            networkSettings.setCipher(state.isCipher)
        }
        restoreState = null
    }

    fun checkError(throwable: Throwable, account: CloudAccount) {
        restoreSettings()
        if (throwable is HttpException && throwable.code() == ApiContract.HttpCodes.CLIENT_UNAUTHORIZED) {
            viewState.onAccountLogin(account.portal ?: "", account.login ?: "")
        } else {
            fetchError(throwable)
        }
    }

}
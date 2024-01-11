package app.editors.manager.ui.fragments.share.link

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.documents.core.network.common.contracts.ApiContract
import app.documents.core.network.manager.models.explorer.CloudFolder
import app.documents.core.network.share.models.ExternalLink
import app.documents.core.network.share.models.ExternalLinkSharedTo
import app.documents.core.network.share.models.Share
import app.documents.core.network.share.models.ShareGroup
import app.documents.core.network.share.models.SharedTo
import app.editors.manager.R
import app.editors.manager.app.accountOnline
import app.editors.manager.app.roomProvider
import app.editors.manager.managers.utils.RoomUtils
import app.editors.manager.ui.activities.main.ShareActivity
import app.editors.manager.ui.dialogs.fragments.BaseDialogFragment
import app.editors.manager.viewModels.link.RoomInfoEffect
import app.editors.manager.viewModels.link.RoomInfoState
import app.editors.manager.viewModels.link.RoomInfoViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.compose.ui.addIf
import lib.compose.ui.theme.ManagerTheme
import lib.compose.ui.theme.colorTextSecondary
import lib.compose.ui.theme.colorTextTertiary
import lib.compose.ui.views.AnimatedVisibilityVerticalFade
import lib.compose.ui.views.AppDescriptionItem
import lib.compose.ui.views.AppDivider
import lib.compose.ui.views.AppHeaderItem
import lib.compose.ui.views.AppScaffold
import lib.compose.ui.views.AppTextButton
import lib.compose.ui.views.AppTopBar
import lib.compose.ui.views.TopAppBarAction
import lib.toolkit.base.managers.utils.KeyboardUtils
import lib.toolkit.base.managers.utils.UiUtils
import lib.toolkit.base.managers.utils.getSerializableExt
import lib.toolkit.base.managers.utils.openSendTextActivity
import lib.toolkit.base.managers.utils.putArgs

class RoomInfoFragment : BaseDialogFragment() {

    companion object {

        private const val KEY_ROOM = "key_room"
        private const val MAX_ADDITIONAL_LINKS_COUNT = 5
        val TAG: String = RoomInfoFragment::class.java.simpleName

        fun newInstance(room: CloudFolder): RoomInfoFragment =
            RoomInfoFragment().putArgs(KEY_ROOM to room)

    }

    enum class RoomInfoScreens {
        RoomInfo, UserAccess, LinkSettings
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ComponentDialog(
            requireContext(),
            if (!UiUtils.isTablet(requireContext())) R.style.FullScreenDialog else 0
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view as? ComposeView)?.setContent {
            ManagerTheme {
                val context = LocalContext.current
                val localView = LocalView.current
                val portal = remember { context.accountOnline?.portal }
                val room = remember { arguments?.getSerializableExt<CloudFolder>(KEY_ROOM) }
                val canEditRoom = room?.security?.editRoom == true
                val viewModel = viewModel { RoomInfoViewModel(requireContext().roomProvider, room?.id.orEmpty()) }
                val navController = rememberNavController().also {
                    it.addOnDestinationChangedListener { _, destination, _ ->
                        if (destination.route == RoomInfoScreens.RoomInfo.name) {
                            viewModel.fetchRoomInfo()
                        }
                    }
                }
                val state by viewModel.state.collectAsState()
                val shareActivityResult = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.fetchRoomInfo()
                }

                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is RoomInfoEffect.Error -> {
                                UiUtils.getShortSnackBar(requireView())
                                    .setText(effect.message)
                                    .show()
                            }
                            is RoomInfoEffect.Create -> {
                                KeyboardUtils.setDataToClipboard(context, effect.url)
                                UiUtils.getSnackBar(localView)
                                    .setText(R.string.rooms_info_copy_link_to_clipboard)
                                    .show()
                            }
                        }
                    }
                }

                Surface(color = MaterialTheme.colors.background) {
                    NavHost(navController = navController, startDestination = RoomInfoScreens.RoomInfo.name) {
                        composable(route = RoomInfoScreens.RoomInfo.name) {
                            RoomInfoScreen(
                                state = state,
                                canEditRoom = canEditRoom,
                                roomType = room?.roomType,
                                roomTitle = room?.title,
                                portal = portal,
                                onBackClick = this@RoomInfoFragment::onBackPressed,
                                onAddUsers = {
                                    ShareActivity.launchForResult(
                                        launcher = shareActivityResult,
                                        fragment = this@RoomInfoFragment,
                                        item = room,
                                        isInfo = false
                                    )
                                },
                                onSetUserAccess = { userId, access, ownerOrAdmin ->
                                    navController.navigate(
                                        RoomInfoScreens.UserAccess.name +
                                                "?userId=$userId" +
                                                "&access=$access" +
                                                "&ownerOrAdmin=$ownerOrAdmin"
                                    )
                                },
                                onGeneralLinkCreate = viewModel::createGeneralLink,
                                onAdditionalLinkCreate = {
                                    navController.navigate("${RoomInfoScreens.LinkSettings.name}?create=true")
                                },
                                onLinkClick = { link ->
                                    val json = Json.encodeToString(link.sharedTo)
                                    navController.navigate("${RoomInfoScreens.LinkSettings.name}?link=$json")
                                }
                            )
                        }
                        composable(
                            route = "${RoomInfoScreens.LinkSettings.name}?link={link}&create={create}",
                            arguments = listOf(
                                navArgument("link") {
                                    type = NavType.StringType
                                    defaultValue = Json.encodeToString(
                                        ExternalLinkSharedTo(
                                            id = "",
                                            title = "",
                                            shareLink = "",
                                            linkType = 1,
                                            password = null,
                                            denyDownload = false,
                                            isExpired = false,
                                            primary = false,
                                            requestToken = "",
                                            expirationDate = null
                                        )
                                    )
                                },
                                navArgument("create") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            ExternalLinkSettingsScreen(
                                link = backStackEntry.arguments?.getString("link")?.let(Json::decodeFromString),
                                isCreate = backStackEntry.arguments?.getBoolean("create") == true,
                                roomId = room?.id,
                                roomType = room?.roomType,
                                onBackListener = navController::popBackStack
                            )
                        }
                        composable(
                            route = "${RoomInfoScreens.UserAccess.name}?" +
                                    "userId={userId}&" +
                                    "access={access}&" +
                                    "ownerOrAdmin={ownerOrAdmin}",
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType },
                                navArgument("access") { type = NavType.IntType },
                                navArgument("ownerOrAdmin") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                            val roomId = room?.id.orEmpty()
                            UserAccessScreen(
                                navController = navController,
                                roomId = roomId,
                                userId = userId,
                                currentAccess = backStackEntry.arguments?.getInt("access"),
                                ownerOrAdmin = backStackEntry.arguments?.getBoolean("ownerOrAdmin") == true,
                            ) { newAccess ->
                                viewModel.setUserAccess(roomId, userId, newAccess)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingPlaceholder() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.placeholder_loading),
                style = MaterialTheme.typography.body1
            )
        }
    }

    @Composable
    private fun RoomInfoScreen(
        state: RoomInfoState,
        canEditRoom: Boolean,
        roomType: Int?,
        roomTitle: String?,
        portal: String?,
        onSetUserAccess: (userId: String, access: Int, ownerOrAdmin: Boolean) -> Unit,
        onAddUsers: () -> Unit,
        onBackClick: () -> Unit,
        onLinkClick: (ExternalLink) -> Unit,
        onGeneralLinkCreate: () -> Unit,
        onAdditionalLinkCreate: () -> Unit
    ) {
        BackHandler(onBack = onBackClick)

        AppScaffold(
            useTablePaddings = false,
            topBar = {
                AppTopBar(
                    title = {
                        Column {
                            Text(text = roomTitle ?: stringResource(id = R.string.list_context_info))
                            roomType?.let { type ->
                                Text(
                                    text = stringResource(id = RoomUtils.getRoomInfo(type).title),
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    },
                    actions = {
                        TopAppBarAction(
                            icon = R.drawable.ic_add_users,
                            onClick = onAddUsers,
                            enabled = canEditRoom
                        )
                    },
                    backListener = onBackClick
                )
            }
        ) {
            if (state.isLoading) {
                LoadingPlaceholder()
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val groupedShareList = Share.groupByAccess(state.shareList)
                    if (ApiContract.RoomType.hasExternalLink(roomType)) {
                        ExternalLinkBlock(
                            generalLink = state.generalLink,
                            additionalLinks = state.additionalLinks,
                            canEditRoom = canEditRoom,
                            onLinkClick = onLinkClick,
                            onGeneralLinkCreate = onGeneralLinkCreate,
                            onAdditionalLinkCreate = onAdditionalLinkCreate
                        )
                    }
                    ShareUsersList(
                        title = R.string.rooms_info_admin_title,
                        portal = portal,
                        shareList = groupedShareList[ShareGroup.Admin],
                        onClick = onSetUserAccess
                    )
                    ShareUsersList(
                        title = R.string.rooms_info_users_title,
                        portal = portal,
                        shareList = groupedShareList[ShareGroup.User],
                        onClick = onSetUserAccess
                    )
                    ShareUsersList(
                        title = R.string.rooms_info_expected_title,
                        portal = portal,
                        shareList = groupedShareList[ShareGroup.Expected],
                        onClick = onSetUserAccess
                    )
                }
            }
        }
    }

    @Composable
    private fun ExternalLinkBlock(
        generalLink: ExternalLink?,
        additionalLinks: List<ExternalLink>,
        canEditRoom: Boolean,
        onLinkClick: (ExternalLink) -> Unit,
        onGeneralLinkCreate: () -> Unit,
        onAdditionalLinkCreate: () -> Unit
    ) {
        val context = LocalContext.current
        if (!canEditRoom && (generalLink != null || additionalLinks.isNotEmpty()) || canEditRoom) {
            AppDescriptionItem(
                modifier = Modifier.padding(top = 8.dp),
                text = R.string.rooms_info_access_desc
            )
        }
        if (!canEditRoom && generalLink != null || canEditRoom) {
            AppHeaderItem(title = R.string.rooms_info_general_link)
        }
        if (generalLink != null) {
            ExternalLinkItem(
                linkTitle = generalLink.sharedTo.title,
                access = generalLink.access,
                hasPassword = !generalLink.sharedTo.password.isNullOrEmpty(),
                expiring = false,
                isExpired = generalLink.sharedTo.isExpired,
                canEdit = canEditRoom,
                onShareClick = {
                    context.openSendTextActivity(
                        context.getString(R.string.toolbar_menu_main_share),
                        generalLink.sharedTo.shareLink
                    )
                },
                onClick = { onLinkClick.invoke(generalLink) }.takeIf { canEditRoom }
            )
        }

        if (canEditRoom && generalLink == null) {
            AppTextButton(
                modifier = Modifier.padding(start = 8.dp),
                title = R.string.rooms_info_create_link,
                onClick = onGeneralLinkCreate
            )
        }

        if (!canEditRoom && additionalLinks.isNotEmpty() || canEditRoom) {
            AppHeaderItem(
                title = stringResource(
                    id = R.string.rooms_info_additional_links,
                    additionalLinks.size,
                    MAX_ADDITIONAL_LINKS_COUNT
                )
            )
        }

        additionalLinks.forEach { link ->
            ExternalLinkItem(
                linkTitle = link.sharedTo.title,
                access = link.access,
                hasPassword = !link.sharedTo.password.isNullOrEmpty(),
                expiring = !link.sharedTo.expirationDate.isNullOrEmpty(),
                isExpired = link.sharedTo.isExpired,
                canEdit = canEditRoom,
                onShareClick = {
                    context.openSendTextActivity(
                        context.getString(R.string.toolbar_menu_main_share),
                        link.sharedTo.shareLink
                    )
                },
                onClick = { onLinkClick.invoke(link) }.takeIf { canEditRoom }
            )
        }
        if (additionalLinks.size < MAX_ADDITIONAL_LINKS_COUNT && canEditRoom) {
            AppTextButton(
                modifier = Modifier.padding(start = 8.dp),
                title = R.string.rooms_info_create_link,
                onClick = onAdditionalLinkCreate
            )
        }
    }

    @Composable
    fun ShareUsersList(title: Int, portal: String?, shareList: List<Share>?, onClick: (String, Int, Boolean) -> Unit) {
        var visible by remember { mutableStateOf(true) }

        if (!shareList.isNullOrEmpty()) {
            Column {
                Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.Bottom),
                        text = stringResource(id = title, shareList.size),
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.colorTextSecondary
                    )
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = { visible = !visible }
                    ) {
                        Icon(
                            modifier = Modifier.rotate(if (visible) 0f else 180f),
                            imageVector = ImageVector.vectorResource(lib.toolkit.base.R.drawable.ic_arrow_down),
                            contentDescription = null,
                            tint = MaterialTheme.colors.colorTextTertiary
                        )
                    }
                }
                AnimatedVisibilityVerticalFade(visible = visible) {
                    Column {
                        shareList.forEach { share ->
                            ShareUserItem(share = share, portal = portal) {
                                onClick.invoke(
                                    share.sharedTo.id,
                                    share.intAccess,
                                    share.sharedTo.isOwner || share.sharedTo.isAdmin
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    private fun ShareUserItem(share: Share, portal: String?, onClick: () -> Unit) {
        Column(modifier = Modifier.addIf(share.canEditAccess) { clickable(onClick = onClick) }) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlideImage(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clip(CircleShape)
                        .size(40.dp)
                        .background(MaterialTheme.colors.colorTextTertiary),
                    model = "${ApiContract.SCHEME_HTTPS}$portal${share.sharedTo.avatarMedium}",
                    loading = placeholder(R.drawable.ic_account_placeholder),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body1,
                    text = share.sharedTo.displayName
                )
                Text(
                    text = stringResource(id = RoomUtils.getAccessTitleOrOwner(share)),
                    style = MaterialTheme.typography.body2,
                    color = if (share.canEditAccess)
                        MaterialTheme.colors.colorTextSecondary else
                        MaterialTheme.colors.colorTextTertiary
                )
                if (share.canEditAccess) {
                    Icon(
                        imageVector = ImageVector.vectorResource(lib.toolkit.base.R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colors.colorTextTertiary
                    )
                }
            }
            AppDivider(startIndent = 16.dp + 40.dp + 16.dp)
        }
    }

    @Preview
    @Composable
    private fun ExternalLinkScreenPreview() {
        val link = ExternalLink(
            access = 2,
            isLocked = false,
            isOwner = false,
            canEditAccess = false,
            sharedTo = ExternalLinkSharedTo(
                id = "",
                title = "Shared link",
                shareLink = "",
                linkType = 2,
                denyDownload = false,
                isExpired = false,
                primary = true,
                requestToken = "",
                password = "",
                expirationDate = ""
            )
        )

        ManagerTheme {
            RoomInfoScreen(
                roomTitle = "Room title",
                roomType = ApiContract.RoomType.CUSTOM_ROOM,
                canEditRoom = true,
                portal = "",
                state = RoomInfoState(
                    isLoading = false,
                    generalLink = link,
                    additionalLinks = listOf(
                        link.copy(sharedTo = link.sharedTo.copy(title = "Link 1", expirationDate = "123")),
                        link.copy(sharedTo = link.sharedTo.copy(title = "Link 2", password = "123")),
                        link.copy(sharedTo = link.sharedTo.copy(title = "Link 3", isExpired = true)),
                    ),
                    shareList = listOf(
                        Share(access = "1", sharedTo = SharedTo(displayName = "User 1"), isOwner = true),
                        Share(access = "9", sharedTo = SharedTo(displayName = "User 2")),
                        Share(access = "11", sharedTo = SharedTo(displayName = "User 3")),
                        Share(access = "10", sharedTo = SharedTo(displayName = "User 4")),
                        Share(access = "10", sharedTo = SharedTo(displayName = "User 4", activationStatus = 2)),
                    )
                ),
                onBackClick = {},
                onAddUsers = {},
                onSetUserAccess = { _, _, _-> },
                onGeneralLinkCreate = {},
                onAdditionalLinkCreate = {},
                onLinkClick = {}
            )
        }
    }
}
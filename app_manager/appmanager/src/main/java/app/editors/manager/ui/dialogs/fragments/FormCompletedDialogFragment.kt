package app.editors.manager.ui.dialogs.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import app.documents.core.model.login.User
import app.documents.core.network.manager.models.explorer.CloudFile
import app.editors.manager.R
import app.editors.manager.managers.utils.GlideAvatarImage
import app.editors.manager.managers.utils.ManagerUiUtils
import app.editors.manager.managers.utils.StringUtils
import lib.compose.ui.theme.ManagerTheme
import lib.compose.ui.theme.colorTextSecondary
import lib.compose.ui.views.AppDescriptionItem
import lib.compose.ui.views.AppDivider
import lib.compose.ui.views.AppHeaderItem
import lib.compose.ui.views.AppListItem
import lib.compose.ui.views.AppScaffold
import lib.compose.ui.views.AppTextButton
import lib.compose.ui.views.AppTopBar
import lib.compose.ui.views.NestedColumn
import lib.toolkit.base.managers.utils.UiUtils
import lib.toolkit.base.managers.utils.getSerializableExt
import lib.toolkit.base.managers.utils.putArgs

class FormCompletedDialogFragment : BaseDialogFragment() {

    companion object {

        private const val KEY_CLOUD_FILE = "key_cloud_file"
        private const val KEY_FORM_OWNER = "key_form_owner"

        private fun newInstance(
            cloudFile: CloudFile,
            formOwner: User
        ): FormCompletedDialogFragment {
            return FormCompletedDialogFragment().putArgs(
                KEY_CLOUD_FILE to cloudFile,
                KEY_FORM_OWNER to formOwner
            )
        }

        fun show(fragmentManager: FragmentManager, cloudFile: CloudFile, formOwner: User) {
            newInstance(cloudFile, formOwner).show(fragmentManager, null)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return ComponentDialog(
            requireContext(),
            if (!UiUtils.isTablet(requireContext())) R.style.FullScreenDialog else 0
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view as? ComposeView)?.setContent {
            ManagerTheme {
                FormCompletedScreen(
                    cloudFile = remember {
                        checkNotNull(
                            arguments?.getSerializableExt<CloudFile>(KEY_CLOUD_FILE)
                        )
                    },
                    formOwner = remember {
                        checkNotNull(
                            arguments?.getSerializableExt<User>(KEY_FORM_OWNER)
                        )
                    },
                    onSendEmailClick = {},
                    onBackToRoomClick = {},
                    onCheckReadyFormsClick = {}
                )
            }
        }
    }
}

@Composable
fun FormCompletedScreen(
    cloudFile: CloudFile,
    formOwner: User,
    onSendEmailClick: () -> Unit,
    onBackToRoomClick: () -> Unit,
    onCheckReadyFormsClick: () -> Unit
) {
    AppScaffold(topBar = {
        AppTopBar(title = R.string.rooms_fill_form_complete_toolbar_title)
    }) {
        NestedColumn(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            AppDescriptionItem(
                modifier = Modifier.padding(vertical = 16.dp),
                text = R.string.rooms_fill_form_complete_desc
            )
            RowItem(
                title = cloudFile.title,
                subtitle = StringUtils.getCloudItemInfo(
                    context = context,
                    item = cloudFile,
                    userId = null,
                    sortBy = null
                ).orEmpty(),
                divider = true,
                image = {
                    Image(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(
                            id = ManagerUiUtils.getIcon(
                                cloudFile
                            )
                        ),
                        contentDescription = null
                    )
                },
                endButton = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_list_context_external_link),
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            )
            AppHeaderItem(title = R.string.rooms_fill_form_complete_number_title)
            AppListItem(title = "1")
            AppHeaderItem(title = R.string.rooms_fill_form_complete_owner_title)
            RowItem(
                title = formOwner.displayName,
                subtitle = formOwner.email.orEmpty(),
                divider = false,
                image = { GlideAvatarImage(url = formOwner.avatarMedium) }
            ) {
                IconButton(onClick = onSendEmailClick) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTextButton(
                    title = R.string.rooms_fill_form_complete_back_to_room,
                    onClick = onBackToRoomClick
                )
                AppTextButton(
                    title = R.string.rooms_fill_form_complete_check_forms,
                    onClick = onCheckReadyFormsClick
                )
            }
        }
    }
}

@Composable
private fun RowItem(
    title: String,
    subtitle: String,
    divider: Boolean,
    image: @Composable () -> Unit,
    endButton: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            image()
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(text = title)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.colorTextSecondary
                    )
                }
                endButton()
            }
            if (divider) AppDivider()
        }
    }
}

@Preview
@Composable
private fun FormCompletedScreenPreview() {
    ManagerTheme {
        FormCompletedScreen(
            CloudFile().apply { title = "1-Dmitry Go-Jordan.pdf" },
            User(displayName = "Username", email = "email@email.com"),
            {},
            {},
            {}
        )
    }
}
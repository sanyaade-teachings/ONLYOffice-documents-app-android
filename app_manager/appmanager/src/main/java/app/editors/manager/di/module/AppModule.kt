package app.editors.manager.di.module

import app.documents.core.di.module.AccountModule
import app.documents.core.di.module.LoginModule
import app.documents.core.di.module.RecentModule
import app.documents.core.di.module.SettingsModule
import app.editors.manager.dropbox.di.module.DropboxLoginModule
import app.editors.manager.googledrive.di.module.GoogleDriveLoginModule
import app.editors.manager.onedrive.di.module.OneDriveAuthModule
import app.editors.manager.onedrive.di.module.OneDriveLoginModule
import dagger.Module

@Module(includes = [ToolModule::class, SettingsModule::class, AccountModule::class, RecentModule::class, LoginModule::class, OneDriveLoginModule::class, OneDriveAuthModule::class, DropboxLoginModule::class, GoogleDriveLoginModule::class])
class AppModule
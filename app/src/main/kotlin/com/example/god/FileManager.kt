package com.example.god

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class FileManager(private val context: Context) {
    fun openFilePicker() {
        context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    fun openFolderPicker() {
        context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        })
    }

    fun saveAuthorizedFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences("god_files", Context.MODE_PRIVATE)
            .edit().putString("folder_uri", uri.toString()).apply()
    }

    fun authorizedFolder(): DocumentFile? {
        val value = context.getSharedPreferences("god_files", Context.MODE_PRIVATE)
            .getString("folder_uri", null) ?: return null
        return DocumentFile.fromTreeUri(context, Uri.parse(value))
    }
}

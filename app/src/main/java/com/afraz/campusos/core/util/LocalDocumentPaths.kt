package com.afraz.campusos.core.util

import android.content.Context
import java.io.File

/**
 * Resolves only app-owned document files that may be opened or removed by the wallet.
 * Canonical paths also reject symlinks that point outside the app's managed document folders.
 */
object LocalDocumentPaths {
    fun resolveManagedFile(context: Context, path: String): File? = runCatching {
        if (path.isBlank()) return null

        val filesRoot = context.filesDir.canonicalFile
        val candidate = File(path).canonicalFile
        val separator = File.separator
        val rootPrefix = filesRoot.path.trimEnd(separator[0]) + separator

        listOf("pdfs", "wallet")
            .map { File(filesRoot, it).canonicalFile }
            .firstOrNull { directory ->
                directory.path.startsWith(rootPrefix) &&
                    candidate.path.startsWith(directory.path.trimEnd(separator[0]) + separator)
            }
            ?.let { candidate }
    }.getOrNull()
}

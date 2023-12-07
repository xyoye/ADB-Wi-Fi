package dev.polek.adbwifi.utils

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileDrop
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent

object ApkFileDropHandler {
    private val apkFileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
        withFileFilter { it.extension == "apk" }
    }

    fun register(component: JComponent, block: (String) -> Unit) {
        FileDrop(component, object : FileDrop.Target {
            override fun getDescriptor(): FileChooserDescriptor {
                return apkFileChooserDescriptor
            }

            override fun isHiddenShown(): Boolean {
                return false
            }

            override fun dropFiles(files: MutableList<out VirtualFile>?) {
                val filePath = files?.firstOrNull()?.path
                if (filePath.isNullOrEmpty()) {
                    return
                }
                block.invoke(filePath)
            }
        })
    }
}
package nl.hannahsten.texifyidea.editor

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import nl.hannahsten.texifyidea.psi.LatexCommands
import nl.hannahsten.texifyidea.settings.TexifySettings
import nl.hannahsten.texifyidea.util.firstParentOfType

/**
 * Select all of the LatexCommands, so including the backslash.
 */
class LatexCommandSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean {
        return TexifySettings.getInstance().includeBackslashInSelection && e.firstParentOfType(LatexCommands::class)?.text == e.text
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): MutableList<TextRange>? {
        val ranges = super.select(e, editorText, cursorOffset, editor) ?: return null
        val commandRange = e.textRange

        SelectWordUtil.addWordOrLexemeSelection(false, editor, cursorOffset, mutableListOf(commandRange)) { c: Char -> c.isLetterOrDigit() || c == '\\'}
        ranges += commandRange
        return ranges
    }
}
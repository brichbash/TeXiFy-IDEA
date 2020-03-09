package nl.hannahsten.texifyidea.util.files

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import nl.hannahsten.texifyidea.index.BibtexEntryIndex
import nl.hannahsten.texifyidea.index.LatexCommandsIndex
import nl.hannahsten.texifyidea.index.LatexDefinitionIndex
import nl.hannahsten.texifyidea.index.LatexIncludesIndex
import nl.hannahsten.texifyidea.psi.LatexCommands
import nl.hannahsten.texifyidea.psi.LatexNormalText
import nl.hannahsten.texifyidea.util.childrenOfType
import nl.hannahsten.texifyidea.util.isDefinition

/**
 * Finds all the files in the project that are somehow related using includes.
 *
 * When A includes B and B includes C then A, B & C will all return a set containing A, B & C.
 *
 * Be careful when using this function directly over something like [ReferencedFileSetService] where the result
 * values are cached.
 *
 * @param baseFile
 *         The file to find the reference set of.
 * @return All the files that are cross referenced between each other.
 */
// Internal because only ReferencedFileSetCache should call this
internal fun findReferencedFileSetWithoutCache(baseFile: PsiFile): Set<PsiFile> {
    // Setup.
    val project = baseFile.project
    val includes = LatexIncludesIndex.getItems(project)

    // Find all root files.
    val roots = includes.asSequence()
            .map { it.containingFile }
            .distinct()
            .filter { it.isRoot() }
            .toSet()

    // Map root to all directly referenced files.
    val sets = HashMap<PsiFile, Set<PsiFile>>()
    for (root in roots) {
        val referenced = root.referencedFiles(root.virtualFile) + root

        if (referenced.contains(baseFile)) {
            return referenced + baseFile
        }

        sets[root] = referenced
    }

    // Look for matching root.
    for (referenced in sets.values) {
        if (referenced.contains(baseFile)) {
            return referenced + baseFile
        }
    }

    return setOf(baseFile)
}

/**
 * Finds all the files in the project that are somehow related using includes.
 *
 * When A includes B and B includes C then A, B & C will all return a set containing A, B & C.
 *
 * @return All the files that are cross referenced between each other.
 */
fun PsiFile.referencedFileSet(): Set<PsiFile> {
    return ReferencedFileSetService.getInstance().referencedFileSetOf(this)
}

/**
 * @see [BibtexEntryIndex.getIndexedEntriesInFileSet]
 */
@Suppress("unused")
fun PsiFile.bibtexIdsInFileSet() = BibtexEntryIndex.getIndexedEntriesInFileSet(this)

/**
 * @see [LatexCommandsIndex.getItemsInFileSet]
 */
fun PsiFile.commandsInFileSet(): Collection<LatexCommands> = LatexCommandsIndex.getItemsInFileSet(this)

/**
 * @see [LatexCommandsIndex.getItemsAndFilesInFileSet]
 */
fun PsiFile.commandsAndFilesInFileSet(): List<Pair<PsiFile, Collection<LatexCommands>>> = LatexCommandsIndex.getItemsAndFilesInFileSet(this)

/**
 * Get all the definitions in the file set.
 */
fun PsiFile.definitionsInFileSet(): Collection<LatexCommands> {
    return LatexDefinitionIndex.getItemsInFileSet(this)
            .filter { it.isDefinition() }
}

/**
 * Get all the definitions and redefinitions in the file set.
 */
fun PsiFile.definitionsAndRedefinitionsInFileSet(): Collection<LatexCommands> {
    return LatexDefinitionIndex.getItemsInFileSet(this)
}

/**
 * When using \includegraphics from graphicx package, a path prefex can be set with \graphicspath.
 * @return Graphicspaths defined in the fileset.
 */
fun getGraphicsPaths(project: Project): List<String> {

    val graphicsPaths = mutableListOf<String>()
    val graphicsPathCommands = LatexCommandsIndex.getItemsByName("\\graphicspath", project, GlobalSearchScope.projectScope(project))

    // Is a graphicspath defined?
    if (graphicsPathCommands.isNotEmpty()) {
        // Only last defined one counts
        val args = graphicsPathCommands.last().parameterList.filter { it.requiredParam != null }
        val subArgs = args.first().childrenOfType(LatexNormalText::class)
        subArgs.forEach { graphicsPaths.add(it.text) }
    }

    return graphicsPaths
}

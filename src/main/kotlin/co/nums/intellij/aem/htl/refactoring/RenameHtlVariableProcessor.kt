package co.nums.intellij.aem.htl.refactoring

import co.nums.intellij.aem.htl.definitions.isHtlIterableBlock
import co.nums.intellij.aem.htl.extensions.isHtlVariableDeclaration
import co.nums.intellij.aem.htl.psi.references.HtlVariableReference
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttribute
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.Queue

private val LOG = Logger.getInstance("#co.nums.intellij.aem.htl.refactoring.RenameHtlVariableProcessor")

class RenameHtlVariableProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement) = element is XmlAttribute && element.isHtlVariableDeclaration()

    override fun renameElement(element: PsiElement, newName: String, usages: Array<UsageInfo>, listener: RefactoringElementListener?) {
        if (element is XmlAttribute) {
            try {
                val blockName = element.localName
                val blockType = blockName.substringBefore('.')
                val variableIdentifier = blockName.substringAfter('.', missingDelimiterValue = "item")
                renameHtlVariables(element, usages, newName, blockType, variableIdentifier)
                val newElement = element.setName("$blockType.$newName")
                listener?.elementRenamed(newElement)
            } catch (e: IncorrectOperationException) {
                LOG.error(e)
            }
        }
    }

    private fun renameHtlVariables(originalElement: PsiElement, usages: Array<UsageInfo>, newName: String, blockType: String, oldVariableIdentifier: String) {
        if (newName == oldVariableIdentifier) return
        val queue = Queue<PsiReference>(usages.size)
        usages
                .filter { it.element != null }
                .mapNotNull { it.reference }
                .forEach { queue.addLast(it) }

        val isIterable = isHtlIterableBlock(blockType)
        val oldIterableVariableName = if (isIterable) "${oldVariableIdentifier}List" else null
        val newIterableVariableName = if (isIterable) "${newName}List" else null

        while (!queue.isEmpty) {
            val reference = queue.pullFirst()
            val oldElement = reference.element
            if (!oldElement.isValid || oldElement === originalElement) continue
            val actualNewName = if (isIterable && oldElement.text.equals(oldIterableVariableName, ignoreCase = true)) newIterableVariableName else newName
            val newElement = reference.handleElementRename(actualNewName)
            if (!oldElement.isValid) {
                for (psiReference in ReferencesSearch.search(originalElement, LocalSearchScope(newElement), false)) {
                    queue.addLast(psiReference)
                }
            }
        }
    }

    override fun findReferences(element: PsiElement?, searchInCommentsAndStrings: Boolean): MutableCollection<PsiReference> {
        return findReferences(element)
    }

    override fun findReferences(element: PsiElement?): MutableCollection<PsiReference> {
        return (element as XmlAttribute).references
                .filterIsInstance(HtlVariableReference::class.java)
                .toMutableList()
    }

}

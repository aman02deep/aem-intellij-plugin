package co.nums.intellij.aem.htl.psi.references

import co.nums.intellij.aem.htl.HtlLanguage
import co.nums.intellij.aem.htl.definitions.HtlBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.search.*
import com.intellij.psi.xml.XmlAttribute

class HtlDeclarationSearchScopeOptimizer : ScopeOptimizer {

    override fun getScopeToExclude(element: PsiElement): GlobalSearchScope? {
        if (element is XmlAttribute && element.name == HtlBlock.USE.type) {
            val originalFile = element.containingFile
            val htlFile = originalFile.viewProvider.getPsi(HtlLanguage) ?: return null
            return GlobalSearchScope.notScope(GlobalSearchScope.filesScope(
                    element.project,
                    mutableListOf(originalFile.virtualFile, htlFile.virtualFile)))
        }
        return null
    }

}

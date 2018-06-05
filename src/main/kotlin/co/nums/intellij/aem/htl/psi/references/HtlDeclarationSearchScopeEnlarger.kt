package co.nums.intellij.aem.htl.psi.references

import co.nums.intellij.aem.htl.HtlLanguage
import co.nums.intellij.aem.htl.extensions.isHtlVariableDeclaration
import co.nums.intellij.aem.htl.psi.HtlVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.search.*
import com.intellij.psi.xml.XmlAttribute

class HtlDeclarationSearchScopeEnlarger : UseScopeEnlarger() {

    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        if ((element is XmlAttribute && element.isHtlVariableDeclaration()) || element is HtlVariable) {
            val originalFile = element.containingFile
            val htlFile = originalFile.viewProvider.getPsi(HtlLanguage) ?: return null
            return LocalSearchScope(htlFile) // FIXME: move it to optimizer?
        }
        return null
    }

}

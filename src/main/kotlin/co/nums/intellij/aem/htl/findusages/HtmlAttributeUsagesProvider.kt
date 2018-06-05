package co.nums.intellij.aem.htl.findusages

import co.nums.intellij.aem.htl.extensions.isHtlVariableDeclaration
import co.nums.intellij.aem.htl.psi.mixins.HtlVariableReference
import com.intellij.find.findUsages.*
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.xml.XmlAttribute

class HtmlAttributeUsagesProvider : FindUsagesHandlerFactory() {

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        return if (element is XmlAttribute && element.isHtlVariableDeclaration()) {
            HtlAttributesFindUsagesHandler(element)
        } else {
            null
        }
    }

    override fun canFindUsages(element: PsiElement): Boolean {
        return element is XmlAttribute && element.isHtlVariableDeclaration()
    }

    class HtlAttributesFindUsagesHandler(xmlAttribute: XmlAttribute) : FindUsagesHandler(xmlAttribute) {

        override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): MutableCollection<PsiReference> {
            return super.findReferencesToHighlight(target, searchScope)
                    .filter { it is HtlVariableReference }
                    .toMutableList()
        }

    }

}

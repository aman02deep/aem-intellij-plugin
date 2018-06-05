package co.nums.intellij.aem.htl.psi.references

import co.nums.intellij.aem.htl.HtlLanguage
import co.nums.intellij.aem.htl.definitions.HtlGlobalObject
import co.nums.intellij.aem.htl.psi.HtlVariable
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class HtlGlobalObjectsReferencesContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(HtlVariable::class.java), HtlGlobalObjectsReferencesProvider())
    }

}

class HtlGlobalObjectsReferencesProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val variable = element as? HtlVariable ?: return emptyArray()
        val variableIdentifier = variable.text
        if (!HtlGlobalObject.allIdentifiers.contains(variableIdentifier)) return emptyArray()
        val htlFile = variable.containingFile.viewProvider.getPsi(HtlLanguage) ?: return emptyArray()
        return PsiTreeUtil.findChildrenOfType(htlFile, HtlVariable::class.java)
                .filterNot { it == variable }
                .filter { it.text.equals(variableIdentifier, ignoreCase = true) }
                .mapNotNull { HtlGlobalObjectReference(element) }
                .toTypedArray()
    }

}

class HtlGlobalObjectReference(private val htlVariable: HtlVariable) : PsiReferenceBase<HtlVariable>(htlVariable) {

    override fun resolve() = htlVariable

    override fun getVariants(): Array<Any> = emptyArray()

}

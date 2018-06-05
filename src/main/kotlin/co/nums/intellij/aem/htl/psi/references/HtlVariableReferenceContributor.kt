package co.nums.intellij.aem.htl.psi.references

import co.nums.intellij.aem.htl.HtlLanguage
import co.nums.intellij.aem.htl.definitions.*
import co.nums.intellij.aem.htl.extensions.getHtlVariableIdentifier
import co.nums.intellij.aem.htl.psi.HtlVariable
import co.nums.intellij.aem.htl.psi.patterns.HtlPatterns
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.xml.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.util.ProcessingContext

class HtlVariableReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(HtlPatterns.htlVariableDeclaration, HtlVariablesReferencesProvider())
    }

}

class HtlVariablesReferencesProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val attribute = element as? XmlAttribute ?: return emptyArray()
        val variableReferences: MutableList<PsiReference> = attribute.findReferences()
                .map { HtlVariableReference(attribute, it) }
                .toMutableList()
        variableReferences.add(0, HtlVariableDeclarationWrapper(attribute as XmlAttributeImpl))
        return variableReferences.toTypedArray()
    }

    private fun XmlAttribute.findReferences(): Collection<HtlVariable> {
        val htlFile = containingFile.viewProvider.getPsi(HtlLanguage) ?: return emptyList()
        val variableIdentifier = getHtlVariableIdentifier() ?: return emptyList()
        val blockName = getBlockName()
        val htlVariables = htlVariables(htlFile, blockName, variableIdentifier)
        return when {
            isElementScopedVariableBlock(blockName) -> referencesInDeclaringAttributeScope(this, variableIdentifier, htlVariables)
            isElementChildrenScopedVariableBlock(blockName) -> referencesInDeclaringAttributeScope(this, variableIdentifier, htlVariables)// FIXME
            isGlobalVariableBlock(blockName) -> referencesAfterDeclaration(this, variableIdentifier, htlVariables)
            isTemplateVariableBlock(blockName) -> emptyList() // TODO: why empty?
            else -> throw IllegalStateException("Invalid block attribute: $blockName")
        }
    }

    private fun htlVariables(htlFile: PsiFile, blockName: String, variableIdentifier: String): List<HtlVariable> {
        val iterableBlock = isHtlIterableBlock(blockName)
        return PsiTreeUtil.findChildrenOfType(htlFile, HtlVariable::class.java)
                .filter {
                    val text = it.text
                    text.equals(variableIdentifier, ignoreCase = true)
                            || (iterableBlock && text.equals("${variableIdentifier}List", ignoreCase = true))
                }
    }

    private fun XmlAttribute.getBlockName() = if (localName.contains('.')) localName.substringBefore('.') else localName

    private fun referencesInDeclaringAttributeScope(attribute: XmlAttribute, variableIdentifier: String, htlVariables: List<HtlVariable>): Collection<HtlVariable> {
        val scopeRange = attribute.parent.textRange
        val overridingScopes = PsiTreeUtil.findChildrenOfType(attribute.parent, XmlAttribute::class.java)
                .filter { it.getHtlVariableIdentifier().equals(variableIdentifier, ignoreCase = true) }
                .filter { isElementScopedVariableBlock(it.getBlockName()) } // FIXME?
                .map { it.parent }
                .filter { it != attribute.parent }
                .map { it.textRange }
        return htlVariables
                .filter { scopeRange.contains(it.textOffset) }
                .filter { isNotOverridden(it, overridingScopes) }
    }

    private fun referencesAfterDeclaration(attribute: XmlAttribute, variableIdentifier: String, htlVariables: List<HtlVariable>): Collection<HtlVariable> {
        val overridingScopes = PsiTreeUtil.findChildrenOfType(attribute.containingFile.viewProvider.getPsi(HTMLLanguage.INSTANCE), XmlAttribute::class.java)
                .filter { it.getHtlVariableIdentifier().equals(variableIdentifier, ignoreCase = true) }
                .filter { isElementScopedVariableBlock(it.getBlockName()) } // FIXME?
                .map { it.parent }
                .map { it.textRange }
        return htlVariables
                .filter { it.textOffset > attribute.textOffset }
                .filter { isNotOverridden(it, overridingScopes) }
    }

    private fun isNotOverridden(htlVariable: HtlVariable, redeclarationScopes: List<TextRange>) =
            redeclarationScopes.none { it.contains(htlVariable.textOffset) }

}

class HtlVariableReference(private val declarationAttribute: XmlAttribute, htlVariable: HtlVariable) : PsiReferenceBase<HtlVariable>(htlVariable) {

    override fun resolve() = declarationAttribute

    override fun isReferenceTo(element: PsiElement?): Boolean {
        return super.isReferenceTo(element) || declarationAttribute.isEquivalentTo(element)
    }

    override fun getVariants(): Array<Any> = emptyArray()

}

class HtlVariableDeclarationWrapper(private val xmlAttribute: XmlAttributeImpl) : XmlAttributeReference(xmlAttribute) {

    override fun resolve() = xmlAttribute

}

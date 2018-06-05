package co.nums.intellij.aem.htl.psi.mixins

import co.nums.intellij.aem.htl.data.blocks.HtlBlockVariable
import co.nums.intellij.aem.htl.definitions.BlockIdentifierType
import co.nums.intellij.aem.htl.file.HtlFileType
import co.nums.intellij.aem.htl.psi.*
import co.nums.intellij.aem.htl.psi.search.HtlSearch
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.*

abstract class HtlVariableMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner, ContributedReferenceHost {

    //find usages is needed?

    override fun getReference() = HtlVariableReference(this as HtlVariable)

    // TODO
    // check if it can be used in eg. term rule and all references can be created and provided there (from term rule)
    // from term to simple at first to resolve attribute, then create all references in properties chain
    // also referencesSearch should be implemented? with the proper scope
    override fun getReferences(): Array<PsiReference> = ReferenceProvidersRegistry.getReferencesFromProviders(this)

    override fun setName(name: String): PsiElement? {
        createIdentifier(name)?.let {
            node.replaceChild(node.firstChildNode, it)
        }
        return this
    }

    private fun createIdentifier(name: String): ASTNode? {
        val dummyFileContent = "${'$'}{$name}"
        val dummyFile = PsiFileFactory.getInstance(project)
                .createFileFromText("dummy.html", HtlFileType, dummyFileContent)
        return PsiTreeUtil.findChildOfType(dummyFile, HtlVariable::class.java)
                ?.node
                ?.findChildByType(HtlTypes.IDENTIFIER)
    }

    override fun getName(): String? = nameIdentifier?.text

    override fun getNameIdentifier(): PsiElement? = findChildByType(HtlTypes.IDENTIFIER)

}

class HtlVariableReference(htlVariable: HtlVariable) : PsiReferenceBase<HtlVariable>(htlVariable) {

    override fun resolve(): PsiElement? {
        return ResolveCache.getInstance(element.project)
                .resolveWithCaching(this, { reference, _ -> doResolve(reference) }, false, false)
    }

    private fun doResolve(reference: HtlVariableReference): PsiElement? {
        val variableName = reference.element.identifier.text
//        val htlJavaSearch = ServiceManager.getService(HtlJavaSearch::class.java)
//        return htlJavaSearch?.globalObjectJavaClass(element.project, variableName) // FIXME: do not resolve it to java classes
        return blockVariableAttribute(variableName)
    }

    private fun blockVariableAttribute(variableName: String?): PsiElement? {
        val htmlFile = element.containingFile.viewProvider.getPsi(StdLanguages.HTML)
        return HtlSearch.blockVariables(htmlFile)
                .filter { it.identifier.equals(variableName, ignoreCase = true) }
                .filter { it.hasMatchingScope() }
                .sortedWith(BestMatchingToElementComparator(element))
                .map { it.declaration } // FIXME: declaration doesn't take scopes into account; or maybe does because of filtering here
                .firstOrNull()
    }

    private fun HtlBlockVariable.hasMatchingScope() = when (identifierType) {
        BlockIdentifierType.ELEMENT_SCOPE_VARIABLE -> isAfterDeclarationAttributeAndInTag()
        BlockIdentifierType.ELEMENT_CHILDREN_SCOPE_VARIABLE -> isInsideOfDeclaringTag()
        BlockIdentifierType.GLOBAL_VARIABLE -> isDeclaredBeforeElement()
        BlockIdentifierType.TEMPLATE_NAME -> true
        else -> false
    }

    private fun HtlBlockVariable.isAfterDeclarationAttributeAndInTag(): Boolean {
        val declaringAttributeEnd = declaration.textRange.endOffset
        val declaringElementEnd = declaration.parent.textRange.endOffset
        return element.textOffset in declaringAttributeEnd..declaringElementEnd
    }

    private fun HtlBlockVariable.isInsideOfDeclaringTag(): Boolean {
        val declaringTag = declaration.parent
        val openingTagEnd = declaringTag.children
                .filterIsInstance(XmlToken::class.java)
                .firstOrNull { it.tokenType == XmlTokenType.XML_TAG_END }
                ?.textRange?.endOffset ?: return false
        return element.textOffset in openingTagEnd..declaringTag.textRange.endOffset
    }

    private fun HtlBlockVariable.isDeclaredBeforeElement() = declaration.textOffset < element.textOffset

    override fun getVariants() = emptyArray<Any>()

}

private class BestMatchingToElementComparator(val element: HtlVariable) : Comparator<HtlBlockVariable> {

    override fun compare(o1: HtlBlockVariable, o2: HtlBlockVariable): Int {
        val o1Priority = o1.getPriority()
        val o2Priority = o2.getPriority()
        if (o1Priority == o2Priority) {
            val o1DistanceFromElement = element.textOffset - o1.declaration.textOffset
            val o2DistanceFromElement = element.textOffset - o2.declaration.textOffset
            return o1DistanceFromElement - o2DistanceFromElement
        }
        return o1Priority - o2Priority
    }

    private fun HtlBlockVariable.getPriority() = when (identifierType) {
        BlockIdentifierType.ELEMENT_CHILDREN_SCOPE_VARIABLE -> 0
        BlockIdentifierType.ELEMENT_SCOPE_VARIABLE -> 1
        BlockIdentifierType.GLOBAL_VARIABLE -> 2
        BlockIdentifierType.TEMPLATE_NAME -> 3
        else -> 4
    }

}

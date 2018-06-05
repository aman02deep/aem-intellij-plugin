package co.nums.intellij.aem.htl.psi.mixins

import co.nums.intellij.aem.htl.file.HtlFileType
import co.nums.intellij.aem.htl.psi.*
import co.nums.intellij.aem.htl.psi.references.HtlVariableReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.PsiTreeUtil

abstract class HtlVariableMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {

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

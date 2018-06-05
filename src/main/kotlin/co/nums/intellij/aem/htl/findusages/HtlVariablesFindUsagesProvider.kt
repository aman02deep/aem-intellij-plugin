package co.nums.intellij.aem.htl.findusages

import co.nums.intellij.aem.htl.extensions.getHtlVariableIdentifier
import co.nums.intellij.aem.htl.lexer.HtlLexerAdapter
import co.nums.intellij.aem.htl.psi.HtlTypes.BOOLEAN_LITERAL
import co.nums.intellij.aem.htl.psi.HtlTypes.COMMENT_CONTENT
import co.nums.intellij.aem.htl.psi.HtlTypes.COMMENT_END
import co.nums.intellij.aem.htl.psi.HtlTypes.COMMENT_START
import co.nums.intellij.aem.htl.psi.HtlTypes.FLOAT_NUMBER
import co.nums.intellij.aem.htl.psi.HtlTypes.IDENTIFIER
import co.nums.intellij.aem.htl.psi.HtlTypes.INTEGER_NUMBER
import co.nums.intellij.aem.htl.psi.HtlTypes.STRING_LITERAL
import co.nums.intellij.aem.htl.psi.HtlVariable
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.xml.XmlAttribute

class HtlVariablesFindUsagesProvider : FindUsagesProvider {

    override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is XmlAttribute || psiElement is HtlVariable

    override fun getWordsScanner() = DefaultWordsScanner(
            HtlLexerAdapter(),
            TokenSet.create(IDENTIFIER),
            TokenSet.create(COMMENT_START, COMMENT_CONTENT, COMMENT_END),
            TokenSet.create(STRING_LITERAL, INTEGER_NUMBER, FLOAT_NUMBER, BOOLEAN_LITERAL)
    )

    override fun getDescriptiveName(element: PsiElement): String = element.text

    override fun getType(element: PsiElement) = when (element) {
        is XmlAttribute, is HtlVariable -> "HTL variable"
        else -> ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = when (element) {
        is XmlAttribute -> element.getHtlVariableIdentifier() ?: ""
        is HtlVariable -> element.text
        else -> "<unknown name>"
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

}

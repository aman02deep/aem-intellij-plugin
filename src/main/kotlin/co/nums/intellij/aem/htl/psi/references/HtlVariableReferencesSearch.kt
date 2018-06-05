package co.nums.intellij.aem.htl.psi.references

import co.nums.intellij.aem.htl.extensions.getHtlVariableIdentifier
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttribute
import com.intellij.util.Processor

class HtlVariableReferencesSearch : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {

    override fun processQuery(searchParams: ReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val attribute = searchParams.elementToSearch as? XmlAttribute ?: return
        val variableIdentifier = attribute.getHtlVariableIdentifier() ?: return
        searchParams.optimizer.searchWord(variableIdentifier, searchParams.effectiveSearchScope, true, attribute)
    }

}

// FIXME: ReferencesSearch:111 - problem - effective scope is empty na makesSense to search
